package com.tlenparty.chat.client.app;

import com.tlenparty.chat.common.protocol.Protocol;

import java.nio.file.Path;

public record ClientConfig(String host, int port, Path historyDirectory) {

    public static ClientConfig fromSystemProperties() {
        String host = System.getProperty("chat.host", Protocol.DEFAULT_HOST);
        int port = Integer.getInteger("chat.port", Protocol.DEFAULT_PORT);
        Path historyDirectory = Path.of(System.getProperty("chat.history.dir",
                Path.of(System.getProperty("user.home"), ".smokertelegram").toString()));
        return new ClientConfig(host, port, historyDirectory);
    }
}
