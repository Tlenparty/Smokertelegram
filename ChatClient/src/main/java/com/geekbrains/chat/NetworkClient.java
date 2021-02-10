package com.geekbrains.chat;

import com.geekbrains.chat.controllers.AuthController;
import com.geekbrains.chat.controllers.ChatController;
import com.geekbrains.chat.models.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class NetworkClient extends Application {


    private Stage primaryStage;
    private Stage authStage;
    private Network network;
    private ChatController chatController;


    @Override
    public void start(Stage primaryStage) throws Exception {

        this.primaryStage = primaryStage;

        // Создадим network. Пустой констурктор будет подключаться
        network = new Network();

       if (!network.connect()) {
            System.out.println("Ошибка подключения");
            showErrorMessage("Проблемы с соединением", "", "Ошибка подключения к серверу");
            return;
        }


        // 1 Открываем окно с авторизацией
        openAuthWindow();
        // 2 Создаем окно
        сreateMainChatWindow();

    }

    private void openAuthWindow() throws IOException {
        // Чтобы открыть окно авторизации
        // Будет свой лоудер. Он будет обращаться auth-vewfxml
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(NetworkClient.class.getResource("auth-view.fxml"));
        Parent root = loader.load();
        // создаем сцену
        authStage = new Stage();

        authStage.setTitle("Authorisation");
        authStage.initModality(Modality.WINDOW_MODAL); // задаем модальное окно
        authStage.initOwner(primaryStage);
        Scene scene = new Scene(root); // Сцена для нашего окна
        authStage.setScene(scene);
        authStage.show();

        TimerTask timeout = new TimerTask() {
            @Override
            public void run() {
                if (!primaryStage.isShowing()) {
                    network.close();
                    System.exit(1);
                }
            }
        };
        new Timer().schedule(timeout, 120 * 1000);

        AuthController authController = loader.getController();
        authController.setNetwork(network);
        authController.setNetworkClient(this);


    }

    public void сreateMainChatWindow() throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(NetworkClient.class.getResource("chat-view.fxml")); // достаем вьюху

        Parent root = loader.load();

        primaryStage.setTitle("SmokerTelegram");
        primaryStage.setScene(new Scene(root, 600, 400));
        // primaryStage.show(); По умолчанию не должно появляться

        // Получим контроллер который будет работать с окном
        chatController = loader.getController();
        chatController.setNetwork(network); // для того чтобы передать network контроллеру
        // network.waitMessage(chatController);
        // Обратимся к сцене
        // primaryStage.setOnCloseRequest(windowEvent -> network.close());
        primaryStage.setOnCloseRequest(windowEvent -> {
            network.sendExitMessage();
            network.close();
        });

    }

    // пропишем Alert
    public static void showErrorMessage(String title, String message, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(errorMessage);
        //Покажем алерт
        alert.showAndWait();

    }

    public static void main(String[] args) {
        launch(args);
    }

    public void openMainChatWindow() {
        authStage.close();
        primaryStage.show();

        // При открытии диалового окна
        primaryStage.setTitle(network.getUsername());
        chatController.setLabel(network.getUsername());
        network.waitMessage(chatController);
    }

}