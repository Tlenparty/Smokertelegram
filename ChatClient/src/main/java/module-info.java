module com.tlenparty.chat.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;
    requires com.tlenparty.chat.common;

    opens com.tlenparty.chat.client.presentation.auth to javafx.fxml;
    opens com.tlenparty.chat.client.presentation.chat to javafx.fxml;
    exports com.tlenparty.chat.client.app;
}
