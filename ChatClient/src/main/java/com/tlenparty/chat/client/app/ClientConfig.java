package com.tlenparty.chat.client.app;

import com.tlenparty.chat.common.protocol.Protocol;

import java.nio.file.Path;

public record ClientConfig(String host, int port, Path historyDirectory) {

    public static ClientConfig fromEnvironment() {
        String host = setting("chat.host", "CHAT_HOST", Protocol.DEFAULT_HOST);
        int port = Integer.parseInt(setting("chat.port", "CHAT_PORT", String.valueOf(Protocol.DEFAULT_PORT)));
        String defaultHistory = Path.of(System.getProperty("user.home"), ".smokertelegram").toString();
        Path historyDirectory = Path.of(setting("chat.history.dir", "CHAT_HISTORY_DIR", defaultHistory));
        return new ClientConfig(host, port, historyDirectory);
    }

    private static String setting(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
