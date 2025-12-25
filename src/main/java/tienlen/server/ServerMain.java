package tienlen.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * ServerMain - Quản lý logic của server (kompatibel với code cũ)
 * Các tính năng quản lý UI nên dùng ServerBackend + ServerUI
 */
public class ServerMain {
    private static final int PORT = 12345;
    private final List<GameSession> sessions = new ArrayList<>();
    private static ServerMain instance; // Singleton pattern

    public static ServerMain getInstance() {
        if (instance == null) {
            instance = new ServerMain();
        }
        return instance;
    }

    public static void main(String[] args) {
        // Khởi động server UI thay vì console
        ServerUI.main(args);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);

                // Không tự động phân chia, chỉ khởi tạo handler
                ClientHandler handler = new ClientHandler(socket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Lấy danh sách tất cả bàn chơi
    public synchronized List<GameSession> getAllSessions() {
        return new ArrayList<>(sessions);
    }

    // Lấy thông tin bàn chơi (tên session, số người, trạng thái)
        public synchronized String getSessionsInfo() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sessions.size(); i++) {
                GameSession session = sessions.get(i);
                int players = session.getPlayers().size();
                String status = session.isGameRunning() ? "RUNNING" : "WAITING";
                if (i > 0) sb.append(";");
                            sb.append(session.getSessionId()).append(",")
                                .append(players).append(",").append(status).append(",").append(session.getDisplayName()).append(",").append(session.getBetAmount());
            }
            return sb.toString();
    }

    // Tạo bàn chơi mới
    public synchronized GameSession createNewSession() {
        GameSession newSession = new GameSession();
        sessions.add(newSession);
        System.out.println("Created new session: " + newSession.getSessionId());
        return newSession;
    }

    // Tạo bàn chơi mới có tên và mức cược
    public synchronized GameSession createNewSession(String displayName, long betAmount) {
        GameSession newSession = new GameSession(displayName);
        newSession.setBetAmount(betAmount);
        sessions.add(newSession);
        System.out.println("Created new session: " + newSession.getSessionId() + " (" + displayName + ", bet=" + betAmount + ")");
        return newSession;
    }

    // Tìm bàn theo ID
    public synchronized GameSession findSessionById(String sessionId) {
        return sessions.stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

    // Kiểm tra bàn có sẵn chỗ không
    public synchronized boolean isSessionAvailable(String sessionId) {
        GameSession session = findSessionById(sessionId);
        return session != null && session.getPlayers().size() < 4 && !session.isGameRunning();
    }
}
