package com.tlenparty.chat.client.presentation.auth;

import com.tlenparty.chat.client.domain.AuthResult;
import com.tlenparty.chat.client.presentation.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    private AuthViewModel viewModel;

    public void bind(AuthViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void checkAuth() {
        AuthResult result = viewModel.login(loginField.getText(), passwordField.getText());
        if (!result.success()) {
            Dialogs.showError("Ошибка авторизации", "Не удалось войти", result.error());
        }
    }
}
