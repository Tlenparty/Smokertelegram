package com.tlenparty.chat.client.data;

import com.tlenparty.chat.client.domain.AuthResult;
import com.tlenparty.chat.client.domain.ChatGateway;
import com.tlenparty.chat.client.domain.ChatListener;
import com.tlenparty.chat.common.protocol.Command;
import com.tlenparty.chat.common.protocol.CommandType;
import com.tlenparty.chat.common.protocol.ProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketChatGateway implements ChatGateway {

    private static final Logger logger = LogManager.getLogger(SocketChatGateway.class);

    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean closed;

    public SocketChatGateway(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect() throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public AuthResult authenticate(String login, String password) {
        try {
            send(Command.auth(login, password));
            Command response = Command.parse(in.readUTF());
            if (response.type() == CommandType.AUTH_OK) {
                return AuthResult.success(response.arg(0));
            }
            if (response.type() == CommandType.AUTH_ERROR) {
                return AuthResult.failure(response.arg(0));
            }
            return AuthResult.failure("Неожиданный ответ сервера: " + response.type().prefix());
        } catch (IllegalArgumentException e) {
            return AuthResult.failure("Логин не должен содержать пробелов");
        } catch (ProtocolException e) {
            logger.warn("Некорректный ответ сервера", e);
            return AuthResult.failure("Некорректный ответ сервера");
        } catch (IOException e) {
            logger.error("Ошибка авторизации", e);
            return AuthResult.failure("Нет связи с сервером: " + e.getMessage());
        }
    }

    @Override
    public void listen(ChatListener listener) {
        Thread reader = new Thread(() -> readLoop(listener), "chat-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop(ChatListener listener) {
        try {
            while (true) {
                String raw = in.readUTF();
                try {
                    dispatch(Command.parse(raw), listener);
                } catch (ProtocolException e) {
                    logger.warn("Некорректная команда от сервера: {}", raw);
                }
            }
        } catch (IOException e) {
            if (!closed) {
                logger.error("Соединение потеряно", e);
                listener.onConnectionLost(e.getMessage());
            }
        }
    }

    private void dispatch(Command command, ChatListener listener) {
        switch (command.type()) {
            case USER_LIST -> listener.onUserList(command.args());
            case CLIENT_MESSAGE -> listener.onPublicMessage(command.arg(0), command.arg(1));
            case PRIVATE_DELIVERY -> listener.onPrivateMessage(command.arg(0), command.arg(2));
            case SERVER_MESSAGE -> listener.onServerMessage(command.arg(0));
            case CHANGE_USERNAME_OK -> listener.onUsernameChanged(command.arg(0));
            case CHANGE_USERNAME_ERROR -> listener.onUsernameChangeRejected(command.arg(0));
            default -> logger.warn("Неожиданная команда от сервера: {}", command.type().prefix());
        }
    }

    @Override
    public void sendPublic(String text) throws IOException {
        send(Command.publicMessage(text));
    }

    @Override
    public void sendPrivate(String recipient, String text) throws IOException {
        send(Command.privateMessage(recipient, text));
    }

    @Override
    public void changeUsername(String newUsername) throws IOException {
        send(Command.changeUsername(newUsername));
    }

    @Override
    public void close() {
        if (closed || socket == null) {
            return;
        }
        closed = true;
        try {
            send(Command.end());
        } catch (IOException e) {
            logger.debug("Не удалось отправить команду завершения: {}", e.getMessage());
        }
        try {
            socket.close();
        } catch (IOException e) {
            logger.warn("Ошибка закрытия сокета", e);
        }
    }

    private synchronized void send(Command command) throws IOException {
        out.writeUTF(command.serialize());
        out.flush();
    }
}
