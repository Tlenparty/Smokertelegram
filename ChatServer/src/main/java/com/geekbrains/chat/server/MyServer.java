package com.geekbrains.chat.server;

import com.geekbrains.chat.server.auth.AuthService;
import com.geekbrains.chat.server.auth.BaseAuthService;
import com.geekbrains.chat.server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class MyServer {

    // 2. Cоздадим конструктор.
    private final ServerSocket serverSocket;
    private final AuthService authService;
    private final List<ClientHandler> clients = new ArrayList<>();

    public List<ClientHandler> getClients() {
        return clients;
    }


    public MyServer(int port) throws IOException {
        // 3. Запускаем сервер и сервис аунтификации
        this.serverSocket = new ServerSocket(port);
        this.authService = new BaseAuthService();

    }


    public void start() throws IOException {
        // 4 Создание пользователей. Хэндлеровы
        System.out.println("Сервер запущен");
        authService.start();
        try {
            while (true) {
                //Ждем подключения к серверу
                waitAndProcessNewClientConnection();
            }
        } catch (IOException e) {
            System.out.println("Ошибка создания нового подключения");
            e.printStackTrace();

        } finally {
            serverSocket.close();
        }
    }

    // ждет и обрабатывает соединение клиента
    private void waitAndProcessNewClientConnection() throws IOException {
        System.out.println("Ожидание пользователя...");
        Socket clientSocket = serverSocket.accept(); // слушает
        System.out.println("Клиент подключился!");
        // 5 clientSocket сокет от нашего клиента. И его отдадим тому кто раб. с ним (в поток)
        processClientConnection(clientSocket);
    }

    private void processClientConnection(Socket clientSocket) throws IOException {
        // Сылка на объект и на сокет
        ClientHandler clientHandler = new ClientHandler(this, clientSocket);
        clientHandler.handle(); // организует логику подключения. Создаст потоки процесса


    }

    public AuthService getAuthService() {
        return authService;
    }

    public void subscribe(ClientHandler clientHandler) {
        // метод будет работаь с коллекцией ClientHandler
        // берет хэндлер и добавляет
        clients.add(clientHandler);
    }

    public void unSubscribe(ClientHandler clientHandler) {
        // Отписка
        clients.remove(clientHandler);
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastMessage(String message, ClientHandler sender, boolean isServerInfoMsg) throws IOException {
        for (ClientHandler client : clients) {
            // Если клиент = отправитель. То игнорируем ч/з continue;
            if (client == sender) {
                continue;
            }
            // sender м.б. null
            client.sendMessage(isServerInfoMsg ? null : sender.getUsername(), message);
        }
    }

    public void privateMessage(String message, ClientHandler sender, String recipient, boolean isServerInfoMsg) throws IOException {

        for (ClientHandler client : clients) {
            if(recipient.equals(client.getUsername())) {
                client.sendMessage(isServerInfoMsg ? null : sender.getUsername(), recipient, message);
            }

        }
    }


    public void broadcastUserList(String userList, boolean isServerInfoMsg) throws IOException {
        for (ClientHandler client : clients) {
            client.sendUserList(isServerInfoMsg ? null : userList);
        }
    }
}
