package com.tlenparty.chat.client.app;

import com.tlenparty.chat.client.data.FileHistoryRepository;
import com.tlenparty.chat.client.data.SocketChatGateway;
import com.tlenparty.chat.client.domain.ChatGateway;
import com.tlenparty.chat.client.presentation.Dialogs;
import com.tlenparty.chat.client.presentation.FxChatListener;
import com.tlenparty.chat.client.presentation.auth.AuthController;
import com.tlenparty.chat.client.presentation.auth.AuthViewModel;
import com.tlenparty.chat.client.presentation.chat.ChatController;
import com.tlenparty.chat.client.presentation.chat.ChatViewModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Timer;
import java.util.TimerTask;

public class ClientApp extends Application {

    private static final long AUTH_TIMEOUT_MILLIS = 120_000;
    private static final Logger logger = LogManager.getLogger(ClientApp.class);

    private final Timer authTimer = new Timer("auth-timeout", true);
    private ClientConfig config;
    private ChatGateway gateway;
    private Stage primaryStage;
    private Stage authStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        this.config = ClientConfig.fromEnvironment();
        this.gateway = new SocketChatGateway(config.host(), config.port());
        try {
            gateway.connect();
        } catch (IOException e) {
            logger.error("Ошибка подключения к серверу {}:{}", config.host(), config.port(), e);
            Dialogs.showError("Проблемы с соединением", "Ошибка подключения к серверу", e.getMessage());
            Platform.exit();
            return;
        }
        openAuthWindow();
    }

    @Override
    public void stop() {
        authTimer.cancel();
        if (gateway != null) {
            gateway.close();
        }
    }

    private void openAuthWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(AuthController.class.getResource("auth-view.fxml"));
        Parent root = loader.load();
        AuthController controller = loader.getController();
        controller.bind(new AuthViewModel(gateway, this::openChatWindow));

        authStage = new Stage();
        authStage.setTitle("Authorisation");
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        authStage.setScene(new Scene(root));
        authStage.setOnCloseRequest(event -> Platform.exit());
        authStage.show();
        scheduleAuthTimeout();
    }

    private void scheduleAuthTimeout() {
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
    }

    private void openChatWindow(String login, String username) {
        authTimer.cancel();
        FXMLLoader loader = new FXMLLoader(ChatController.class.getResource("chat-view.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ChatViewModel viewModel = new ChatViewModel(username,
                gateway, new FileHistoryRepository(config.historyDirectory(), login), Dialogs::showError);
        ChatController controller = loader.getController();
        controller.bind(viewModel);

        primaryStage.titleProperty().bind(viewModel.usernameProperty());
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setOnCloseRequest(event -> Platform.exit());
        authStage.close();
        primaryStage.show();

        viewModel.loadHistory();
        gateway.listen(new FxChatListener(viewModel));
    }
}
