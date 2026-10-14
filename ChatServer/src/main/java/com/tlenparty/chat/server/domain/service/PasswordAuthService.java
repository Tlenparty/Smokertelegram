package com.tlenparty.chat.server.domain.service;

import com.tlenparty.chat.server.domain.model.User;
import com.tlenparty.chat.server.domain.model.UserAccount;
import com.tlenparty.chat.server.domain.repository.UserRepository;
import com.tlenparty.chat.server.domain.security.PasswordHasher;

import java.util.Optional;

public class PasswordAuthService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public PasswordAuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<User> authenticate(String login, String password) {
        if (login == null || login.isBlank() || password == null) {
            return Optional.empty();
        }
        return userRepository.findByLogin(login)
                .filter(account -> passwordHasher.matches(password, account.passwordHash()))
                .map(UserAccount::toUser);
    }
}
