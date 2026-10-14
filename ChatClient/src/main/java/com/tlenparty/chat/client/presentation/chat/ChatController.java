package com.tlenparty.chat.client.presentation.chat;

import javafx.collections.ListChangeListener;
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

public class ChatController {

    @FXML
    private TextArea chatHistory;

    @FXML
    private ListView<String> usersList;

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

    private ChatViewModel viewModel;

    public void bind(ChatViewModel viewModel) {
        this.viewModel = viewModel;
        usernameTitle.textProperty().bind(viewModel.usernameProperty());
        usersList.setItems(viewModel.onlineUsers());
        userSend.setItems(viewModel.recipients());
        userSend.setValue(ChatViewModel.EVERYONE);
        viewModel.recipients().addListener((ListChangeListener<String>) change -> {
            if (!viewModel.recipients().contains(userSend.getValue())) {
                userSend.setValue(ChatViewModel.EVERYONE);
            }
        });
        viewModel.setTranscript(chatHistory::appendText);
    }

    @FXML
    public void initialize() {
        textField.setOnAction(event -> sendMessage());
        sendButton.setOnAction(event -> sendMessage());
        usersList.setCellFactory(list -> createToggleCell());
    }

    @FXML
    public void openChangeNameField() {
        changeNameFieldID.setText(usernameTitle.getText());
        changeNameFieldID.setVisible(true);
        changeNameLinkID.setVisible(false);
    }

    @FXML
    public void updateUsername() {
        if (viewModel.requestRename(changeNameFieldID.getText())) {
            changeNameLinkID.setVisible(true);
            changeNameFieldID.setVisible(false);
        }
    }

    private void sendMessage() {
        String selected = usersList.getSelectionModel().getSelectedItem();
        String recipient = selected != null ? selected : userSend.getValue();
        if (viewModel.send(textField.getText(), recipient)) {
            textField.clear();
        }
    }

    private ListCell<String> createToggleCell() {
        MultipleSelectionModel<String> selectionModel = usersList.getSelectionModel();
        ListCell<String> cell = new ListCell<>();
        cell.textProperty().bind(cell.itemProperty());
        cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            usersList.requestFocus();
            if (cell.isEmpty()) {
                return;
            }
            int index = cell.getIndex();
            if (selectionModel.getSelectedIndices().contains(index)) {
                selectionModel.clearSelection(index);
            } else {
                selectionModel.select(index);
            }
            event.consume();
        });
        return cell;
    }
}
