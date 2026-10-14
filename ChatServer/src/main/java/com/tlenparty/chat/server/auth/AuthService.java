package com.tlenparty.chat.server.auth;

import java.sql.SQLException;

public interface AuthService {

    String getUsernameByLoginAndPassword(String login, String password) throws SQLException;

    boolean isUsernameTaken(String username) throws SQLException;

    boolean changeUsername(String login, String newUsername) throws SQLException;
}
