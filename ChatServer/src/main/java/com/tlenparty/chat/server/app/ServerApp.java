package com.tlenparty.chat.server.app;

import com.tlenparty.chat.server.data.Pbkdf2PasswordHasher;
import com.tlenparty.chat.server.data.SqliteUserRepository;
import com.tlenparty.chat.server.domain.repository.UserRepository;
import com.tlenparty.chat.server.domain.service.ChatRoom;
import com.tlenparty.chat.server.domain.service.PasswordAuthService;
import com.tlenparty.chat.server.transport.SocketServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class ServerApp {

    private static final Logger logger = LogManager.getLogger(ServerApp.class);
    private static final int AUTH_TIMEOUT_MILLIS = 120_000;

    private ServerApp() {
    }

    public static void main(String[] args) {
        ServerConfig config;
        try {
            config = ServerConfig.resolve(args, System.getProperties(), System.getenv());
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            System.exit(2);
            return;
        }

        logger.info("База данных: {}", config.databasePath().toAbsolutePath());
        try (SocketServer server = createServer(config)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.serve();
        } catch (IOException e) {
            logger.error("Ошибка запуска сервера", e);
            System.exit(1);
        }
    }

    public static SocketServer createServer(ServerConfig config) throws IOException {
        UserRepository userRepository = new SqliteUserRepository(config.jdbcUrl());
        PasswordAuthService authService = new PasswordAuthService(userRepository, new Pbkdf2PasswordHasher());
        ChatRoom chatRoom = new ChatRoom(userRepository);
        return new SocketServer(config.port(), authService, chatRoom, AUTH_TIMEOUT_MILLIS);
    }
}
