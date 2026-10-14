package com.tlenparty.chat.controllers;

import com.tlenparty.chat.NetworkClient;
import com.tlenparty.chat.models.Network;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatController {

    private static final String EVERYONE = "Всем";
    private static final int HISTORY_LINES = 100;
    private static final Logger logger = LogManager.getLogger(ChatController.class);

    @FXML
    private TextArea chatHistory;

    @FXML
    public ListView<String> usersList;

    @FXML
    private Label usernameTitle;

    @FXML
    private TextField textField;

    @FXML
    private Button sendButton;

    @FXML
    private ChoiceBox<String> userSend;

    @FXML
    private TextField changeNameFieldID;

    @FXML
    private Hyperlink changeNameLinkID;

    private Network network;
    private String selectedRecipient;
    private List<String> onlineUsers = List.of();

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setLabel(String usernameTitle) {
        this.usernameTitle.setText(usernameTitle);
    }

    @FXML
    public void initialize() {
        updateUserList(List.of());
        textField.setOnAction(event -> sendMessage());
        sendButton.setOnAction(event -> sendMessage());

        usersList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = usersList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                usersList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedRecipient = null;
                    } else {
                        selectionModel.select(index);
                        selectedRecipient = cell.getItem();
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }

    public void updateUserList(List<String> users) {
        onlineUsers = List.copyOf(users);
        String ownName = usernameTitle.getText();
        List<String> recipients = new ArrayList<>();
        recipients.add(EVERYONE);
        for (String user : onlineUsers) {
            if (!user.equals(ownName)) {
                recipients.add(user);
            }
        }
        userSend.setItems(FXCollections.observableArrayList(recipients));
        userSend.setValue(EVERYONE);
        usersList.setItems(FXCollections.observableArrayList(onlineUsers));
        if (selectedRecipient != null && !recipients.contains(selectedRecipient)) {
            selectedRecipient = null;
        }
    }

    private void sendMessage() {
        String message = textField.getText();
        if (message.isBlank()) {
            return;
        }
        String recipient = selectedRecipient != null ? selectedRecipient : userSend.getValue();
        boolean isPrivate = recipient != null && !EVERYONE.equals(recipient);
        try {
            if (isPrivate) {
                network.sendPrivateMessage(message, recipient);
                appendMessage(String.format("Я → %s: %s", recipient, message));
            } else {
                network.sendMessage(message);
                appendMessage("Я: " + message);
            }
            textField.clear();
        } catch (IOException e) {
            logger.error("Ошибка при отправке сообщения", e);
            NetworkClient.showErrorMessage("Ошибка подключения", "Ошибка при отправке сообщения", e.getMessage());
        }
    }

    public void appendMessage(String message) {
        String timestamp = DateFormat.getInstance().format(new Date());
        String entry = timestamp + System.lineSeparator() + message + System.lineSeparator();
        try {
            Path file = historyFile();
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Не удалось сохранить историю", e);
        }
        chatHistory.appendText(entry + System.lineSeparator());
    }

    public void chatHistoryDisplay() {
        Path file = historyFile();
        if (!Files.exists(file)) {
            chatHistory.appendText("История переписки отсутствует" + System.lineSeparator());
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines.subList(Math.max(0, lines.size() - HISTORY_LINES), lines.size())) {
                chatHistory.appendText(line + System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error("Не удалось прочитать историю", e);
        }
    }

    private Path historyFile() {
        String dir = System.getProperty("chat.history.dir", Path.of(System.getProperty("user.home"), ".smokertelegram").toString());
        return Path.of(dir, String.format("chatHistory_%s.txt", network.getLogin()));
    }

    public void openChangeNameField() {
        changeNameFieldID.setVisible(true);
        changeNameLinkID.setVisible(false);
        changeNameFieldID.setText(usernameTitle.getText());
    }

    public void updateUsername() {
        String newUsername = changeNameFieldID.getText().trim();
        if (newUsername.isEmpty() || newUsername.matches(".*\\s.*")) {
            NetworkClient.showErrorMessage("Ошибка смены имени", "Ошибка ввода", "Имя должно быть одним словом без пробелов");
            return;
        }
        network.sendChangeNameCommand(newUsername);
        changeNameLinkID.setVisible(true);
        changeNameFieldID.setVisible(false);
    }

    public void onUsernameChanged(String newUsername) {
        usernameTitle.setText(newUsername);
        updateUserList(onlineUsers);
    }

    public void onUsernameChangeRejected(String reason) {
        NetworkClient.showErrorMessage("Ошибка смены имени", "Сервер отклонил смену имени", reason);
    }
}
