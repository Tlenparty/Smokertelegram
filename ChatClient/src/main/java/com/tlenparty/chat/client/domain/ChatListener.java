package com.tlenparty.chat.client.domain;

import java.util.List;

public interface ChatListener {

    void onPublicMessage(String sender, String text);

    void onPrivateMessage(String sender, String text);

    void onServerMessage(String text);

    void onUserList(List<String> usernames);

    void onUsernameChanged(String newUsername);

    void onUsernameChangeRejected(String reason);

    void onConnectionLost(String reason);
}
