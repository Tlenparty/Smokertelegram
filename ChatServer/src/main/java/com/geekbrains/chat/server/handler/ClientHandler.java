package com.geekbrains.chat.server.handler;

import com.geekbrains.chat.server.MyServer;
import com.geekbrains.chat.server.auth.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {

    private static final String AUTH_CMD_PREFIX = "/auth";
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // Если аут окей
    private static final String AUTHERR_CMD_PREFIX = "/autherr"; // Если ошибка
    private static final String PRIVATE_MSG_PREFIX = "/w";  // для лс
    private static final String CLIENT_MSG_PREFIX = "/clientMsg"; // сигнал о завершении
    private static final String SERVER_MSG_PREFIX = "/serverMsg"; // сигнал о завершении
    private static final String END_CMD = "/end"; // сигнал о завершении
    private static final String USER_LIST = "/userList";


    private final MyServer myServer;
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
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
                System.out.println(e.getMessage());
            } catch (SQLException | ClassNotFoundException throwables) {
                throwables.printStackTrace();
            }

        }).start();
    }

    private void authentication() throws IOException, SQLException, ClassNotFoundException {
        String message = in.readUTF();


        while (true) {
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                String[] parts = message.split("\\s+", 3);
                 String login = parts[1];
                String password = parts[2];
                AuthService authService = myServer.getAuthService();
                username = authService.getUsernameByLoginAndPassword(login, password);
                if (username != null) {
                    if (myServer.isUsernameBusy(username)) {
                        out.writeUTF(String.format("%s %s", AUTHERR_CMD_PREFIX, "Логин уже используется"));
                    }
                    out.writeUTF(String.format("%s %s", AUTHOK_CMD_PREFIX, username));
                    myServer.broadcastMessage(String.format(">>>>> %s подключился к чату", username), this, true);
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
                }
            } else {
                out.writeUTF(String.format("%s %s", AUTHERR_CMD_PREFIX, "Ошибка авторизации"));
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
                myServer.privateMessage(message, this, recipient,false);
            } else {
                myServer.broadcastMessage(message, this, false);
            }
        }
    }

    public String getUsername() {
        return username;
    }


    public synchronized void sendMessage(String sender,  String message) throws IOException {
        if (sender == null) {
            out.writeUTF(String.format("%s %s", SERVER_MSG_PREFIX, message));
        }
        else {
            out.writeUTF(String.format("%s %s %s ", CLIENT_MSG_PREFIX, sender,  message));
        }
    }

    public synchronized void sendMessage(String sender, String recipient, String message) throws IOException {
        if (sender == null) {
            out.writeUTF(String.format("%s %s", SERVER_MSG_PREFIX, message));
        }
        else {
            out.writeUTF(String.format("%s %s %s %s", PRIVATE_MSG_PREFIX, sender, recipient, message));
        }
    }

}
