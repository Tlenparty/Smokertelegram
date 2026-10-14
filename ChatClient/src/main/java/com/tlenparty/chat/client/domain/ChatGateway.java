package com.tlenparty.chat.client.domain;

import java.io.IOException;

public interface ChatGateway extends AutoCloseable {

    void connect() throws IOException;

    AuthResult authenticate(String login, String password);

    void listen(ChatListener listener);

    void sendPublic(String text) throws IOException;

    void sendPrivate(String recipient, String text) throws IOException;

    void changeUsername(String newUsername) throws IOException;

    @Override
    void close();
}
