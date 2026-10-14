# Smokertelegram

Многопользовательский сетевой чат на Java: сервер на сокетах и клиент с GUI на JavaFX.
Пользователи хранятся в SQLite, история переписки — локально у клиента.

## Возможности

- Авторизация по логину и паролю (таблица `auth` в SQLite, пароли — соль + PBKDF2-SHA256)
- Общий чат и личные сообщения: получатель выбирается в выпадающем списке или кликом по участнику
- Список пользователей онлайн, обновляется при входе, выходе и смене ника
- Смена ника с сохранением в базе и проверкой уникальности
- История переписки в файле на каждого пользователя; при входе показываются последние 100 строк
- Закрытие клиента, если вход не выполнен в течение 120 секунд (сервер тоже закрывает такие соединения)
- Логирование через Log4j 2 (консоль + файл)

## Стек

| Область | Технологии |
|---------|------------|
| Язык | Java 17 |
| Сборка | Maven (многомодульный проект, Maven Wrapper) |
| UI | JavaFX 17 (FXML) |
| Сеть | `java.net.Socket`, `DataInputStream` / `DataOutputStream` |
| Хранение | SQLite (sqlite-jdbc) |
| Логирование | Log4j 2 |
| Тесты | JUnit 5 |

## Архитектура

Три Maven-модуля:

```
ChatCommon/   общий протокол команд (используется сервером и клиентом)
ChatServer/   сервер
ChatClient/   JavaFX-клиент
```

Зависимости между слоями направлены внутрь: `app` → `transport`/`data`/`presentation` → `domain`.
Домен не знает ни о сокетах, ни о SQLite, ни о JavaFX. Зависимости собираются вручную в `app`.

### ChatCommon — `com.tlenparty.chat.common.protocol`

- `CommandType` — все команды протокола и их арность
- `Command` — разбор (`parse`) и сборка (`serialize`) строк протокола, фабрики команд
- `Protocol` — порт и хост по умолчанию

### ChatServer — `com.tlenparty.chat.server`

```
domain/
  model/        User, UserAccount, Message, UsernamePolicy
  repository/   UserRepository, RepositoryException
  security/     PasswordHasher
  service/      AuthService, PasswordAuthService      — аутентификация
                ChatRoom, Participant                 — вход/выход, рассылка,
                PrivateMessageResult, RenameResult      личные сообщения, смена ника
data/           SqliteUserRepository (PreparedStatement), Pbkdf2PasswordHasher
transport/      SocketServer (accept-цикл, пул потоков), ClientHandler (протокол ⇄ ChatRoom)
app/            ServerApp (точка входа, сборка зависимостей), ServerConfig, PasswordHashTool
```

### ChatClient — `com.tlenparty.chat.client` (JPMS-модуль `com.tlenparty.chat.client`)

```
domain/         ChatGateway, ChatListener, HistoryRepository, AuthResult
data/           SocketChatGateway (сетевой клиент), FileHistoryRepository
presentation/   auth/  AuthController + AuthViewModel + auth-view.fxml
                chat/  ChatController + ChatViewModel + chat-view.fxml
                FxChatListener (перенос событий в FX-поток), Dialogs, ErrorReporter
app/            ClientApp (JavaFX Application, навигация между окнами), ClientConfig
```

FXML лежат в `ChatClient/src/main/resources/com/tlenparty/chat/client/presentation/{auth,chat}/`.

### Протокол

Строки передаются через `DataOutputStream.writeUTF`. Последний аргумент текстовых команд может содержать пробелы.

| Направление | Команда |
|-------------|---------|
| клиент → сервер | `/auth <login> <password>`, `/msg <text>`, `/w <recipient> <text>`, `/changeUsername <name>`, `/end` |
| сервер → клиент | `/authok <name>`, `/autherr <reason>`, `/clientMsg <sender> <text>`, `/privateMsg <sender> <recipient> <text>`, `/serverMsg <text>`, `/userList <name>...`, `/changeUsernameOk <name>`, `/changeUsernameErr <reason>` |

## Сборка и запуск

Нужен только JDK 17+; Maven скачивается wrapper'ом.

```bash
./mvnw install -DskipTests
```

### Сервер

```bash
java -jar ChatServer/target/chat-server.jar                 # порт 8189, БД ищется автоматически
java -jar ChatServer/target/chat-server.jar 9000 /path/to/mainDB.db
```

Настройки (приоритет: аргумент → системное свойство → переменная окружения → значение по умолчанию):

| Параметр | Аргумент | Свойство | Переменная | По умолчанию |
|----------|----------|----------|------------|--------------|
| Порт | 1-й | `-Dchat.port` | `CHAT_PORT` | `8189` |
| Файл БД | 2-й | `-Dchat.db` | `CHAT_DB` | первый существующий из `mainDB.db`, `src/main/resources/mainDB.db`, `ChatServer/src/main/resources/mainDB.db` |
| Каталог логов | — | `-Dchat.log.dir` | — | `logs` |

Если файл БД не найден, сервер завершится с ошибкой (а не создаст пустую базу).

Хеш пароля для нового пользователя:

```bash
java -cp ChatServer/target/chat-server.jar com.tlenparty.chat.server.app.PasswordHashTool 'secret'
```

### Клиент

```bash
./mvnw -pl ChatClient javafx:run
CHAT_HOST=example.org CHAT_PORT=9000 ./mvnw -pl ChatClient javafx:run
```

| Параметр | Свойство / переменная | По умолчанию |
|----------|-----------------------|--------------|
| Хост | `chat.host` / `CHAT_HOST` | `localhost` |
| Порт | `chat.port` / `CHAT_PORT` | `8189` |
| Каталог истории | `chat.history.dir` / `CHAT_HISTORY_DIR` | `~/.smokertelegram` |

`javafx:run` запускает приложение в отдельной JVM, поэтому из Maven настройки передаются через переменные окружения.

## Тесты

```bash
./mvnw -pl ChatServer -am test     # ChatCommon + ChatServer
./mvnw test                        # все модули
```

- `CommandTest` — разбор и сборка команд протокола
- `SqliteAuthenticationTest` — вход через SQLite-репозиторий на временной БД: успех, неверный пароль,
  неизвестный логин, попытки SQL-инъекции, смена ника
- `Pbkdf2PasswordHasherTest` — хеширование паролей
- `ChatRoomTest` — рассылка, личные сообщения, смена ника, выход (фейковые участники)
- `ServerConfigTest` — разрешение порта и пути к БД
- `SocketServerIntegrationTest` — сервер на свободном порту, два клиента через `Socket`, авторизация и доставка сообщений

## Тестовые аккаунты

| Логин | Пароль | Ник |
|-------|--------|-----|
| `user1` | `1111` | ВероникаКлубника |
| `user2` | `2222` | Димас |
| `user3` | `3333` | Morty |

В `mainDB.db` хранятся только PBKDF2-хеши этих паролей.
