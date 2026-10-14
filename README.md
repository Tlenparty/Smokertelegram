# Smokertelegram

A multi-user network chat in Java: a socket server and a JavaFX GUI client.
Users are stored in SQLite; chat history is kept locally on the client.

## Features

- Login and password authentication (`auth` table in SQLite, passwords salted and hashed with PBKDF2-SHA256)
- Public chat and private messages: pick the recipient from the drop-down or click a participant
- Online user list, updated on join, leave and nickname change
- Nickname change, persisted in the database and checked for uniqueness
- Per-user chat history file; the last 100 lines are shown after login
- The client closes if login isn't completed within 120 seconds (the server drops such connections too)
- Logging with Log4j 2 (console + file)

## Tech stack

| Area | Technologies |
|------|--------------|
| Language | Java 17 |
| Build | Maven (multi-module, Maven Wrapper) |
| UI | JavaFX 17 (FXML) |
| Networking | `java.net.Socket`, `DataInputStream` / `DataOutputStream` |
| Storage | SQLite (sqlite-jdbc) |
| Logging | Log4j 2 |
| Tests | JUnit 5 |

## Architecture

Three Maven modules:

```
ChatCommon/   shared command protocol (used by server and client)
ChatServer/   server
ChatClient/   JavaFX client
```

Dependencies point inward: `app` → `transport`/`data`/`presentation` → `domain`.
The domain knows nothing about sockets, SQLite or JavaFX. Dependencies are wired by hand in `app`.

### ChatCommon — `com.tlenparty.chat.common.protocol`

- `CommandType` — all protocol commands and their arity
- `Command` — parsing (`parse`) and serialization (`serialize`) of protocol lines, command factories
- `Protocol` — default host and port

### ChatServer — `com.tlenparty.chat.server`

```
domain/
  model/        User, UserAccount, Message, UsernamePolicy
  repository/   UserRepository, RepositoryException
  security/     PasswordHasher
  service/      AuthService, PasswordAuthService      — authentication
                ChatRoom, Participant                 — join/leave, broadcast,
                PrivateMessageResult, RenameResult      private messages, nickname change
data/           SqliteUserRepository (PreparedStatement), Pbkdf2PasswordHasher
transport/      SocketServer (accept loop, thread pool), ClientHandler (protocol ⇄ ChatRoom)
app/            ServerApp (entry point, dependency wiring), ServerConfig, PasswordHashTool
```

### ChatClient — `com.tlenparty.chat.client` (JPMS module `com.tlenparty.chat.client`)

```
domain/         ChatGateway, ChatListener, HistoryRepository, AuthResult
data/           SocketChatGateway (network client), FileHistoryRepository
presentation/   auth/  AuthController + AuthViewModel + auth-view.fxml
                chat/  ChatController + ChatViewModel + chat-view.fxml
                FxChatListener (dispatches events to the FX thread), Dialogs, ErrorReporter
app/            ClientApp (JavaFX Application, window navigation), ClientConfig
```

FXML files live in `ChatClient/src/main/resources/com/tlenparty/chat/client/presentation/{auth,chat}/`.

### Protocol

Lines are sent with `DataOutputStream.writeUTF`. The last argument of text commands may contain spaces.

| Direction | Commands |
|-----------|----------|
| client → server | `/auth <login> <password>`, `/msg <text>`, `/w <recipient> <text>`, `/changeUsername <name>`, `/end` |
| server → client | `/authok <name>`, `/autherr <reason>`, `/clientMsg <sender> <text>`, `/privateMsg <sender> <recipient> <text>`, `/serverMsg <text>`, `/userList <name>...`, `/changeUsernameOk <name>`, `/changeUsernameErr <reason>` |

## Build and run

Only JDK 17+ is required; Maven is downloaded by the wrapper.

```bash
./mvnw install -DskipTests
```

### Server

```bash
java -jar ChatServer/target/chat-server.jar                 # port 8189, database located automatically
java -jar ChatServer/target/chat-server.jar 9000 /path/to/mainDB.db
```

Settings (priority: argument → system property → environment variable → default):

| Setting | Argument | Property | Variable | Default |
|---------|----------|----------|----------|---------|
| Port | 1st | `-Dchat.port` | `CHAT_PORT` | `8189` |
| Database file | 2nd | `-Dchat.db` | `CHAT_DB` | first existing of `mainDB.db`, `src/main/resources/mainDB.db`, `ChatServer/src/main/resources/mainDB.db` |
| Log directory | — | `-Dchat.log.dir` | — | `logs` |

If the database file isn't found, the server exits with an error instead of creating an empty database.

Password hash for a new user:

```bash
java -cp ChatServer/target/chat-server.jar com.tlenparty.chat.server.app.PasswordHashTool 'secret'
```

### Client

```bash
./mvnw -pl ChatClient javafx:run
CHAT_HOST=example.org CHAT_PORT=9000 ./mvnw -pl ChatClient javafx:run
```

| Setting | Property / variable | Default |
|---------|---------------------|---------|
| Host | `chat.host` / `CHAT_HOST` | `localhost` |
| Port | `chat.port` / `CHAT_PORT` | `8189` |
| History directory | `chat.history.dir` / `CHAT_HISTORY_DIR` | `~/.smokertelegram` |

`javafx:run` starts the app in a separate JVM, so settings are passed from Maven through environment variables.

## Tests

```bash
./mvnw -pl ChatServer -am test     # ChatCommon + ChatServer
./mvnw test                        # all modules
```

- `CommandTest` — parsing and serializing protocol commands
- `SqliteAuthenticationTest` — login through the SQLite repository on a temporary database: success, wrong password,
  unknown login, SQL injection attempts, nickname change
- `Pbkdf2PasswordHasherTest` — password hashing
- `ChatRoomTest` — broadcast, private messages, nickname change, leaving (fake participants)
- `ServerConfigTest` — port and database path resolution
- `SocketServerIntegrationTest` — server on a free port, two clients over `Socket`, authentication and message delivery

## Test accounts

| Login | Password | Nickname |
|-------|----------|----------|
| `user1` | `1111` | ВероникаКлубника |
| `user2` | `2222` | Димас |
| `user3` | `3333` | Morty |

`mainDB.db` stores only the PBKDF2 hashes of these passwords.
