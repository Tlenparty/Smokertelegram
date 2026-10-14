package com.tlenparty.chat.server.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BaseAuthService implements AuthService {

    private final String jdbcUrl;
    private final PasswordHasher passwordHasher;

    public BaseAuthService(String jdbcUrl, PasswordHasher passwordHasher) {
        this.jdbcUrl = jdbcUrl;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password, username FROM auth WHERE login = ?")) {
            statement.setString(1, login);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return passwordHasher.matches(password, rs.getString("password")) ? rs.getString("username") : null;
            }
        }
    }

    @Override
    public boolean isUsernameTaken(String username) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM auth WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean changeUsername(String login, String newUsername) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE auth SET username = ? WHERE login = ?")) {
            statement.setString(1, newUsername);
            statement.setString(2, login);
            return statement.executeUpdate() == 1;
        }
    }
}
