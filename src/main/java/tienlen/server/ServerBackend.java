package tienlen.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Backend logic cho server - quản lý connections, players, và sessions
 */
public class ServerBackend {
    private static final int PORT = 12345;
    private final List<GameSession> gameSessions = new CopyOnWriteArrayList<>();
    private final Map<String, PlayerInfo> onlinePlayers = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, ClientHandler> clientHandlers = Collections.synchronizedMap(new HashMap<>());
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public void start(ServerUI ui) throws IOException {
        serverSocket = new ServerSocket(PORT);
        isRunning = true;
        ui.appendLog("✅ Server started on port " + PORT);

        // Tạo 2 bàn mẫu khi server khởi động
        GameSession s1 = new GameSession("bàn 1");
        GameSession s2 = new GameSession("bàn 2");
        gameSessions.add(s1);
        gameSessions.add(s2);

        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                String clientIp = socket.getInetAddress().getHostAddress();
                ui.appendLog("🔗 New client connected: " + clientIp);

                ClientHandler handler = new ClientHandler(socket, this);
                new Thread(handler).start();
            } catch (IOException e) {
                if (isRunning) {
                    ui.appendLog("❌ Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    public void registerPlayer(String username, String ipAddress, ClientHandler handler) {
        PlayerInfo player = new PlayerInfo(username, ipAddress);
        onlinePlayers.put(username, player);
        clientHandlers.put(username, handler);
    }

    public void unregisterPlayer(String username) {
        onlinePlayers.remove(username);
        clientHandlers.remove(username);
    }

    public void registerPlayerInSession(String username, String sessionId) {
        PlayerInfo player = onlinePlayers.get(username);
        if (player != null) {
            player.setStatus("IN_GAME");
            player.setCurrentSession(sessionId);
        }
    }

    public void unregisterPlayerFromSession(String username) {
        PlayerInfo player = onlinePlayers.get(username);
        if (player != null) {
            player.setStatus("IDLE");
            player.setCurrentSession(null);
        }
    }

    public List<PlayerInfo> getAllPlayers() {
        return new ArrayList<>(onlinePlayers.values());
    }

    public List<SessionInfo> getAllSessions() {
        List<SessionInfo> sessionInfos = new ArrayList<>();
        for (GameSession session : gameSessions) {
            String status = session.isGameRunning() ? "RUNNING" : "WAITING";
            sessionInfos.add(new SessionInfo(session.getSessionId(), session.getPlayers().size(), status, session.getDisplayName(), session.getBetAmount()));
        }
        return sessionInfos;
    }

    public GameSession createNewSession() {
        GameSession session = new GameSession();
        gameSessions.add(session);
        return session;
    }

    public GameSession createNewSession(String displayName, long betAmount) {
        GameSession session = new GameSession(displayName);
        session.setBetAmount(betAmount);
        gameSessions.add(session);
        return session;
    }

    public GameSession findSessionById(String sessionId) {
        return gameSessions.stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

    public List<GameSession> getAvailableSessions() {
        List<GameSession> available = new ArrayList<>();
        for (GameSession session : gameSessions) {
            if (session.getPlayers().size() < 4 && !session.isGameRunning()) {
                available.add(session);
            }
        }
        return available;
    }

    public void kickPlayer(String username) {
        PlayerInfo player = onlinePlayers.get(username);
        if (player != null) {
            // Gửi message kick đến player
            ClientHandler handler = clientHandlers.get(username);
            if (handler != null) {
                handler.closeConnection();
            }
            unregisterPlayer(username);
        }
    }

    public String getSessionDetails(String sessionId) {
        GameSession session = findSessionById(sessionId);
        if (session == null) {
            return "Session not found!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Session ID: ").append(sessionId).append("\n");
        sb.append("Players (").append(session.getPlayers().size()).append("/4):\n");

        for (tienlen.model.Player p : session.getPlayers()) {
            sb.append("  - ").append(p.getName()).append("\n");
        }

        sb.append("\nStatus: ").append(session.isGameRunning() ? "RUNNING" : "WAITING");

        return sb.toString();
    }

    public void closeSession(String sessionId) {
        GameSession session = findSessionById(sessionId);
        if (session != null) {
            // Notify all players in this session
            for (tienlen.model.Player player : session.getPlayers()) {
                unregisterPlayerFromSession(player.getName());
            }
            gameSessions.remove(session);
        }
    }

    public int getTotalPlayers() {
        return onlinePlayers.size();
    }

    public int getTotalSessions() {
        return gameSessions.size();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
