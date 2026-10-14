package com.tlenparty.chat.client.presentation.auth;

import com.tlenparty.chat.client.domain.AuthResult;
import com.tlenparty.chat.client.domain.ChatGateway;

public class AuthViewModel {

    public interface SuccessHandler {
        void onAuthenticated(String login, String username);
    }

    private final ChatGateway gateway;
    private final SuccessHandler successHandler;

    public AuthViewModel(ChatGateway gateway, SuccessHandler successHandler) {
        this.gateway = gateway;
        this.successHandler = successHandler;
    }

    public AuthResult login(String login, String password) {
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            return AuthResult.failure("Поля не должны быть пустыми");
        }
        String trimmedLogin = login.trim();
        AuthResult result = gateway.authenticate(trimmedLogin, password);
        if (result.success()) {
            successHandler.onAuthenticated(trimmedLogin, result.username());
        }
        return result;
    }
}
