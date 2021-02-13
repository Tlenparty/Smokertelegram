package com.geekbrains.chat.server.auth;

import java.sql.*;

public class BaseAuthService implements AuthService {


    private static Connection connection;
    private static Statement stmt;
    private static ResultSet rs;

    public static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:ChatServer/src/main/resources/mainDB.db");
        stmt = connection.createStatement();
    }

    public static void disconnect() throws SQLException {
        connection.close();
    }


    @Override
    public void start() {
        System.out.println("Сервер аунтификации запущен");

    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException {
        connection();
        rs = stmt.executeQuery(String.format("SELECT password,  username " +
                "FROM auth WHERE login = '%s'", login));
        String username = rs.getString("username");
        System.out.println(rs.getString("username"));
        if (rs.getString("password").equals(password)) {
            disconnect();
            return username;
        }
        /*

        for (User client : clients) {
            if(client.getLogin().equals(login) && client.getPassword().equals(password)){
                return client.getUsername();
            }
        }*/
        disconnect();
        return null;
    }

    @Override
    public void close() {
        System.out.println("Сервер аунтификации завершен");
    }

}
