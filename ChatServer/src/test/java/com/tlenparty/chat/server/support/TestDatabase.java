package com.tlenparty.chat.server.support;

import com.tlenparty.chat.server.data.Pbkdf2PasswordHasher;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestDatabase {

    private TestDatabase() {
    }

    public static Path create(Path directory) throws SQLException {
        Path file = directory.resolve("test.db");
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        try (Connection connection = DriverManager.getConnection(jdbcUrl(file))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE auth (login VARCHAR(32) NOT NULL PRIMARY KEY, "
                        + "password VARCHAR(32) NOT NULL, username VARCHAR(64) NOT NULL)");
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO auth VALUES (?, ?, ?)")) {
                String[][] users = {{"user1", "1111", "Rick"}, {"user2", "2222", "Morty"}, {"user3", "3333", "Summer"}};
                for (String[] user : users) {
                    insert.setString(1, user[0]);
                    insert.setString(2, hasher.hash(user[1]));
                    insert.setString(3, user[2]);
                    insert.executeUpdate();
                }
            }
        }
        return file;
    }

    public static String jdbcUrl(Path file) {
        return "jdbc:sqlite:" + file.toAbsolutePath();
    }
}
