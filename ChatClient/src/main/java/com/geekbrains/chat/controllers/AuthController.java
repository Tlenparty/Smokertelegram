package com.geekbrains.chat.controllers;

import com.geekbrains.chat.NetworkClient;
import com.geekbrains.chat.models.Network;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {
    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passwordField;

    // Авторизируемся ч/з нетворк для клиента и нетворка
    private Network network;
    private NetworkClient networkClient;

    // Метод для проверка аунтификации
    @FXML
    public void checkAuth() {
        String login = loginField.getText();
        String password = passwordField.getText();

        // На нетворк передадим данные. Нетворк на сервер. Сервер проверит. И возвр. юзернейм
        if (login.isBlank() || password.isBlank()) {
            NetworkClient.showErrorMessage("Ошибка авторизации", "Ошибка ввода", "Поля не должны быть пустыми");
            return;
        }

        // У нетворка создадим метод. Отправка на авторизацию
        String authErrorMessage = network.sendAuthCommand(login, password);
        // Если будет ошибка, то алерт
        if (authErrorMessage != null) {
            NetworkClient.showErrorMessage("Ошибка авторизации", "Что-то не то", authErrorMessage);
        } else {
            networkClient.openMainChatWindow(); // Если не пустое то откроется 2 окно и текущая вьюха закроется.
        }
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }
}
