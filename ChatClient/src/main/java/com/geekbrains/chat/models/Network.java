package com.geekbrains.chat.models;

import com.geekbrains.chat.NetworkClient;
import com.geekbrains.chat.controllers.ChatController;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Network {
    private static final String AUTH_CMD_PREFIX = "/auth";
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // Если аут окей
    private static final String AUTHERR_CMD_PREFIX = "/autherr"; // Если ошибка
    private static final String PRIVATE_MSG_PREFIX = "/w";  // для лс
    private static final String CLIENT_MSG_PREFIX = "/clientMsg"; // сигнал о завершении сообщения
    private static final String SERVER_MSG_PREFIX = "/serverMsg"; // сигнал о завершении
    private static final String END_CMD = "/end"; // сигнал о завершении
    private static final String USER_LIST = "/userList";
    public static final String CHANGE_USERNAME_PREFIX = "/changeUsername";
    public static List<String> userList = new ArrayList<>();

    private static final int SERVER_PORT = 8189;
    private static final String SERVER_HOST = "localhost";

    private final int port;
    private final String host;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    private String username;

    public Network() {
        this(SERVER_PORT, SERVER_HOST);

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
            System.out.println("Соединение не было установлено");
            e.printStackTrace();
            return false;
        }
    }


    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DataInputStream getIn() {
        return in;
    }

    public String getUsername() {
        return username;
    }

    public DataOutputStream getOut() {
        return out;
    }


    public void waitMessage(ChatController chatController) {
        // Должен создавать поток. Который блокируется
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    // Будем ждать из потока
                    String message = in.readUTF();

                    if (message.startsWith(USER_LIST)) {
                        String[] parts = message.split("\\s+");
                        userList.clear();
                        userList.addAll(Arrays.asList(parts).subList(1, parts.length));
                        Platform.runLater(() -> chatController.newUserList());

                    } else if (message.startsWith(CLIENT_MSG_PREFIX)) {
                        String[] parts = message.split("\\s+", 3);
                        String sender = parts[1];
                        String msgBody = parts[2];

                        Platform.runLater(() -> chatController.appendMessage(String.format("%s: %s", sender, msgBody)));

                    } else if (message.startsWith(PRIVATE_MSG_PREFIX)) {
                        String[] parts = message.split("\\s+", 4);
                        String sender = parts[1];
                        String msgBody = parts[3];

                        Platform.runLater(() -> chatController.appendMessage(String.format("%s: %s", sender, msgBody)));

                    } else if (message.startsWith(SERVER_MSG_PREFIX) || message.startsWith(CHANGE_USERNAME_PREFIX)) {
                        String[] parts = message.split("\\s+", 2);
                        Platform.runLater(() -> chatController.appendMessage(parts[1]));
                    } else {
                        Platform.runLater(() -> NetworkClient.showErrorMessage("Неизвестная команда", message, ""));
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Соединение потеряно");
                NetworkClient.showErrorMessage("Ошибка подключения", "", e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();


    }

    public String sendAuthCommand(String login, String password) {

        try {

            sendMessage(String.format("%s %s %s", AUTH_CMD_PREFIX, login, password));
            String response = in.readUTF();

            if (response.startsWith(AUTHOK_CMD_PREFIX)) {
                this.username = response.split("\\s+", 2)[1];
                return null; // Возвращаем ошибку
            }
            return response.split("\\s+", 2)[1];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }


    public void sendPrivateMessage(String message, String recipient) throws IOException {
        message = PRIVATE_MSG_PREFIX + " " + recipient + " " + message;
        out.writeUTF(message);
    }

    public void sendExitMessage() {
        try {
            out.writeUTF(END_CMD);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendChangeNameCommand(String oldUsername, String newUsername) {

        try {
            sendMessage(String.format("%s %s %s", CHANGE_USERNAME_PREFIX, oldUsername, newUsername));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
