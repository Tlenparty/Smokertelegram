package com.geekbrains.chat.server;

import java.util.Objects;

public class User {
    // Тут будет БД?


    private final String login;
    private final String password;
    private final String username;


    public User(String login, String password, String username) {
        this.login = login;
        this.password = password;
        this.username = username;
    }


    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return  false;
        User user = (User) o;
        return Objects.equals(login, user.login) && Objects.equals(password, user.password) &&
                Objects.equals(username,username);
    }

}
