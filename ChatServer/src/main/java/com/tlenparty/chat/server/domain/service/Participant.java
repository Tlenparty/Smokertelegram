package com.tlenparty.chat.server.domain.service;

import com.tlenparty.chat.server.domain.model.Message;
import com.tlenparty.chat.server.domain.model.User;

import java.util.List;

public interface Participant {

    void joined(User user);

    void renamed(String newUsername);

    void deliver(Message message);

    void userJoined(String username);

    void userLeft(String username);

    void userRenamed(String oldUsername, String newUsername);

    void usersOnline(List<String> usernames);
}
