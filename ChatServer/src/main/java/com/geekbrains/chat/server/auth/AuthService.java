package com.geekbrains.chat.server.auth;

public interface AuthService {

    void start(); // тут будут БД

    String getUsernameByLoginAndPassword(String login, String password); // сответсвует ли логин пароль клиента?
    // правильность ввода возвращает username

    void close();


}
