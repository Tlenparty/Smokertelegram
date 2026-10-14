package com.tlenparty.chat.server.data;

import com.tlenparty.chat.server.domain.model.UserAccount;
import com.tlenparty.chat.server.domain.repository.RepositoryException;
import com.tlenparty.chat.server.domain.repository.UserRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {

    private static final String FIND_BY_LOGIN = "SELECT login, password, username FROM auth WHERE login = ?";
    private static final String EXISTS_BY_USERNAME = "SELECT 1 FROM auth WHERE username = ?";
    private static final String UPDATE_USERNAME = "UPDATE auth SET username = ? WHERE login = ?";

    private final String jdbcUrl;

    public SqliteUserRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public Optional<UserAccount> findByLogin(String login) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(FIND_BY_LOGIN)) {
            statement.setString(1, login);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(rs.getString("login"), rs.getString("password"), rs.getString("username")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to load user " + login, e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(EXISTS_BY_USERNAME)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to check username " + username, e);
        }
    }

    @Override
    public boolean updateUsername(String login, String newUsername) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(UPDATE_USERNAME)) {
            statement.setString(1, newUsername);
            statement.setString(2, login);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to rename user " + login, e);
        }
    }
}
