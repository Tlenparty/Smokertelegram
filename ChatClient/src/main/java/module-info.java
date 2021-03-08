module com.geekbrains.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;

    opens com.geekbrains.chat.controllers to javafx.fxml;
    exports com.geekbrains.chat;
}