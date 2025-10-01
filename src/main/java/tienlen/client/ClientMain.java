package tienlen.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import tienlen.model.Message;
import tienlen.utils.Protocol;

public class ClientMain {
    private static final String HOST = "localhost"; // có thể đổi thành IP server
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT)) {
            System.out.println("✅ Kết nối thành công tới server " + HOST + ":" + PORT);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);

            // Nhập tên người chơi
            System.out.print("Nhập tên của bạn: ");
            String username = scanner.nextLine().trim();
            Message ms = new Message("JOIN", username);
            out.println(Protocol.encode(ms));

            // Thread lắng nghe server
            Thread listener = new Thread(new ServerListener(in));
            listener.start();

            // Luồng nhập lệnh từ bàn phím
            while (true) {
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("quit")) {
                    out.println("QUIT");
                    break;
                }

                // Lệnh hợp lệ: PLAY:..., PASS
                out.println(input);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
