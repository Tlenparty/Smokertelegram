import com.geekbrains.chat.server.MyServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ServerApp {

    public static final int DEFAULT_PORT = 8189;
    private static Logger logger = LogManager.getLogger(ServerApp.class);

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if(args.length != 0){
            port = Integer.parseInt(args[0]); // Вытаскиваем 1 число
        }

        try {
            new MyServer(port).start();
        } catch (IOException e) {
            e.printStackTrace();
           logger.error("Ошибка запуска сервера",e);
           System.exit(1);
        }

    }

}
