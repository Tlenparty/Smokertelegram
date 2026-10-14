package com.tlenparty.chat.server.domain.service;

import com.tlenparty.chat.server.domain.model.User;

import java.util.Optional;

public interface AuthService {

    Optional<User> authenticate(String login, String password);
}
