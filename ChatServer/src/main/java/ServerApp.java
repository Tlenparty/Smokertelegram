import com.tlenparty.chat.server.MyServer;
import com.tlenparty.chat.server.auth.BaseAuthService;
import com.tlenparty.chat.server.auth.PasswordHasher;
import com.tlenparty.chat.server.config.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ServerApp {

    private static final Logger logger = LogManager.getLogger(ServerApp.class);

    public static void main(String[] args) {
        ServerConfig config;
        try {
            config = ServerConfig.resolve(args, System.getProperties(), System.getenv());
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            System.exit(2);
            return;
        }

        logger.info("Using database {}", config.databasePath().toAbsolutePath());
        try (MyServer server = new MyServer(config.port(), new BaseAuthService(config.jdbcUrl(), new PasswordHasher()))) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.start();
        } catch (IOException e) {
            logger.error("Ошибка запуска сервера", e);
            System.exit(1);
        }
    }
}
