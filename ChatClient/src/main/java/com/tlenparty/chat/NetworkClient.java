package com.tlenparty.chat;

import com.tlenparty.chat.controllers.AuthController;
import com.tlenparty.chat.controllers.ChatController;
import com.tlenparty.chat.models.Network;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkClient extends Application {

    private static final long AUTH_TIMEOUT_MILLIS = 120_000;
    private static final Logger logger = LogManager.getLogger(NetworkClient.class);

    private Stage primaryStage;
    private Stage authStage;
    private Network network;
    private ChatController chatController;
    private final Timer authTimer = new Timer("auth-timeout", true);

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        network = new Network();

        if (!network.connect()) {
            logger.error("Ошибка подключения к серверу");
            showErrorMessage("Проблемы с соединением", "", "Ошибка подключения к серверу");
            Platform.exit();
            return;
        }

        openAuthWindow();
        createMainChatWindow();
    }

    @Override
    public void stop() {
        authTimer.cancel();
        if (network != null) {
            network.close();
        }
    }

    private void openAuthWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(NetworkClient.class.getResource("auth-view.fxml"));
        Parent root = loader.load();
        authStage = new Stage();
        authStage.setTitle("Authorisation");
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        authStage.setScene(new Scene(root));
        authStage.setOnCloseRequest(event -> Platform.exit());
        authStage.show();

        authTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (!primaryStage.isShowing()) {
                        logger.info("Время на авторизацию истекло");
                        Platform.exit();
                    }
                });
            }
        }, AUTH_TIMEOUT_MILLIS);

        AuthController authController = loader.getController();
        authController.setNetwork(network);
        authController.setNetworkClient(this);
    }

    private void createMainChatWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(NetworkClient.class.getResource("chat-view.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("SmokerTelegram");
        primaryStage.setScene(new Scene(root, 600, 400));
        chatController = loader.getController();
        chatController.setNetwork(network);
        primaryStage.setOnCloseRequest(windowEvent -> Platform.exit());
    }

    public static void showErrorMessage(String title, String message, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void openMainChatWindow() {
        authTimer.cancel();
        authStage.close();
        primaryStage.show();
        primaryStage.setTitle(network.getUsername());
        chatController.setLabel(network.getUsername());
        chatController.chatHistoryDisplay();
        network.waitMessage(chatController);
    }
}
