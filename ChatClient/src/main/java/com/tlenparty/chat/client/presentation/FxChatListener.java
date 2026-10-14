package com.tlenparty.chat.client.presentation;

import com.tlenparty.chat.client.domain.ChatListener;
import javafx.application.Platform;

import java.util.List;

public class FxChatListener implements ChatListener {

    private final ChatListener delegate;

    public FxChatListener(ChatListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onPublicMessage(String sender, String text) {
        Platform.runLater(() -> delegate.onPublicMessage(sender, text));
    }

    @Override
    public void onPrivateMessage(String sender, String text) {
        Platform.runLater(() -> delegate.onPrivateMessage(sender, text));
    }

    @Override
    public void onServerMessage(String text) {
        Platform.runLater(() -> delegate.onServerMessage(text));
    }

    @Override
    public void onUserList(List<String> usernames) {
        Platform.runLater(() -> delegate.onUserList(usernames));
    }

    @Override
    public void onUsernameChanged(String newUsername) {
        Platform.runLater(() -> delegate.onUsernameChanged(newUsername));
    }

    @Override
    public void onUsernameChangeRejected(String reason) {
        Platform.runLater(() -> delegate.onUsernameChangeRejected(reason));
    }

    @Override
    public void onConnectionLost(String reason) {
        Platform.runLater(() -> delegate.onConnectionLost(reason));
    }
}
