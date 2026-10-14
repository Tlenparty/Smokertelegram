package com.tlenparty.chat.server.domain.model;

import java.util.Objects;

public record Message(String sender, String recipient, String text) {

    public Message {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(text, "text");
    }

    public static Message broadcast(String sender, String text) {
        return new Message(sender, null, text);
    }

    public static Message direct(String sender, String recipient, String text) {
        return new Message(sender, Objects.requireNonNull(recipient, "recipient"), text);
    }

    public boolean isPrivate() {
        return recipient != null;
    }
}
