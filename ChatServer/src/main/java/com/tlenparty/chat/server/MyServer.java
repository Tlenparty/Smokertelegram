package com.tlenparty.chat.server;

import com.tlenparty.chat.server.auth.AuthService;
import com.tlenparty.chat.server.handler.ClientHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyServer implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(MyServer.class);

    private final ServerSocket serverSocket;
    private final AuthService authService;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public MyServer(int port, AuthService authService) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.authService = authService;
    }

    public void start() {
        logger.info("Сервер запущен на порту {}", serverSocket.getLocalPort());
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                logger.info("Клиент подключился: {}", clientSocket.getRemoteSocketAddress());
                executor.execute(new ClientHandler(this, clientSocket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    logger.error("Ошибка создания нового подключения", e);
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("Ошибка закрытия серверного сокета", e);
        }
        clients.forEach(ClientHandler::disconnect);
        executor.shutdownNow();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean subscribe(ClientHandler clientHandler) {
        if (isUsernameBusy(clientHandler.getUsername())) {
            return false;
        }
        clients.add(clientHandler);
        clientHandler.onSubscribed();
        broadcastUserList();
        return true;
    }

    public synchronized void unSubscribe(ClientHandler clientHandler) {
        if (clients.remove(clientHandler)) {
            broadcastUserList();
        }
    }

    public synchronized boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (username.equals(client.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendClientMessage(sender.getUsername(), message);
            }
        }
    }

    public void broadcastServerMessage(String message, ClientHandler except) {
        for (ClientHandler client : clients) {
            if (client != except) {
                client.sendServerMessage(message);
            }
        }
    }

    public synchronized String changeUsername(ClientHandler client, String newUsername) throws SQLException {
        if (newUsername.equals(client.getUsername())) {
            return "Это уже ваше имя";
        }
        if (isUsernameBusy(newUsername) || authService.isUsernameTaken(newUsername)) {
            return "Имя " + newUsername + " уже занято";
        }
        if (!authService.changeUsername(client.getLogin(), newUsername)) {
            return "Учетная запись не найдена";
        }
        String oldUsername = client.getUsername();
        client.onUsernameChanged(newUsername);
        broadcastServerMessage(String.format("%s сменил имя на %s", oldUsername, newUsername), null);
        broadcastUserList();
        return null;
    }

    public boolean privateMessage(String message, ClientHandler sender, String recipient) {
        for (ClientHandler client : clients) {
            if (recipient.equals(client.getUsername())) {
                client.sendPrivateMessage(sender.getUsername(), recipient, message);
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastUserList() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            usernames.add(client.getUsername());
        }
        for (ClientHandler client : clients) {
            client.sendUserList(usernames);
        }
    }
}
