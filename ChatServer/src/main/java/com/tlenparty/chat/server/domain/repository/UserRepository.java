package com.tlenparty.chat.server.domain.repository;

import com.tlenparty.chat.server.domain.model.UserAccount;

import java.util.Optional;

public interface UserRepository {

    Optional<UserAccount> findByLogin(String login);

    boolean existsByUsername(String username);

    boolean updateUsername(String login, String newUsername);
}
