package com.tlenparty.chat.server.app;

import com.tlenparty.chat.common.protocol.Protocol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public final class ServerConfig {

    public static final int DEFAULT_PORT = Protocol.DEFAULT_PORT;
    public static final String PORT_PROPERTY = "chat.port";
    public static final String PORT_ENV = "CHAT_PORT";
    public static final String DB_PROPERTY = "chat.db";
    public static final String DB_ENV = "CHAT_DB";

    static final List<Path> DEFAULT_DB_LOCATIONS = List.of(
            Path.of("mainDB.db"),
            Path.of("src", "main", "resources", "mainDB.db"),
            Path.of("ChatServer", "src", "main", "resources", "mainDB.db"));

    private final int port;
    private final Path databasePath;

    public ServerConfig(int port, Path databasePath) {
        this.port = port;
        this.databasePath = databasePath;
    }

    public static ServerConfig resolve(String[] args, Properties properties, Map<String, String> env) {
        String portValue = firstNonBlank(arg(args, 0), properties.getProperty(PORT_PROPERTY), env.get(PORT_ENV))
                .orElse(String.valueOf(DEFAULT_PORT));
        int port;
        try {
            port = Integer.parseInt(portValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + portValue, e);
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port out of range: " + port);
        }

        Path databasePath = firstNonBlank(arg(args, 1), properties.getProperty(DB_PROPERTY), env.get(DB_ENV))
                .map(Path::of)
                .orElseGet(ServerConfig::defaultDatabasePath);
        if (!Files.isRegularFile(databasePath)) {
            throw new IllegalArgumentException("Database file not found: " + databasePath.toAbsolutePath());
        }
        return new ServerConfig(port, databasePath);
    }

    public int port() {
        return port;
    }

    public Path databasePath() {
        return databasePath;
    }

    public String jdbcUrl() {
        return "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    private static Path defaultDatabasePath() {
        return DEFAULT_DB_LOCATIONS.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(DEFAULT_DB_LOCATIONS.get(0));
    }

    private static String arg(String[] args, int index) {
        return args != null && args.length > index ? args[index] : null;
    }

    private static Optional<String> firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
