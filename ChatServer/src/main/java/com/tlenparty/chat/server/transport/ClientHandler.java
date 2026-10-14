package com.tlenparty.chat.server.transport;

import com.tlenparty.chat.common.protocol.Command;
import com.tlenparty.chat.common.protocol.CommandType;
import com.tlenparty.chat.common.protocol.ProtocolException;
import com.tlenparty.chat.server.domain.model.Message;
import com.tlenparty.chat.server.domain.model.User;
import com.tlenparty.chat.server.domain.model.UsernamePolicy;
import com.tlenparty.chat.server.domain.repository.RepositoryException;
import com.tlenparty.chat.server.domain.service.AuthService;
import com.tlenparty.chat.server.domain.service.ChatRoom;
import com.tlenparty.chat.server.domain.service.Participant;
import com.tlenparty.chat.server.domain.service.PrivateMessageResult;
import com.tlenparty.chat.server.domain.service.RenameResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ClientHandler implements Runnable, Participant {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket socket;
    private final AuthService authService;
    private final ChatRoom chatRoom;
    private final int authTimeoutMillis;
    private final Consumer<ClientHandler> onFinished;
    private DataOutputStream out;
    private volatile String username;

    public ClientHandler(Socket socket, AuthService authService, ChatRoom chatRoom, int authTimeoutMillis,
                         Consumer<ClientHandler> onFinished) {
        this.socket = socket;
        this.authService = authService;
        this.chatRoom = chatRoom;
        this.authTimeoutMillis = authTimeoutMillis;
        this.onFinished = onFinished;
    }

    @Override
    public void run() {
        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            synchronized (this) {
                out = output;
            }
            socket.setSoTimeout(authTimeoutMillis);
            if (authenticate(in)) {
                socket.setSoTimeout(0);
                processCommands(in);
            }
        } catch (SocketTimeoutException e) {
            logger.info("Клиент не авторизовался за отведенное время");
        } catch (EOFException | SocketException e) {
            logger.info("Клиент {} отключился", username);
        } catch (IOException e) {
            logger.error("Ошибка соединения с клиентом {}", username, e);
        } finally {
            chatRoom.leave(this);
            onFinished.accept(this);
        }
    }

    private boolean authenticate(DataInputStream in) throws IOException {
        while (true) {
            Optional<Command> command = read(in);
            if (command.isEmpty()) {
                send(Command.authError("Ошибка авторизации"));
                continue;
            }
            Command received = command.get();
            if (received.type() == CommandType.END) {
                return false;
            }
            if (received.type() != CommandType.AUTH) {
                send(Command.authError("Сначала необходимо авторизоваться"));
            } else if (tryLogin(received.arg(0), received.arg(1))) {
                return true;
            }
        }
    }

    private boolean tryLogin(String login, String password) {
        Optional<User> user;
        try {
            user = authService.authenticate(login, password);
        } catch (RepositoryException e) {
            logger.error("Ошибка доступа к базе пользователей", e);
            send(Command.authError("Сервис авторизации недоступен"));
            return false;
        }
        if (user.isEmpty()) {
            logger.info("Неверный логин или пароль для {}", login);
            send(Command.authError("Логин или пароль не соответствуют действительности"));
            return false;
        }
        if (!chatRoom.join(this, user.get())) {
            send(Command.authError("Учетная запись уже используется"));
            return false;
        }
        logger.info("Пользователь {} подключился к чату", user.get().username());
        return true;
    }

    private void processCommands(DataInputStream in) throws IOException {
        while (true) {
            Optional<Command> parsed = read(in);
            if (parsed.isEmpty()) {
                send(Command.serverMessage("Некорректная команда"));
                continue;
            }
            Command command = parsed.get();
            if (command.type() == CommandType.END) {
                return;
            }
            switch (command.type()) {
                case PUBLIC_MESSAGE -> chatRoom.broadcast(this, command.arg(0));
                case PRIVATE_MESSAGE -> handlePrivateMessage(command.arg(0), command.arg(1));
                case CHANGE_USERNAME -> handleRename(command.arg(0));
                default -> send(Command.serverMessage("Команда не поддерживается: " + command.type().prefix()));
            }
        }
    }

    private void handlePrivateMessage(String recipient, String text) {
        PrivateMessageResult result = chatRoom.sendPrivate(this, recipient, text);
        if (result == PrivateMessageResult.RECIPIENT_OFFLINE) {
            send(Command.serverMessage("Пользователь " + recipient + " не в сети"));
        } else if (result == PrivateMessageResult.SELF) {
            send(Command.serverMessage("Нельзя отправить личное сообщение самому себе"));
        }
    }

    private void handleRename(String newUsername) {
        RenameResult result;
        try {
            result = chatRoom.rename(this, newUsername);
        } catch (RepositoryException e) {
            logger.error("Ошибка смены имени", e);
            send(Command.changeUsernameError("Ошибка базы данных"));
            return;
        }
        String error = switch (result) {
            case RENAMED -> null;
            case UNCHANGED -> "Это уже ваше имя";
            case INVALID -> "Имя должно быть одним словом длиной до " + UsernamePolicy.MAX_LENGTH + " символов";
            case TAKEN -> "Имя " + newUsername + " уже занято";
            case ACCOUNT_NOT_FOUND -> "Учетная запись не найдена";
        };
        if (error != null) {
            send(Command.changeUsernameError(error));
        }
    }

    private Optional<Command> read(DataInputStream in) throws IOException {
        String raw = in.readUTF();
        try {
            return Optional.of(Command.parse(raw));
        } catch (ProtocolException e) {
            logger.debug("Некорректная команда от клиента {}: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void joined(User user) {
        username = user.username();
        send(Command.authOk(user.username()));
    }

    @Override
    public void renamed(String newUsername) {
        logger.info("{} сменил имя на {}", username, newUsername);
        username = newUsername;
        send(Command.changeUsernameOk(newUsername));
    }

    @Override
    public void deliver(Message message) {
        send(message.isPrivate()
                ? Command.privateDelivery(message.sender(), message.recipient(), message.text())
                : Command.clientMessage(message.sender(), message.text()));
    }

    @Override
    public void userJoined(String joinedUsername) {
        send(Command.serverMessage(joinedUsername + " подключился к чату"));
    }

    @Override
    public void userLeft(String leftUsername) {
        send(Command.serverMessage(leftUsername + " покинул чат"));
    }

    @Override
    public void userRenamed(String oldUsername, String newUsername) {
        send(Command.serverMessage(oldUsername + " сменил имя на " + newUsername));
    }

    @Override
    public void usersOnline(List<String> usernames) {
        send(Command.userList(usernames));
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            logger.warn("Ошибка закрытия сокета", e);
        }
    }

    private synchronized void send(Command command) {
        if (out == null) {
            return;
        }
        try {
            out.writeUTF(command.serialize());
            out.flush();
        } catch (UTFDataFormatException e) {
            logger.warn("Сообщение для клиента {} слишком длинное: {}", username, e.getMessage());
        } catch (IOException e) {
            logger.debug("Не удалось отправить сообщение клиенту {}: {}", username, e.getMessage());
            disconnect();
        }
    }
}
