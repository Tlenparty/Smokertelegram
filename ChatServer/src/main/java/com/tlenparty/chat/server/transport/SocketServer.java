package com.tlenparty.chat.server.transport;

import com.tlenparty.chat.server.domain.service.AuthService;
import com.tlenparty.chat.server.domain.service.ChatRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(SocketServer.class);

    private final ServerSocket serverSocket;
    private final AuthService authService;
    private final ChatRoom chatRoom;
    private final int authTimeoutMillis;
    private final Set<ClientHandler> handlers = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SocketServer(int port, AuthService authService, ChatRoom chatRoom, int authTimeoutMillis) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.authService = authService;
        this.chatRoom = chatRoom;
        this.authTimeoutMillis = authTimeoutMillis;
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serve() {
        logger.info("Сервер запущен на порту {}", port());
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                logger.info("Клиент подключился: {}", socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, authService, chatRoom, authTimeoutMillis, handlers::remove);
                handlers.add(handler);
                executor.execute(handler);
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    logger.error("Ошибка приема подключения", e);
                }
            }
        }
        logger.info("Сервер остановлен");
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("Ошибка закрытия серверного сокета", e);
        }
        handlers.forEach(ClientHandler::disconnect);
        executor.shutdownNow();
    }
}
