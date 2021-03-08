package com.geekbrains.chat.controllers;


import com.geekbrains.chat.NetworkClient;
import com.geekbrains.chat.models.Network;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ChatController {
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

    private List<String> user = new ArrayList<>();
    private static Logger logger = LogManager.getLogger(ChatController.class);

    // NetworkChat(эко клиент) знает network.
    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setLabel(String usernameTitle) {
        this.usernameTitle.setText(usernameTitle);
    }

    @FXML
    public void initialize() {
        //userList.setItems(FXCollections.observableArrayList(NetworkClient.USERS_TEST_DATA));
        user.add(0, "Всем");
        user.addAll(Network.userList);
        user.remove(usernameTitle.getText());
        userSend.setItems(FXCollections.observableArrayList(user));
        userSend.setValue(user.get(0));
        usersList.setItems(FXCollections.observableArrayList(Network.userList));
        textField.setOnAction(event -> ChatController.this.sendMessage());
        sendButton.setOnAction(event -> ChatController.this.sendMessage());

        // Отправка сообщения по нажатию на пользователя.
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

    public void newUserList() {
        user.clear();
        user.add(0, "Всем");
        user.addAll(Network.userList);
        user.remove(usernameTitle.getText());
        userSend.setItems(FXCollections.observableArrayList(user));
        userSend.setValue(user.get(0));
        usersList.setItems(FXCollections.observableArrayList(Network.userList));
    }

    // Отправка сообщения на нетворк + вывод на экран
    private void sendMessage() {
        String message = textField.getText();
        appendMessage("Я: " + message); // добавляет текст.
        textField.clear();

        try {
            // Отправляем сообщение на сервер
            // network.sendMessage(message); // get.Out().writeUTF(message);
/*            if (userSend.getValue().equals("Всем")) {
                network.sendMessage(message);
            } */
            if (selectedRecipient != null) {
                network.sendPrivateMessage(message, selectedRecipient);
            } else {
                //network.sendPrivateMessage(message, userSend.getValue());
                network.sendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            NetworkClient.showErrorMessage("Ошибка подключения", "Ошибка при отправке сообщения", e.getMessage());
            logger.error("Ошибка подключения, ошибка при отправке сообщения\n" + e.getMessage(), e);
        }

    }


    public void appendMessage(String message) { // вывод на экран

        String timestamp = DateFormat.getInstance().format(new Date());
        File file = new File(String.format("ChatClient/src/main/resources/com/geekbrains/lib/chatHistory_%s.txt", network.getLogin()));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
            }
        } else {
            try (FileOutputStream writer = new FileOutputStream(file, true)) {
                writer.write(timestamp.getBytes(StandardCharsets.UTF_8));
                writer.write("\n".getBytes(StandardCharsets.UTF_8));
                writer.write(message.getBytes(StandardCharsets.UTF_8));
                writer.write("\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
            }

            chatHistory.appendText(timestamp);
            chatHistory.appendText(System.lineSeparator());
            chatHistory.appendText(message);
            chatHistory.appendText(System.lineSeparator());
            chatHistory.appendText(System.lineSeparator());
        }
    }

    public void chatHistoryDisplay() {
        File file = new File(String.format("ChatClient/src/main/resources/com/geekbrains/lib/chatHistory_%s.txt", network.getLogin()));
        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                String strLine;
                int counter = 0;
                while ((strLine = in.readLine()) != null) {
                    chatHistory.appendText(strLine + "\n");
                    counter++;
                    if (counter == 100) {
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
            }
        } else {
            chatHistory.appendText("Истрия переписки отсутствует");
        }
    }


    public void setUsernameTitle(String username) {


    }

    // смена ника
    public void openChangeNameField() {
        changeNameFieldID.setVisible(true);
        changeNameLinkID.setVisible(false);
        changeNameFieldID.setText(usernameTitle.getText());

    }


    // Отправка нового никнейма на нетворк
    public void updateUsername() {
        String oldUsername = network.getUsername();
        String newUsername = changeNameFieldID.getText();
        if (newUsername.isBlank()) {
            NetworkClient.showErrorMessage("Ошибка смены имени", "Ошибка ввода ", "Поле не должно " +
                    "быть пустым ");
            return;
        }
        usernameTitle.setText(changeNameFieldID.getText());
        network.sendChangeNameCommand(oldUsername, newUsername);
        changeNameLinkID.setVisible(true);
        changeNameFieldID.setVisible(false);


    }
}
