package com.geekbrains.chat.server.handler;

import com.geekbrains.chat.server.MyServer;
import com.geekbrains.chat.server.auth.AuthService;
import com.geekbrains.chat.server.auth.BaseAuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {
    private static Logger logger = LogManager.getLogger(ClientHandler.class);
    private static final String AUTH_CMD_PREFIX = "/auth";
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // Если аут окей
    private static final String AUTHERR_CMD_PREFIX = "/autherr"; // Если ошибка
    private static final String PRIVATE_MSG_PREFIX = "/w";  // для лс
    private static final String CLIENT_MSG_PREFIX = "/clientMsg"; // сигнал о завершении
    private static final String SERVER_MSG_PREFIX = "/serverMsg"; // сигнал о завершении
    private static final String END_CMD = "/end"; // сигнал о завершении
    public static final String CHANGE_USERNAME_PREFIX = "/changeUsername";
    private static final String USER_LIST = "/userList";


    private final MyServer myServer;
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    //  private static Statement stmt;
    private String username;

    // 6  Принимает myServer и clientSocket
    public ClientHandler(MyServer myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());
        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
            } catch (SQLException | ClassNotFoundException throwables) {
                logger.error(throwables.getMessage(), throwables);
            }

        }).start();
    }

    private void authentication() throws IOException, SQLException, ClassNotFoundException {

        while (true) {
            String message = in.readUTF();
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                String[] parts = message.split("\\s+", 3);
                String login = parts[1];
                String password = parts[2];
                AuthService authService = myServer.getAuthService();
                username = authService.getUsernameByLoginAndPassword(login, password);
                if (username != null) {
                    if (myServer.isUsernameBusy(username)) {
                        out.writeUTF(String.format("%s %s", AUTHERR_CMD_PREFIX, "Логин уже используется"));
                        logger.info("Логин уже используется");
                    }
                    out.writeUTF(String.format("%s %s", AUTHOK_CMD_PREFIX, username));
                    myServer.broadcastMessage(String.format(">>>>> %s подключился к чату", username), this, true);
                    logger.info("Пользователь" + username + " Подключился к чату");
                    myServer.subscribe(this);
                    String userList = USER_LIST;
                    for (ClientHandler client : myServer.getClients()) {
                        userList = userList + " " + client.getUsername();
                    }
                    myServer.broadcastUserList(userList, false);
                    break;
                } else {
                    out.writeUTF(String.format("%s %s", AUTHERR_CMD_PREFIX, "Логин или пароль не соответсвтуют" +
                            " действительности"));
                    logger.info("Логин или пароль неверный");
                }
            } else {
                out.writeUTF(String.format("%s %s", AUTHERR_CMD_PREFIX, "Ошибка авторизации"));
                logger.info("Ошибка авторизации");
            }
        }
    }


    public synchronized void sendUserList(String userLists) throws IOException {
        out.writeUTF(String.format("%s", userLists));
    }


    private void readMessage() throws IOException {
        while (true) {
            String message = in.readUTF();
            String[] parts;
            String recipient;
            System.out.println("message | " + username + ":" + message);

            if (message.startsWith(END_CMD)) {
                myServer.unSubscribe(this);
                String userList = USER_LIST;

                for (ClientHandler client : myServer.getClients()) {
                    userList = userList + " " + client.getUsername();
                }
                myServer.broadcastUserList(userList, false);
                myServer.broadcastUserList(userList, false);
                return;

            } else if (message.startsWith(PRIVATE_MSG_PREFIX)) {
                parts = message.split("\\s+", 3);
                recipient = parts[1];
                message = parts[2];
                myServer.privateMessage(message, this, recipient, false);
            } else if (message.startsWith(CHANGE_USERNAME_PREFIX)) {
                parts = message.split("\\s+", 3);
                String oldUsername = parts[1];
                String newUsername = parts[2];
                try {
                    BaseAuthService.connection();
                    int result = BaseAuthService.stmt.executeUpdate(String.format("UPDATE auth " +
                                    "SET username = '%s' " +
                                    "WHERE username = '%s'",
                            newUsername, oldUsername));
                    System.out.println("Имя сменилось");
                    myServer.broadcastMessage(String.format("%s >>>>>>>> сменил имя на %s", oldUsername, newUsername),
                            this, true);
                    logger.info(oldUsername + " сменил имя на " + newUsername);
                    System.out.println(result);
                    BaseAuthService.disconnect();
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage(), e);
                }
                String userList = USER_LIST;
                this.username = newUsername;
                for (ClientHandler client : myServer.getClients()) {
                    userList = userList + " " + client.getUsername();
                }
                myServer.broadcastUserList(userList, false);


            } else {
                myServer.broadcastMessage(message, this, false);
            }
        }
    }

    public String getUsername() {
        return username;
    }


    public synchronized void sendMessage(String sender, String message) throws IOException {
        if (sender == null) {
            out.writeUTF(String.format("%s %s", SERVER_MSG_PREFIX, message));
        } else {
            out.writeUTF(String.format("%s %s %s ", CLIENT_MSG_PREFIX, sender, message));
        }
    }

    public synchronized void sendMessage(String sender, String recipient, String message) throws IOException {
        if (sender == null) {
            out.writeUTF(String.format("%s %s", SERVER_MSG_PREFIX, message));
        } else {
            out.writeUTF(String.format("%s %s %s %s", PRIVATE_MSG_PREFIX, sender, recipient, message));
        }
    }

}
