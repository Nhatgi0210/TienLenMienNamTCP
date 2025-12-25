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

            // Tạo bảng users
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String createTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "username VARCHAR(50) UNIQUE NOT NULL, " +
                        "password_hash VARCHAR(255) NOT NULL, " +
                        "balance BIGINT DEFAULT 1000000, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "last_login TIMESTAMP NULL)";
                
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSql);
                    
                    // Thêm cột balance nếu bảng đã tồn tại nhưng chưa có cột này
                    try {
                        String addColumnSql = "ALTER TABLE users ADD COLUMN balance BIGINT DEFAULT 1000000";
                        stmt.execute(addColumnSql);
                    } catch (SQLException e) {
                        // Cột đã tồn tại, bỏ qua
                    }
                }
            }
            System.out.println("✅ Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
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
    }}