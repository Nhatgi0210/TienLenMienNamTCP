package tienlen.server;

import tienlen.logic.GameLogic;
import tienlen.model.Card;
import tienlen.model.Message;
import tienlen.model.Move;
import tienlen.model.Player;
import tienlen.utils.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerMain {
    private static final int PORT = 12345;
    private final List<GameSession> sessions = new ArrayList<>();

    public static void main(String[] args) {
        new ServerMain().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);

                // Tìm phòng chưa đầy
                GameSession session = findAvailableSession();

                // Giao client cho session
                ClientHandler handler = new ClientHandler(socket, session);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Client mất kết nối!");
        }
    }

    private synchronized GameSession findAvailableSession() {
        for (GameSession session : sessions) {
            if (session.getPlayers().size() < 4) {
                return session;
            }
        }
        // nếu không có phòng trống → tạo phòng mới
        GameSession newSession = new GameSession();
        sessions.add(newSession);
        return newSession;
    }
}
