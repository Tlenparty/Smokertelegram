package com.geekbrains.chat.server.auth;

import com.geekbrains.chat.server.User;

import java.util.List;

public class BaseAuthService implements AuthService {

    // Сформируем коллекцию

    public static final List<User> clients =List.of(
            new User("user1","1111","Морти_Смит"),
            new User("user2","2222","Isaac_Duran"),
            new User("user3","3333", "Вероника_Клубника")
    );


    @Override
    public void start() {
        System.out.println("Сервер аунтификации запущен");

    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User client : clients) {
            if(client.getLogin().equals(login) && client.getPassword().equals(password)){
                return client.getUsername();
            }
        }
        return null;
    }

    @Override
    public void close() {
        System.out.println("Сервер аунтификации завершен");
    }

}
