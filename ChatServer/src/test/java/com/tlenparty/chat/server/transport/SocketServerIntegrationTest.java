package com.tlenparty.chat.server.transport;

import com.tlenparty.chat.common.protocol.Command;
import com.tlenparty.chat.common.protocol.CommandType;
import com.tlenparty.chat.server.app.ServerApp;
import com.tlenparty.chat.server.app.ServerConfig;
import com.tlenparty.chat.server.support.TestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SocketServerIntegrationTest {

    @TempDir
    Path tempDir;

    private SocketServer server;
    private Thread serverThread;

    @BeforeEach
    void startServer() throws Exception {
        server = ServerApp.createServer(new ServerConfig(0, TestDatabase.create(tempDir)));
        serverThread = new Thread(server::serve, "test-server");
        serverThread.start();
    }

    @AfterEach
    void stopServer() throws InterruptedException {
        server.close();
        serverThread.join(5_000);
    }

    @Test
    void deliversMessagesBetweenAuthenticatedClients() throws Exception {
        try (TestClient rick = new TestClient(server.port());
             TestClient morty = new TestClient(server.port())) {
            rick.send(Command.auth("user1", "wrong"));
            assertEquals(CommandType.AUTH_ERROR, rick.receive().type());

            rick.send(Command.auth("user1", "1111"));
            assertEquals(Command.authOk("Rick"), rick.receive());
            morty.send(Command.auth("user2", "2222"));
            assertEquals(Command.authOk("Morty"), morty.receive());

            rick.send(Command.publicMessage("hello, Morty"));
            assertEquals(Command.clientMessage("Rick", "hello, Morty"), morty.receiveFirst(CommandType.CLIENT_MESSAGE));

            morty.send(Command.privateMessage("Rick", "secret"));
            assertEquals(Command.privateDelivery("Morty", "Rick", "secret"), rick.receiveFirst(CommandType.PRIVATE_DELIVERY));

            morty.send(Command.end());
            Command onlineAfterLeave = Command.userList(List.of("Rick"));
            assertEquals(onlineAfterLeave, rick.receiveUntil(onlineAfterLeave));
        }
    }

    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;

        TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(5_000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        void send(Command command) throws IOException {
            out.writeUTF(command.serialize());
            out.flush();
        }

        Command receive() throws Exception {
            return Command.parse(in.readUTF());
        }

        Command receiveFirst(CommandType type) throws Exception {
            Command command = receive();
            while (command.type() != type) {
                command = receive();
            }
            return command;
        }

        Command receiveUntil(Command expected) throws Exception {
            Command command = receive();
            while (!command.equals(expected)) {
                command = receive();
            }
            return command;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
