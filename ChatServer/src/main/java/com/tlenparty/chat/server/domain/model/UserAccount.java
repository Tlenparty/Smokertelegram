package com.tlenparty.chat.server.domain.model;

public record UserAccount(String login, String passwordHash, String username) {

    public User toUser() {
        return new User(login, username);
    }
}
