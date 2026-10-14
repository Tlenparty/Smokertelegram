package com.tlenparty.chat.server.domain.model;

import java.util.Objects;

public record User(String login, String username) {

    public User {
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(username, "username");
    }

    public User withUsername(String newUsername) {
        return new User(login, newUsername);
    }
}
