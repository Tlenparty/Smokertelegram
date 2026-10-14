package com.tlenparty.chat.client.presentation;

import javafx.scene.control.Alert;

public final class Dialogs {

    private Dialogs() {
    }

    public static void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
