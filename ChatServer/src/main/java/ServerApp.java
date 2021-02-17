import com.geekbrains.chat.server.MyServer;

import java.io.IOException;

public class ServerApp {

    public static final int DEFAULT_PORT = 8189;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if(args.length != 0){
            port = Integer.parseInt(args[0]); // Вытаскиваем 1 число
        }

        try {
            new MyServer(port).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка !"); // Тут будем менять на логер
            System.exit(1); // Выход
        }

    }

}
