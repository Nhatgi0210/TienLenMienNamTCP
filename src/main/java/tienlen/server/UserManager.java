package tienlen.server;

import tienlen.utils.PasswordUtil;

import java.sql.*;

public class UserManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tienlencards";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";  // Thay đổi password của bạn nếu cần
    private static UserManager instance;

    private UserManager() {
        initDatabase();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // Khởi tạo database nếu chưa có
    private void initDatabase() {
        try {
            // Tạo database nếu chưa tồn tại
            String createDbUrl = "jdbc:mysql://localhost:3306";
            try (Connection conn = DriverManager.getConnection(createDbUrl, DB_USER, DB_PASSWORD)) {
                String createDbSql = "CREATE DATABASE IF NOT EXISTS tienlencards CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createDbSql);
                }
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                Statement stmt = conn.createStatement();
                
                // 1. Bảng USERS - Tài khoản người chơi
                String usersTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "username VARCHAR(50) UNIQUE NOT NULL, " +
                        "password_hash VARCHAR(255) NOT NULL, " +
                        "balance BIGINT DEFAULT 1000000, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "last_login TIMESTAMP NULL, " +
                        "INDEX idx_username (username)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                stmt.execute(usersTableSql);
                System.out.println("✅ users table initialized");

                // 2. Bảng GAME_SESSIONS - Các bàn chơi
                String gameSessionsTableSql = "CREATE TABLE IF NOT EXISTS game_sessions (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "session_id VARCHAR(50) UNIQUE NOT NULL, " +
                        "display_name VARCHAR(100), " +
                        "bet_amount BIGINT NOT NULL, " +
                        "status VARCHAR(20) DEFAULT 'WAITING', " +
                        "winner VARCHAR(50), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "started_at TIMESTAMP NULL, " +
                        "ended_at TIMESTAMP NULL, " +
                        "INDEX idx_session_id (session_id), " +
                        "INDEX idx_status (status), " +
                        "INDEX idx_created (created_at)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                stmt.execute(gameSessionsTableSql);
                System.out.println("✅ game_sessions table initialized");

                // 3. Bảng GAME_RECORDS - Lịch sử chi tiết từng ván chơi
                String gameRecordsTableSql = "CREATE TABLE IF NOT EXISTS game_records (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "session_id VARCHAR(50) NOT NULL, " +
                        "player_name VARCHAR(50) NOT NULL, " +
                        "result VARCHAR(20) NOT NULL, " +
                        "bet_amount BIGINT DEFAULT 0, " +
                        "amount_won BIGINT DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "INDEX idx_session (session_id), " +
                        "INDEX idx_player (player_name), " +
                        "INDEX idx_date (created_at)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                stmt.execute(gameRecordsTableSql);
                System.out.println("✅ game_records table initialized");

                // 4. Bảng PLAYER_SESSIONS - Tracking online sessions
                String playerSessionsTableSql = "CREATE TABLE IF NOT EXISTS player_sessions (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "user_id INT NOT NULL, " +
                        "session_token VARCHAR(100), " +
                        "ip_address VARCHAR(45), " +
                        "device_info VARCHAR(255), " +
                        "status ENUM('ONLINE', 'AWAY', 'OFFLINE') DEFAULT 'ONLINE', " +
                        "login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "logout_at TIMESTAMP NULL, " +
                        "last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                        "INDEX idx_player (user_id), " +
                        "INDEX idx_status (status), " +
                        "INDEX idx_login (login_at)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                stmt.execute(playerSessionsTableSql);
                System.out.println("✅ player_sessions table initialized");

                // 5. Bảng PLAYER_STATISTICS - Thống kê người chơi
                String playerStatsTableSql = "CREATE TABLE IF NOT EXISTS player_statistics (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "player_id INT UNIQUE NOT NULL, " +
                        "total_games INT DEFAULT 0, " +
                        "total_wins INT DEFAULT 0, " +
                        "total_losses INT DEFAULT 0, " +
                        "total_draws INT DEFAULT 0, " +
                        "win_rate FLOAT DEFAULT 0, " +
                        "total_bet BIGINT DEFAULT 0, " +
                        "total_won BIGINT DEFAULT 0, " +
                        "net_profit BIGINT DEFAULT 0, " +
                        "highest_pot BIGINT DEFAULT 0, " +
                        "longest_streak INT DEFAULT 0, " +
                        "last_game_at TIMESTAMP NULL, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE CASCADE, " +
                        "INDEX idx_win_rate (win_rate), " +
                        "INDEX idx_profit (net_profit)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                stmt.execute(playerStatsTableSql);
                System.out.println("✅ player_statistics table initialized");

                System.out.println("✅ All database tables initialized successfully");
                
            }
        } catch (SQLException e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Đăng ký tài khoản mới
    public synchronized boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }

        String hashedPassword = PasswordUtil.hashSHA256(password);
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username.trim());
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            
            System.out.println("✅ User registered: " + username);
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("❌ Username already exists: " + username);
            } else {
                System.err.println("❌ Error registering user: " + e.getMessage());
            }
            return false;
        }
    }

    // Đăng nhập
    public synchronized boolean loginUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }

        String hashedPassword = PasswordUtil.hashSHA256(password);
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (storedHash.equals(hashedPassword)) {
                    // Cập nhật last_login
                    updateLastLogin(username);
                    System.out.println("✅ User logged in: " + username);
                    return true;
                }
            }
            System.out.println("❌ Invalid credentials for user: " + username);
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error logging in user: " + e.getMessage());
            return false;
        }
    }

    // Kiểm tra username đã tồn tại
    public synchronized boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error checking user: " + e.getMessage());
            return false;
        }
    }

    // Cập nhật thời gian login cuối cùng
    private void updateLastLogin(String username) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username.trim());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updating last login: " + e.getMessage());
        }
    }

    // Lấy số lượng người dùng
    public synchronized int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("❌ Error getting user count: " + e.getMessage());
            return 0;
        }
    }

    // Lấy số dư tiền của user
    public synchronized long getBalance(String username) {
        String sql = "SELECT balance FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("balance");
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("❌ Error getting balance: " + e.getMessage());
            return 0;
        }
    }

    // Cập nhật số dư tiền
    public synchronized boolean updateBalance(String username, long newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, newBalance);
            pstmt.setString(2, username.trim());
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Balance updated for " + username + ": " + newBalance);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error updating balance: " + e.getMessage());
            return false;
        }
    }

    // Cộng tiền (khi thắng)
    public synchronized boolean addBalance(String username, long amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, amount);
            pstmt.setString(2, username.trim());
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Added " + amount + " to " + username);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error adding balance: " + e.getMessage());
            return false;
        }
    }

    // Trừ tiền (khi thua)
    public synchronized boolean subtractBalance(String username, long amount) {
        String sql = "UPDATE users SET balance = balance - ? WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, amount);
            pstmt.setString(2, username.trim());
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Subtracted " + amount + " from " + username);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error subtracting balance: " + e.getMessage());
            return false;
        }
    }

    // Lấy tất cả người dùng từ database
    public synchronized java.util.List<String> getAllUsers() {
        java.util.List<String> users = new java.util.ArrayList<>();
        String sql = "SELECT username FROM users ORDER BY username";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting all users: " + e.getMessage());
        }
        return users;
    }

    // Lưu kết quả trò chơi vào database
    public synchronized boolean saveGameResult(String gameId, String playerName, String result, long betAmount, long amountWon) {
        String sql = "INSERT INTO game_records (session_id, player_name, result, bet_amount, amount_won) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, gameId);
            pstmt.setString(2, playerName.trim());
            pstmt.setString(3, result);
            pstmt.setLong(4, betAmount);
            pstmt.setLong(5, amountWon);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Game result saved: " + playerName + " - " + result);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Error saving game result: " + e.getMessage());
            return false;
        }
    }

    // Lấy lịch sử trò chơi của một người chơi
    public synchronized java.util.List<String[]> getPlayerGameHistory(String playerName, int limit) {
        java.util.List<String[]> history = new java.util.ArrayList<>();
        String sql = "SELECT game_id, game_result, bet_amount, amount_won, played_at FROM game_history " +
                     "WHERE player_name = ? ORDER BY played_at DESC LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, playerName.trim());
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String[] record = {
                    rs.getString("game_id"),
                    rs.getString("game_result"),
                    String.valueOf(rs.getLong("bet_amount")),
                    String.valueOf(rs.getLong("amount_won")),
                    rs.getString("played_at")
                };
                history.add(record);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting game history: " + e.getMessage());
        }
        return history;
    }

    // Lấy thống kê trò chơi của người chơi
    public synchronized String[] getPlayerStatistics(String playerName) {
        String sql = "SELECT " +
                     "COUNT(*) as total_games, " +
                     "SUM(CASE WHEN game_result = 'WIN' THEN 1 ELSE 0 END) as wins, " +
                     "SUM(CASE WHEN game_result = 'LOSE' THEN 1 ELSE 0 END) as losses, " +
                     "SUM(amount_won) as total_won, " +
                     "SUM(bet_amount) as total_bet " +
                     "FROM game_history WHERE player_name = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, playerName.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new String[] {
                    String.valueOf(rs.getInt("total_games")),
                    String.valueOf(rs.getInt("wins")),
                    String.valueOf(rs.getInt("losses")),
                    String.valueOf(rs.getLong("total_won")),
                    String.valueOf(rs.getLong("total_bet"))
                };
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting player statistics: " + e.getMessage());
        }
        return new String[] {"0", "0", "0", "0", "0"};
    }

    // Lưu game session vào database
    public synchronized boolean createGameSession(String sessionId, String displayName, long betAmount) {
        String sql = "INSERT INTO game_sessions (session_id, display_name, bet_amount, status) " +
                     "VALUES (?, ?, ?, 'WAITING')";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, sessionId);
            pstmt.setString(2, displayName);
            pstmt.setLong(3, betAmount);
            pstmt.executeUpdate();
            
            System.out.println("✅ Game session created: " + sessionId);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Error creating game session: " + e.getMessage());
            return false;
        }
    }

    // Cập nhật trạng thái session
    public synchronized void updateSessionStatus(String sessionId, String status) {
        String sql = "UPDATE game_sessions SET status = ? WHERE session_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setString(2, sessionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updating session status: " + e.getMessage());
        }
    }

    // Kết thúc game session
    public synchronized void endGameSession(String sessionId, String winner, long totalPot) {
        String sql = "UPDATE game_sessions SET status = 'ENDED', ended_at = NOW(), " +
                     "winner = ? WHERE session_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, winner);
            pstmt.setString(2, sessionId);
            pstmt.executeUpdate();
            
            System.out.println("✅ Game session ended: " + sessionId + ", Winner: " + winner);
        } catch (SQLException e) {
            System.err.println("❌ Error ending game session: " + e.getMessage());
        }
    }

    // Lấy số lượng game sessions hiện tại
    public synchronized int getActiveSessionCount() {
        String sql = "SELECT COUNT(*) as count FROM game_sessions WHERE status IN ('WAITING', 'PLAYING')";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting active session count: " + e.getMessage());
        }
        return 0;
    }

    // Lấy số lượng game sessions kết thúc trong ngày
    public synchronized int getCompletedSessionCountToday() {
        String sql = "SELECT COUNT(*) as count FROM game_sessions " +
                     "WHERE status = 'ENDED' AND DATE(ended_at) = CURDATE()";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting completed session count: " + e.getMessage());
        }
        return 0;
    }

    // Lấy tổng tiền cược hôm nay (từ game_records)
    public synchronized long getTotalBetToday() {
        String sql = "SELECT SUM(bet_amount) as total FROM game_records WHERE DATE(created_at) = CURDATE()";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                Long total = rs.getLong("total");
                return total != null && !rs.wasNull() ? total : 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total bet today: " + e.getMessage());
        }
        return 0;
    }

    // Lấy tổng tiền thắng hôm nay
    public synchronized long getTotalWinToday() {
        String sql = "SELECT SUM(amount_won) as total FROM game_records WHERE DATE(created_at) = CURDATE() AND result = 'WIN'";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                Long total = rs.getLong("total");
                return total != null && !rs.wasNull() ? total : 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total win today: " + e.getMessage());
        }
        return 0;
    }

    // Lấy cược trung bình hôm nay
    public synchronized long getAverageBetToday() {
        String sql = "SELECT AVG(bet_amount) as avg_bet FROM game_records WHERE DATE(created_at) = CURDATE()";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                Double avgBet = rs.getDouble("avg_bet");
                return avgBet != null && !rs.wasNull() ? avgBet.longValue() : 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting average bet today: " + e.getMessage());
        }
        return 0;
    }

    // Lấy top player theo tiền thắng hôm nay
    public synchronized String getTopPlayerToday() {
        String sql = "SELECT player_name, SUM(amount_won) as total_won FROM game_records " +
                     "WHERE DATE(created_at) = CURDATE() AND result = 'WIN' " +
                     "GROUP BY player_name ORDER BY total_won DESC LIMIT 1";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getString("player_name");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top player today: " + e.getMessage());
        }
        return "N/A";
    }

    // Lấy tổng lưu thông (flow) - tổng cược x số bàn
    public synchronized long getTotalFlow() {
        String sql = "SELECT COUNT(*) as game_count FROM game_sessions WHERE status IN ('PLAYING', 'ENDED')";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int gameCount = rs.getInt("game_count");
                long avgBet = getAverageBetToday();
                return gameCount * avgBet * 4; // 4 players per game
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total flow: " + e.getMessage());
        }
        return 0;
    }

    // Log hành động vào system_log
    public synchronized void logSystemAction(String action, Integer userId, String details) {
        String sql = "INSERT INTO system_log (action, user_id, details, status) VALUES (?, ?, ?, 'SUCCESS')";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, action);
            if (userId != null) pstmt.setInt(2, userId);
            else pstmt.setNull(2, java.sql.Types.INTEGER);
            pstmt.setString(3, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error logging action: " + e.getMessage());
        }
    }

    // Lưu giao dịch vào transactions table
    public synchronized void recordTransaction(int userId, String type, long amount, long balanceBefore, long balanceAfter, String description) {
        String sql = "INSERT INTO transactions (user_id, type, amount, balance_before, balance_after, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            pstmt.setLong(3, amount);
            pstmt.setLong(4, balanceBefore);
            pstmt.setLong(5, balanceAfter);
            pstmt.setString(6, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error recording transaction: " + e.getMessage());
        }
    }

    // Cập nhật player statistics
    public synchronized void updatePlayerStats(String playerName, boolean isWin, long bet, long won) {
        String sql = "INSERT INTO player_statistics (player_id, total_games, total_wins, total_losses, total_bet, total_won, net_profit, last_game_at, updated_at) " +
                     "SELECT id, 1, ?, ?, ?, ?, ?, NOW(), NOW() FROM users WHERE username = ? " +
                     "ON DUPLICATE KEY UPDATE " +
                     "total_games = total_games + 1, " +
                     "total_wins = total_wins + ?, " +
                     "total_losses = total_losses + ?, " +
                     "total_bet = total_bet + ?, " +
                     "total_won = total_won + ?, " +
                     "net_profit = net_profit + ?, " +
                     "win_rate = (total_wins + ?) / (total_games + 1) * 100, " +
                     "last_game_at = NOW(), " +
                     "updated_at = NOW()";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int wins = isWin ? 1 : 0;
            int losses = isWin ? 0 : 1;
            long netProfit = won - bet;
            
            pstmt.setInt(1, wins);
            pstmt.setInt(2, losses);
            pstmt.setLong(3, bet);
            pstmt.setLong(4, won);
            pstmt.setLong(5, netProfit);
            pstmt.setString(6, playerName.trim());
            pstmt.setInt(7, wins);
            pstmt.setInt(8, losses);
            pstmt.setLong(9, bet);
            pstmt.setLong(10, won);
            pstmt.setLong(11, netProfit);
            pstmt.setInt(12, wins);
            
            pstmt.executeUpdate();
            System.out.println("✅ Updated player stats for: " + playerName);
        } catch (SQLException e) {
            System.err.println("❌ Error updating player stats: " + e.getMessage());
        }
    }

    // Lưu kết quả ván chơi vào game_records
    public synchronized void saveGameResultNew(String sessionId, String playerName, String result, long betAmount, long amountWon) {
        String sql = "INSERT INTO game_records (session_id, player_name, result, bet_amount, amount_won) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, sessionId);
            pstmt.setString(2, playerName.trim());
            pstmt.setString(3, result);
            pstmt.setLong(4, betAmount);
            pstmt.setLong(5, amountWon);
            pstmt.executeUpdate();
            
            // Update player statistics
            boolean isWin = "WIN".equals(result);
            updatePlayerStats(playerName, isWin, betAmount, amountWon);
            
            System.out.println("✅ Game result saved: " + playerName + " - " + result);
        } catch (SQLException e) {
            System.err.println("❌ Error saving game result: " + e.getMessage());
        }
    }

    // Lưu player session (login/logout tracking)
    public synchronized void createPlayerSession(int userId, String ipAddress, String deviceInfo) {
        String sql = "INSERT INTO player_sessions (player_id, ip_address, device_info, status, login_at, last_activity) " +
                     "VALUES (?, ?, ?, 'ONLINE', NOW(), NOW())";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, ipAddress);
            pstmt.setString(3, deviceInfo);
            pstmt.executeUpdate();
            
            logSystemAction("PLAYER_LOGIN", userId, "IP: " + ipAddress);
        } catch (SQLException e) {
            System.err.println("❌ Error creating player session: " + e.getMessage());
        }
    }

    // Lấy danh sách active sessions từ database (status = WAITING hoặc PLAYING)
    public synchronized java.util.List<java.util.Map<String, Object>> loadActiveSessions() {
        java.util.List<java.util.Map<String, Object>> sessions = new java.util.ArrayList<>();
        String sql = "SELECT session_id, display_name, bet_amount, status FROM game_sessions " +
                     "WHERE status IN ('WAITING', 'PLAYING') ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                java.util.Map<String, Object> session = new java.util.HashMap<>();
                session.put("session_id", rs.getString("session_id"));
                session.put("display_name", rs.getString("display_name"));
                session.put("bet_amount", rs.getLong("bet_amount"));
                session.put("status", rs.getString("status"));
                sessions.add(session);
            }
            
            System.out.println("✅ Loaded " + sessions.size() + " active sessions from database");
        } catch (SQLException e) {
            System.err.println("❌ Error loading active sessions: " + e.getMessage());
        }
        return sessions;
    }

    // Cập nhật server statistics hàng ngày
    public synchronized void updateServerStats() {
        String sql = "INSERT INTO server_statistics (stat_date, total_players, total_games, average_bet) " +
                     "SELECT CURDATE(), COUNT(DISTINCT player_id), COUNT(*), AVG(bet_amount) FROM game_records WHERE DATE(created_at) = CURDATE() " +
                     "ON DUPLICATE KEY UPDATE " +
                     "total_players = VALUES(total_players), " +
                     "total_games = VALUES(total_games), " +
                     "average_bet = VALUES(average_bet), " +
                     "updated_at = NOW()";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("❌ Error updating server stats: " + e.getMessage());
        }
    }

    // =================== PLAYER STATISTICS METHODS ===================

    // Lấy thông tin chi tiết một player
    public synchronized java.util.Map<String, Object> getPlayerInfo(String username) {
        java.util.Map<String, Object> playerInfo = new java.util.HashMap<>();
        
        // Get basic user info
        String userSql = "SELECT id, username, balance, created_at FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(userSql)) {
            
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                playerInfo.put("username", rs.getString("username"));
                playerInfo.put("balance", rs.getLong("balance"));
                playerInfo.put("createdAt", rs.getTimestamp("created_at"));
                
                // Get game statistics
                String statsSql = "SELECT " +
                        "COUNT(*) as totalGames, " +
                        "SUM(CASE WHEN result = 'WIN' THEN 1 ELSE 0 END) as totalWins, " +
                        "SUM(CASE WHEN result = 'LOSE' THEN 1 ELSE 0 END) as totalLosses, " +
                        "ROUND(SUM(CASE WHEN result = 'WIN' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as winRate, " +
                        "SUM(bet_amount) as totalBet, " +
                        "SUM(amount_won) as totalWon, " +
                        "SUM(amount_won) - SUM(bet_amount) as netProfit " +
                        "FROM game_records WHERE player_name = ?";
                
                try (PreparedStatement statsPstmt = conn.prepareStatement(statsSql)) {
                    statsPstmt.setString(1, username.trim());
                    ResultSet statsRs = statsPstmt.executeQuery();
                    
                    if (statsRs.next()) {
                        playerInfo.put("totalGames", statsRs.getInt("totalGames"));
                        playerInfo.put("totalWins", statsRs.getInt("totalWins"));
                        playerInfo.put("totalLosses", statsRs.getInt("totalLosses"));
                        playerInfo.put("winRate", statsRs.getDouble("winRate"));
                        playerInfo.put("totalBet", statsRs.getLong("totalBet"));
                        playerInfo.put("totalWon", statsRs.getLong("totalWon"));
                        playerInfo.put("netProfit", statsRs.getLong("netProfit"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting player info: " + e.getMessage());
        }
        
        return playerInfo;
    }

    // Lấy tất cả player stats
    public synchronized java.util.List<java.util.Map<String, Object>> getAllPlayerStats() {
        java.util.List<java.util.Map<String, Object>> playersList = new java.util.ArrayList<>();
        
        String sql = "SELECT " +
                "u.username, " +
                "u.balance, " +
                "COUNT(gr.id) as totalGames, " +
                "SUM(CASE WHEN gr.result = 'WIN' THEN 1 ELSE 0 END) as totalWins, " +
                "SUM(CASE WHEN gr.result = 'LOSE' THEN 1 ELSE 0 END) as totalLosses, " +
                "ROUND(SUM(CASE WHEN gr.result = 'WIN' THEN 1 ELSE 0 END) * 100.0 / COUNT(gr.id), 2) as winRate, " +
                "SUM(gr.bet_amount) as totalBet, " +
                "SUM(gr.amount_won) as totalWon, " +
                "SUM(gr.amount_won) - SUM(gr.bet_amount) as netProfit " +
                "FROM users u " +
                "LEFT JOIN game_records gr ON u.username = gr.player_name " +
                "GROUP BY u.id, u.username " +
                "ORDER BY u.username";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                java.util.Map<String, Object> player = new java.util.HashMap<>();
                player.put("username", rs.getString("username"));
                player.put("balance", rs.getLong("balance"));
                player.put("totalGames", rs.getInt("totalGames"));
                player.put("totalWins", rs.getInt("totalWins"));
                player.put("totalLosses", rs.getInt("totalLosses"));
                player.put("winRate", rs.getDouble("winRate"));
                player.put("totalBet", rs.getLong("totalBet"));
                player.put("totalWon", rs.getLong("totalWon"));
                player.put("netProfit", rs.getLong("netProfit"));
                playersList.add(player);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting all player stats: " + e.getMessage());
        }
        
        return playersList;
    }

    // Lấy top players theo win rate
    public synchronized java.util.List<java.util.Map<String, Object>> getTopPlayersByWinRate(int limit) {
        java.util.List<java.util.Map<String, Object>> playersList = new java.util.ArrayList<>();
        
        String sql = "SELECT " +
                "u.username, " +
                "COUNT(gr.id) as totalGames, " +
                "COALESCE(SUM(CASE WHEN gr.result = 'WIN' THEN 1 ELSE 0 END), 0) as totalWins, " +
                "COALESCE(ROUND(SUM(CASE WHEN gr.result = 'WIN' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(gr.id), 0), 2), 0) as winRate " +
                "FROM users u " +
                "LEFT JOIN game_records gr ON u.username = gr.player_name " +
                "GROUP BY u.id, u.username " +
                "ORDER BY winRate DESC, totalGames DESC " +
                "LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                java.util.Map<String, Object> player = new java.util.HashMap<>();
                player.put("playerName", rs.getString("username"));
                player.put("totalGames", rs.getInt("totalGames"));
                player.put("totalWins", rs.getInt("totalWins"));
                player.put("winRate", rs.getDouble("winRate"));
                playersList.add(player);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top players by win rate: " + e.getMessage());
        }
        
        return playersList;
    }

    // Lấy top players theo net profit
    public synchronized java.util.List<java.util.Map<String, Object>> getTopPlayersByProfit(int limit) {
        java.util.List<java.util.Map<String, Object>> playersList = new java.util.ArrayList<>();
        
        String sql = "SELECT " +
                "u.username, " +
                "COUNT(gr.id) as totalGames, " +
                "COALESCE(SUM(gr.amount_won) - SUM(gr.bet_amount), 0) as netProfit, " +
                "COALESCE(SUM(gr.amount_won), 0) as totalWon, " +
                "COALESCE(SUM(gr.bet_amount), 0) as totalBet " +
                "FROM users u " +
                "LEFT JOIN game_records gr ON u.username = gr.player_name " +
                "GROUP BY u.id, u.username " +
                "ORDER BY netProfit DESC " +
                "LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                java.util.Map<String, Object> player = new java.util.HashMap<>();
                player.put("playerName", rs.getString("username"));
                player.put("totalGames", rs.getInt("totalGames"));
                player.put("netProfit", rs.getLong("netProfit"));
                player.put("totalWon", rs.getLong("totalWon"));
                player.put("totalBet", rs.getLong("totalBet"));
                playersList.add(player);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top players by profit: " + e.getMessage());
        }
        
        return playersList;
    }

    // Lấy top players theo tổng tiền thắng
    public synchronized java.util.List<java.util.Map<String, Object>> getTopPlayersByTotalWon(int limit) {
        java.util.List<java.util.Map<String, Object>> playersList = new java.util.ArrayList<>();
        
        String sql = "SELECT " +
                "u.username, " +
                "COUNT(gr.id) as totalGames, " +
                "COALESCE(SUM(gr.amount_won), 0) as totalWon " +
                "FROM users u " +
                "LEFT JOIN game_records gr ON u.username = gr.player_name " +
                "GROUP BY u.id, u.username " +
                "ORDER BY totalWon DESC " +
                "LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                java.util.Map<String, Object> player = new java.util.HashMap<>();
                player.put("playerName", rs.getString("username"));
                player.put("totalGames", rs.getInt("totalGames"));
                player.put("totalWon", rs.getLong("totalWon"));
                playersList.add(player);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top players by total won: " + e.getMessage());
        }
        
        return playersList;
    }

    // Lấy top players theo số trận chơi
    public synchronized java.util.List<java.util.Map<String, Object>> getTopPlayersByGamesPlayed(int limit) {
        java.util.List<java.util.Map<String, Object>> playersList = new java.util.ArrayList<>();
        
        String sql = "SELECT " +
                "u.username, " +
                "COUNT(gr.id) as totalGames, " +
                "COALESCE(SUM(CASE WHEN gr.result = 'WIN' THEN 1 ELSE 0 END), 0) as totalWins " +
                "FROM users u " +
                "LEFT JOIN game_records gr ON u.username = gr.player_name " +
                "GROUP BY u.id, u.username " +
                "ORDER BY totalGames DESC " +
                "LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                java.util.Map<String, Object> player = new java.util.HashMap<>();
                player.put("playerName", rs.getString("username"));
                player.put("totalGames", rs.getInt("totalGames"));
                player.put("totalWins", rs.getInt("totalWins"));
                playersList.add(player);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top players by games played: " + e.getMessage());
        }
        
        return playersList;
    }

    // Lấy số player offline
    public synchronized int getOfflinePlayerCount() {
        String sql = "SELECT COUNT(*) as count FROM users WHERE id NOT IN " +
                     "(SELECT DISTINCT user_id FROM player_sessions WHERE status IN ('ONLINE', 'AWAY'))";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting offline player count: " + e.getMessage());
        }
        return 0;
    }

    // Lấy số sessions đang chờ người
    public synchronized int getWaitingSessionCount() {
        String sql = "SELECT COUNT(*) as count FROM game_sessions WHERE status = 'WAITING'";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting waiting session count: " + e.getMessage());
        }
        return 0;
    }

    // Lấy số player hoạt động trong 30 phút gần nhất
    public synchronized int getActivePlayers30Min() {
        String sql = "SELECT COUNT(DISTINCT user_id) as count FROM player_sessions " +
                     "WHERE status IN ('ONLINE', 'AWAY') " +
                     "AND last_activity > DATE_SUB(NOW(), INTERVAL 30 MINUTE)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting active players (30 min): " + e.getMessage());
        }
        return 0;
    }

    // ============ Quản lý Player Sessions ============
    
    // Tạo session khi player login
    public synchronized void createPlayerSession(String username, String ipAddress) {
        String sql = "INSERT INTO player_sessions (player_id, ip_address, status, login_at, last_activity) " +
                     "SELECT id, ?, 'ONLINE', NOW(), NOW() FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ipAddress);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("✅ Created player session for: " + username);
        } catch (SQLException e) {
            System.err.println("❌ Error creating player session: " + e.getMessage());
        }
    }

    // Cập nhật last_activity khi player hoạt động
    public synchronized void updatePlayerActivity(String username) {
        String sql = "UPDATE player_sessions SET last_activity = NOW() " +
                     "WHERE player_id = (SELECT id FROM users WHERE username = ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updating player activity: " + e.getMessage());
        }
    }

    // Cập nhật trạng thái player (ONLINE/AWAY/OFFLINE)
    public synchronized void updatePlayerStatus(String username, String status) {
        String sql = "UPDATE player_sessions SET status = ? " +
                     "WHERE player_id = (SELECT id FROM users WHERE username = ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("✅ Updated player status: " + username + " -> " + status);
        } catch (SQLException e) {
            System.err.println("❌ Error updating player status: " + e.getMessage());
        }
    }

    // Xóa session khi player logout
    public synchronized void deletePlayerSession(String username) {
        String sql = "DELETE FROM player_sessions " +
                     "WHERE player_id = (SELECT id FROM users WHERE username = ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.executeUpdate();
            System.out.println("✅ Deleted player session for: " + username);
        } catch (SQLException e) {
            System.err.println("❌ Error deleting player session: " + e.getMessage());
        }
    }
}
