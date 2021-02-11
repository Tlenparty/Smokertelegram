package com.geekbrains.chat.server.auth;

import java.sql.*;

public class BaseAuthService implements AuthService {

/*    public static final List<User> clients =List.of(
            new User("user1","1111","Морти_Смит"),
            new User("user2","2222","Isaac_Duran"),
            new User("user3","3333", "Вероника_Клубника")
    );*/

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet rs;

    private static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:H:\\YandexDisk\\Geekbrains\\ChatServer\\src\\main\\resources\\mainDB.db");
        stmt = connection.createStatement();
    }

    private static void disconnect() throws SQLException {
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
                        "FROM auth WHERE login = '%s'",login));
        String username = rs.getString("username");
        System.out.println(rs.getString("username"));
        if(rs.getString("password").equals(password)){
          //  disconnect();
            return username;
        }
        /*

        for (User client : clients) {
            if(client.getLogin().equals(login) && client.getPassword().equals(password)){
                return client.getUsername();
            }
        }*/
        return null;
    }

    @Override
    public void close() {
        System.out.println("Сервер аунтификации завершен");
    }

}
