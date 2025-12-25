package tienlen.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp để lưu trữ thông tin người chơi kết nối đến server
 */
public class PlayerInfo {
    private String username;
    private String ipAddress;
    private String status; // ONLINE, IN_GAME, IDLE
    private LocalDateTime connectTime;
    private String currentSession;

    public PlayerInfo(String username, String ipAddress) {
        this.username = username;
        this.ipAddress = ipAddress;
        this.status = "IDLE";
        this.connectTime = LocalDateTime.now();
        this.currentSession = "-";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    public String getConnectTimeFormatted() {
        return connectTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(String sessionId) {
        this.currentSession = sessionId != null ? sessionId : "-";
    }

    @Override
    public String toString() {
        return username + " (" + status + ") - " + ipAddress;
    }
}
