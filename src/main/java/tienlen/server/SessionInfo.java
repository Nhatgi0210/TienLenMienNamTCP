package tienlen.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp để lưu trữ thông tin bàn chơi
 */
public class SessionInfo {
    private String sessionId;
    private int playerCount;
    private String status; // WAITING, RUNNING, FINISHED
    private String displayName;
    private long betAmount;
    private LocalDateTime createdTime;

    public SessionInfo(String sessionId, int playerCount, String status, String displayName, long betAmount) {
        this.sessionId = sessionId;
        this.playerCount = playerCount;
        this.status = status;
        this.displayName = displayName;
        this.betAmount = betAmount;
        this.createdTime = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public String getCreatedTimeFormatted() {
        return createdTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long betAmount) {
        this.betAmount = betAmount;
    }
}
