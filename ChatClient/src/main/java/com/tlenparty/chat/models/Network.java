package com.tlenparty.chat.models;

import com.tlenparty.chat.NetworkClient;
import com.tlenparty.chat.controllers.ChatController;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class Network {
    private static final String AUTH_CMD = "/auth";
    private static final String AUTHOK_CMD = "/authok";
    private static final String PUBLIC_MSG_CMD = "/msg";
    private static final String PRIVATE_MSG_CMD = "/w";
    private static final String PRIVATE_DELIVERY_CMD = "/privateMsg";
    private static final String CLIENT_MSG_CMD = "/clientMsg";
    private static final String SERVER_MSG_CMD = "/serverMsg";
    private static final String END_CMD = "/end";
    private static final String USER_LIST_CMD = "/userList";
    private static final String CHANGE_USERNAME_CMD = "/changeUsername";
    private static final String CHANGE_USERNAME_OK_CMD = "/changeUsernameOk";
    private static final String CHANGE_USERNAME_ERR_CMD = "/changeUsernameErr";

    private static final int SERVER_PORT = 8189;
    private static final String SERVER_HOST = "localhost";

    private static final Logger logger = LogManager.getLogger(Network.class);

    private final int port;
    private final String host;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    private String login;
    private volatile String username;
    private volatile boolean closed;

    public Network() {
        this(Integer.getInteger("chat.port", SERVER_PORT), System.getProperty("chat.host", SERVER_HOST));
    }

    public Network(int serverPort, String serverHost) {
        this.port = serverPort;
        this.host = serverHost;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            return true;
        } catch (IOException e) {
            logger.error("Соединение не было установлено", e);
            return false;
        }
    }

    public void close() {
        if (closed || socket == null) {
            return;
        }
        closed = true;
        try {
            out.writeUTF(END_CMD);
            out.flush();
        } catch (IOException e) {
            logger.debug("Не удалось отправить команду завершения", e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            logger.warn("Ошибка закрытия сокета", e);
        }
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getUsername() {
        return username;
    }

    public void waitMessage(ChatController chatController) {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    dispatch(in.readUTF(), chatController);
                }
            } catch (IOException e) {
                if (!closed) {
                    logger.error("Соединение потеряно", e);
                    Platform.runLater(() -> NetworkClient.showErrorMessage("Ошибка подключения", "Соединение потеряно", e.getMessage()));
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void dispatch(String message, ChatController chatController) {
        String[] head = message.split("\\s+", 2);
        String command = head[0];
        String rest = head.length > 1 ? head[1] : "";

        if (USER_LIST_CMD.equals(command)) {
            List<String> users = rest.isBlank() ? List.of() : Arrays.asList(rest.trim().split("\\s+"));
            Platform.runLater(() -> chatController.updateUserList(users));
        } else if (CLIENT_MSG_CMD.equals(command)) {
            String[] parts = rest.split("\\s+", 2);
            String text = parts.length > 1 ? parts[1] : "";
            Platform.runLater(() -> chatController.appendMessage(String.format("%s: %s", parts[0], text)));
        } else if (PRIVATE_DELIVERY_CMD.equals(command)) {
            String[] parts = rest.split("\\s+", 3);
            String text = parts.length > 2 ? parts[2] : "";
            Platform.runLater(() -> chatController.appendMessage(String.format("%s (лично): %s", parts[0], text)));
        } else if (SERVER_MSG_CMD.equals(command)) {
            Platform.runLater(() -> chatController.appendMessage(rest));
        } else if (CHANGE_USERNAME_OK_CMD.equals(command)) {
            username = rest.trim();
            Platform.runLater(() -> chatController.onUsernameChanged(username));
        } else if (CHANGE_USERNAME_ERR_CMD.equals(command)) {
            Platform.runLater(() -> chatController.onUsernameChangeRejected(rest));
        } else {
            logger.warn("Неизвестная команда от сервера: {}", message);
        }
    }

    public String sendAuthCommand(String login, String password) {
        try {
            sendRaw(String.format("%s %s %s", AUTH_CMD, login, password));
            String response = in.readUTF();
            String[] parts = response.split("\\s+", 2);
            String payload = parts.length > 1 ? parts[1] : "";
            if (AUTHOK_CMD.equals(parts[0])) {
                this.username = payload;
                return null;
            }
            return payload.isEmpty() ? "Ошибка авторизации" : payload;
        } catch (IOException e) {
            logger.error("Ошибка авторизации", e);
            return "Нет связи с сервером: " + e.getMessage();
        }
    }

    public void sendMessage(String message) throws IOException {
        sendRaw(PUBLIC_MSG_CMD + " " + message);
    }

    public void sendPrivateMessage(String message, String recipient) throws IOException {
        sendRaw(PRIVATE_MSG_CMD + " " + recipient + " " + message);
    }

    public void sendChangeNameCommand(String newUsername) {
        try {
            sendRaw(CHANGE_USERNAME_CMD + " " + newUsername);
        } catch (IOException e) {
            logger.error("Ошибка смены имени", e);
        }
    }

    private synchronized void sendRaw(String message) throws IOException {
        out.writeUTF(message);
        out.flush();
    }
}
