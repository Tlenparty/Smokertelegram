package com.tlenparty.chat.client.domain;

public record AuthResult(boolean success, String username, String error) {

    public static AuthResult success(String username) {
        return new AuthResult(true, username, null);
    }

    public static AuthResult failure(String error) {
        return new AuthResult(false, null, error);
    }
}
