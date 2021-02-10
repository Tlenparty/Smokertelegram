package com.geekbrains.chat.server.handler;

import com.geekbrains.chat.server.MyServer;
import com.geekbrains.chat.server.auth.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
            // аутентификация  + чтение ожидания и чтение сообщений
            try {
                authentication();
                readMessage(); // Сможет нам вернуть ощибку

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }

        }).start();
    }


    private void authentication() throws IOException {
        // во вр. аутнефикации клинет отправит логин и пароль
        String message = in.readUTF();// принимаем сообщение c консоли
        // Нужыне перфиксы для мессаджа . И для этого будем строку парсить
        // Чтобы по-разному наши сообщения делать. Отправка кому-то, логин пароль, всем

        while (true) {
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                // если сообщение начинается на /auth то
                String[] parts = message.split("\\s+", 3); // регулярное выражение
                // ожидаем 1 пробел или несколько и будем делить на 3 части наше сообщение
                // Получаем логин и пароль
                String login = parts[1];
                String password = parts[2];
                // CH будет обращаться к серверу и забирать у него сервисаунтифик
                AuthService authService = myServer.getAuthService();
                username = authService.getUsernameByLoginAndPassword(login, password);
                // на выходе ждем имя пользователя
                if (username != null) {
                    // никнейм незанят
                    if (myServer.isUsernameBusy(username)) {
                        // Если клиент занят, то оповестим с сервера, клинету об этом
                        out.writeUTF(String.format("%s %s", AUTHERR_CMD_PREFIX, "Логин уже используется"));
                    }
                    out.writeUTF(String.format("%s %s", AUTHOK_CMD_PREFIX, username));
                    // оповестим пользователей о подключении новичка
                    // Будет строка.Для продкаста передадим handle ч/з this. True - флаг серверное ли сообщение или нет
                    myServer.broadcastMessage(String.format(">>>>> %s подключился к чату", username), this, true);
                    // зарегистрировать клиента
                    //должен быть список клиентов. уточнить не подключен ли уже пользлователь.
                    //myServer будет хранить все хэндлеры с пользователями (subscribe)
                    myServer.subscribe(this); // на вход будем  отдавать текущий хэндлер

                    String userList = USER_LIST;

                    for (ClientHandler client : myServer.getClients()) {
                        userList = userList + " " + client.getUsername();
                    }
                    myServer.broadcastUserList(userList, false);

                    break;

                } else {
                    // То не прошла авторизация
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
                myServer.broadcastUserList(userList, false);  // Отправка сообщений пользавателям
                myServer.broadcastUserList(userList, false);
                return; // Выхожим. Завершаем действие хэндлера

            } else if (message.startsWith(PRIVATE_MSG_PREFIX)) {
                parts = message.split("\\s+", 3);
                recipient = parts[1];
                message = parts[2];
                myServer.privateMessage(message, this, recipient,false);
            } else {
                // Мы будем просто выводить на экран всем. false значит не серверное сообщение
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
