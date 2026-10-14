package com.tlenparty.chat.server.handler;

import com.tlenparty.chat.server.MyServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private static final String AUTH_CMD = "/auth";
    private static final String AUTHOK_CMD = "/authok";
    private static final String AUTHERR_CMD = "/autherr";
    private static final String PUBLIC_MSG_CMD = "/msg";
    private static final String PRIVATE_MSG_CMD = "/w";
    private static final String PRIVATE_DELIVERY_CMD = "/privateMsg";
    private static final String CLIENT_MSG_CMD = "/clientMsg";
    private static final String SERVER_MSG_CMD = "/serverMsg";
    private static final String END_CMD = "/end";
    private static final String CHANGE_USERNAME_CMD = "/changeUsername";
    private static final String CHANGE_USERNAME_OK_CMD = "/changeUsernameOk";
    private static final String CHANGE_USERNAME_ERR_CMD = "/changeUsernameErr";
    private static final String USER_LIST_CMD = "/userList";

    private static final int AUTH_TIMEOUT_MILLIS = 120_000;
    private static final int MAX_USERNAME_LENGTH = 32;

    private final MyServer myServer;
    private final Socket clientSocket;
    private DataOutputStream out;
    private volatile String login;
    private volatile String username;

    public ClientHandler(MyServer myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (Socket socket = clientSocket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            this.out = output;
            socket.setSoTimeout(AUTH_TIMEOUT_MILLIS);
            if (authenticate(in)) {
                socket.setSoTimeout(0);
                readMessages(in);
            }
        } catch (SocketTimeoutException e) {
            logger.info("Клиент не авторизовался вовремя");
        } catch (EOFException | SocketException e) {
            logger.info("Клиент {} отключился", username);
        } catch (IOException e) {
            logger.error("Ошибка соединения с клиентом {}", username, e);
        } finally {
            if (username != null) {
                myServer.unSubscribe(this);
                myServer.broadcastServerMessage(username + " покинул чат", this);
            }
        }
    }

    private boolean authenticate(DataInputStream in) throws IOException {
        while (true) {
            String[] parts = in.readUTF().split("\\s+", 3);
            if (END_CMD.equals(parts[0])) {
                return false;
            }
            if (!AUTH_CMD.equals(parts[0]) || parts.length < 3) {
                write(AUTHERR_CMD + " Ошибка авторизации");
                continue;
            }
            String candidateLogin = parts[1];
            String candidateUsername;
            try {
                candidateUsername = myServer.getAuthService().getUsernameByLoginAndPassword(candidateLogin, parts[2]);
            } catch (SQLException e) {
                logger.error("Ошибка доступа к базе пользователей", e);
                write(AUTHERR_CMD + " Сервис авторизации недоступен");
                continue;
            }
            if (candidateUsername == null) {
                write(AUTHERR_CMD + " Логин или пароль не соответствуют действительности");
                logger.info("Неверный логин или пароль для {}", candidateLogin);
                continue;
            }
            this.login = candidateLogin;
            this.username = candidateUsername;
            if (!myServer.subscribe(this)) {
                this.login = null;
                this.username = null;
                write(AUTHERR_CMD + " Учетная запись уже используется");
                continue;
            }
            myServer.broadcastServerMessage(candidateUsername + " подключился к чату", this);
            logger.info("Пользователь {} подключился к чату", candidateUsername);
            return true;
        }
    }

    private void readMessages(DataInputStream in) throws IOException {
        while (true) {
            String message = in.readUTF();
            String[] parts = message.split("\\s+", 2);
            String command = parts[0];
            String rest = parts.length > 1 ? parts[1] : "";

            if (END_CMD.equals(command)) {
                return;
            } else if (PUBLIC_MSG_CMD.equals(command) && !rest.isBlank()) {
                myServer.broadcastMessage(rest, this);
            } else if (PRIVATE_MSG_CMD.equals(command)) {
                handlePrivateMessage(rest);
            } else if (CHANGE_USERNAME_CMD.equals(command)) {
                handleChangeUsername(rest.trim());
            } else {
                sendServerMessage("Неизвестная команда: " + command);
            }
        }
    }

    private void handlePrivateMessage(String rest) {
        String[] parts = rest.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            sendServerMessage("Формат личного сообщения: /w <получатель> <текст>");
            return;
        }
        String recipient = parts[0];
        if (recipient.equals(username)) {
            sendServerMessage("Нельзя отправить личное сообщение самому себе");
        } else if (!myServer.privateMessage(parts[1], this, recipient)) {
            sendServerMessage("Пользователь " + recipient + " не в сети");
        }
    }

    private void handleChangeUsername(String newUsername) {
        if (newUsername.isEmpty() || newUsername.length() > MAX_USERNAME_LENGTH || newUsername.matches(".*\\s.*")) {
            write(CHANGE_USERNAME_ERR_CMD + " Имя должно быть одним словом длиной до " + MAX_USERNAME_LENGTH + " символов");
            return;
        }
        try {
            String error = myServer.changeUsername(this, newUsername);
            if (error != null) {
                write(CHANGE_USERNAME_ERR_CMD + " " + error);
            }
        } catch (SQLException e) {
            logger.error("Ошибка смены имени", e);
            write(CHANGE_USERNAME_ERR_CMD + " Ошибка базы данных");
        }
    }

    public void onSubscribed() {
        write(AUTHOK_CMD + " " + username);
    }

    public void onUsernameChanged(String newUsername) {
        logger.info("{} сменил имя на {}", username, newUsername);
        this.username = newUsername;
        write(CHANGE_USERNAME_OK_CMD + " " + newUsername);
    }

    public String getLogin() {
        return login;
    }

    public String getUsername() {
        return username;
    }

    public void sendClientMessage(String sender, String message) {
        write(String.format("%s %s %s", CLIENT_MSG_CMD, sender, message));
    }

    public void sendPrivateMessage(String sender, String recipient, String message) {
        write(String.format("%s %s %s %s", PRIVATE_DELIVERY_CMD, sender, recipient, message));
    }

    public void sendServerMessage(String message) {
        write(SERVER_MSG_CMD + " " + message);
    }

    public void sendUserList(List<String> usernames) {
        write(usernames.isEmpty() ? USER_LIST_CMD : USER_LIST_CMD + " " + String.join(" ", usernames));
    }

    public void disconnect() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            logger.warn("Ошибка закрытия сокета", e);
        }
    }

    private synchronized void write(String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            logger.debug("Не удалось отправить сообщение клиенту {}: {}", username, e.getMessage());
            disconnect();
        }
    }
}
