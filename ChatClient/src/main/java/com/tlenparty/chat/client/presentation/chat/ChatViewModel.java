package com.tlenparty.chat.client.presentation.chat;

import com.tlenparty.chat.client.domain.ChatGateway;
import com.tlenparty.chat.client.domain.ChatListener;
import com.tlenparty.chat.client.domain.HistoryRepository;
import com.tlenparty.chat.client.presentation.ErrorReporter;
import com.tlenparty.chat.common.protocol.Command;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatViewModel implements ChatListener {

    public static final String EVERYONE = "Всем";

    private static final int HISTORY_LINES = 100;
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

    private final ChatGateway gateway;
    private final HistoryRepository history;
    private final ErrorReporter errorReporter;
    private final ReadOnlyStringWrapper username;
    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final ObservableList<String> recipients = FXCollections.observableArrayList(EVERYONE);
    private Consumer<String> transcript = text -> {
    };

    public ChatViewModel(String username, ChatGateway gateway, HistoryRepository history, ErrorReporter errorReporter) {
        this.username = new ReadOnlyStringWrapper(username);
        this.gateway = gateway;
        this.history = history;
        this.errorReporter = errorReporter;
    }

    public ReadOnlyStringProperty usernameProperty() {
        return username.getReadOnlyProperty();
    }

    public ObservableList<String> onlineUsers() {
        return onlineUsers;
    }

    public ObservableList<String> recipients() {
        return recipients;
    }

    public void setTranscript(Consumer<String> transcript) {
        this.transcript = transcript;
    }

    public void loadHistory() {
        List<String> lines = history.lastLines(HISTORY_LINES);
        if (lines.isEmpty()) {
            transcript.accept("История переписки отсутствует" + System.lineSeparator());
            return;
        }
        lines.forEach(line -> transcript.accept(line + System.lineSeparator()));
    }

    public boolean send(String text, String recipient) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean isPrivate = recipient != null && !EVERYONE.equals(recipient);
        try {
            if (isPrivate) {
                gateway.sendPrivate(recipient, text);
                append(String.format("Я → %s: %s", recipient, text));
            } else {
                gateway.sendPublic(text);
                append("Я: " + text);
            }
            return true;
        } catch (IOException | IllegalArgumentException e) {
            errorReporter.report("Ошибка отправки", "Не удалось отправить сообщение", e.getMessage());
            return false;
        }
    }

    public boolean requestRename(String newUsername) {
        String candidate = newUsername == null ? "" : newUsername.trim();
        if (!Command.isToken(candidate)) {
            errorReporter.report("Ошибка смены имени", "Ошибка ввода", "Имя должно быть одним словом без пробелов");
            return false;
        }
        try {
            gateway.changeUsername(candidate);
            return true;
        } catch (IOException e) {
            errorReporter.report("Ошибка смены имени", "Не удалось отправить запрос", e.getMessage());
            return false;
        }
    }

    @Override
    public void onPublicMessage(String sender, String text) {
        append(String.format("%s: %s", sender, text));
    }

    @Override
    public void onPrivateMessage(String sender, String text) {
        append(String.format("%s (лично): %s", sender, text));
    }

    @Override
    public void onServerMessage(String text) {
        append(text);
    }

    @Override
    public void onUserList(List<String> usernames) {
        onlineUsers.setAll(usernames);
        refreshRecipients();
    }

    @Override
    public void onUsernameChanged(String newUsername) {
        username.set(newUsername);
        refreshRecipients();
    }

    @Override
    public void onUsernameChangeRejected(String reason) {
        errorReporter.report("Ошибка смены имени", "Сервер отклонил смену имени", reason);
    }

    @Override
    public void onConnectionLost(String reason) {
        errorReporter.report("Ошибка подключения", "Соединение с сервером потеряно", reason);
    }

    private void refreshRecipients() {
        List<String> updated = new ArrayList<>();
        updated.add(EVERYONE);
        onlineUsers.stream().filter(user -> !user.equals(username.get())).forEach(updated::add);
        recipients.setAll(updated);
    }

    private void append(String line) {
        String entry = TIMESTAMP.format(LocalDateTime.now()) + System.lineSeparator() + line;
        history.append(entry);
        transcript.accept(entry + System.lineSeparator() + System.lineSeparator());
    }
}
