package com.tlenparty.chat.server.domain.security;

public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String storedHash);
}
