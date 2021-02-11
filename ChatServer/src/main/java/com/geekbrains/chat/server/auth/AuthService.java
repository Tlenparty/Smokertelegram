package com.geekbrains.chat.server.auth;

import java.sql.SQLException;

public interface AuthService {

    void start();

    String getUsernameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException;


    void close();


}
