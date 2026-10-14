module com.tlenparty.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;

    opens com.tlenparty.chat.controllers to javafx.fxml;
    exports com.tlenparty.chat;
}