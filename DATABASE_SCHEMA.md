# 🎯 Tien Len Card Game - Database Schema (Simplified)

## 📊 Database Architecture

Database `tienlencards` được thiết kế đơn giản, dễ sử dụng với 3 bảng chính:

---

## 📋 Cấu Trúc Bảng

### 1. **users** - Tài khoản người chơi
Lưu thông tin cơ bản của người dùng

```sql
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  balance BIGINT DEFAULT 1000000,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL,
  INDEX idx_username (username)
);
```

**Columns:**
- `id` - Primary key
- `username` - Tên đăng nhập duy nhất
- `password_hash` - Hash password (SHA256)
- `balance` - Số dư tài khoản (VND, mặc định 1 triệu)
- `created_at` - Thời điểm tạo tài khoản
- `last_login` - Lần đăng nhập cuối

---

### 2. **game_sessions** - Các bàn chơi
Lưu thông tin về mỗi ván chơi

```sql
CREATE TABLE game_sessions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(50) UNIQUE NOT NULL,
  display_name VARCHAR(100),
  bet_amount BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'WAITING',
  winner VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMP NULL,
  ended_at TIMESTAMP NULL,
  INDEX idx_session_id (session_id),
  INDEX idx_status (status),
  INDEX idx_created (created_at)
);
```

**Columns:**
- `id` - Khóa chính
- `session_id` - UUID của ván (dùng trong code)
- `display_name` - Tên bàn hiển thị cho người chơi
- `bet_amount` - Mức cược mỗi ván (VND)
- `status` - WAITING/PLAYING/ENDED
- `winner` - Tên người thắng
- `timestamps` - Thời gian tạo, bắt đầu, kết thúc

---

### 3. **game_records** - Kết quả chi tiết ván chơi
Lưu kết quả cho mỗi người chơi trong mỗi ván

```sql
CREATE TABLE game_records (
  id INT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(50) NOT NULL,
  player_name VARCHAR(50) NOT NULL,
  result VARCHAR(20) NOT NULL,
  bet_amount BIGINT DEFAULT 0,
  amount_won BIGINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id),
  INDEX idx_player (player_name),
  INDEX idx_date (created_at)
);
```

**Columns:**
- `id` - Primary key
- `session_id` - Tham chiếu đến session_id trong game_sessions
- `player_name` - Tên người chơi
- `result` - Kết quả (WIN/LOSE)
- `bet_amount` - Tiền cược
- `amount_won` - Tiền thắng
- `created_at` - Thời điểm ghi lại

---

## 🔄 Data Flow - Luồng Dữ Liệu

### Khi người chơi tạo bàn chơi:
1. **game_sessions** - INSERT (session_id, display_name, bet_amount)

### Khi người chơi join bàn:
1. **game_sessions** - UPDATE current_players

### Khi ván kết thúc:
1. **game_sessions** - UPDATE status → ENDED, total_pot, winner
2. **game_records** - INSERT (chi tiết cho mỗi người chơi)
3. **users** - UPDATE balance (cho người thắng)

---

## 📝 Query Examples

### Lấy thông tin người chơi
```sql
SELECT * FROM users WHERE username = 'player1';
```

### Lấy lịch sử ván chơi của người chơi
```sql
SELECT 
  gs.display_name,
  gr.result,
  gr.bet_amount,
  gr.amount_won,
  gr.created_at
FROM game_records gr
JOIN game_sessions gs ON gr.session_id = gs.session_id
WHERE gr.player_name = 'player1'
ORDER BY gr.created_at DESC
LIMIT 20;
```

### Lấy tất cả bàn chơi đang chờ
```sql
SELECT * FROM game_sessions 
WHERE status = 'WAITING' 
ORDER BY created_at DESC;
```

### Lấy kết quả một ván
```sql
SELECT * FROM game_records 
WHERE session_id = 'abc-123' 
ORDER BY amount_won DESC;
```

### Thống kê người chơi (tổng chiến thắng)
```sql
SELECT 
  player_name,
  COUNT(*) as total_games,
  SUM(CASE WHEN result = 'WIN' THEN 1 ELSE 0 END) as wins,
  SUM(amount_won) as total_won,
  SUM(amount_won) - SUM(bet_amount) as net_profit
FROM game_records
WHERE player_name = 'player1'
GROUP BY player_name;
```

### Lấy thông tin số dư người chơi
```sql
SELECT username, balance FROM users WHERE username = 'player1';
```

---

## 🚀 Tương tác với Code

### UserManager Methods:

**Tạo session:**
```java
createGameSession(String sessionId, String displayName, long betAmount);
```

**Cập nhật số lượng người chơi:**
```java
updateSessionPlayerCount(String sessionId, int playerCount);
```

**Cập nhật trạng thái session:**
```java
updateSessionStatus(String sessionId, String status);
```

**Kết thúc session và lưu người thắng:**
```java
endGameSession(String sessionId, String winner, long totalPot);
```

**Lưu kết quả ván:**
```java
saveGameResult(String gameId, String playerName, String result, 
               long betAmount, long amountWon);
```

**Lấy/cập nhật số dư:**
```java
getBalance(String username);
addBalance(String username, long amount);
subtractBalance(String username, long amount);
```

---

## 💾 Data Integrity

- **Indexes** - Tối ưu hóa truy vấn theo username, session_id, status, dates
- **Unique** - username và session_id là unique
- **Default Values** - balance mặc định 1 triệu VND
- **Timestamps** - Tự động ghi thời gian tạo/cập nhật

---

## 📌 Ưu Điểm của Thiết Kế Đơn Giản

✅ **Dễ hiểu** - Chỉ 3 bảng, mỗi bảng có mục đích rõ ràng
✅ **Dễ query** - Không cần JOIN phức tạp
✅ **Dễ bảo trì** - Ít columns, ít constraints
✅ **Đủ tính năng** - Lưu được tất cả dữ liệu cần thiết
✅ **Tính linh hoạt** - Có thể mở rộng sau nếu cần

---

**Database Version:** MySQL 8.0+
**Charset:** utf8mb4 (hỗ trợ emoji và ký tự đặc biệt)
**Engine:** InnoDB


---

## 📋 Cấu Trúc Bảng

### 1. **users** - Tài khoản người chơi
Lưu thông tin cơ bản và tài khoản của người dùng

```sql
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(100) UNIQUE,
  full_name VARCHAR(100),
  avatar_url VARCHAR(255),
  balance BIGINT DEFAULT 1000000 CHECK (balance >= 0),
  status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL,
  INDEX idx_username (username),
  INDEX idx_status (status),
  INDEX idx_created (created_at)
);
```

**Columns:**
- `id` - Primary key, auto increment
- `username` - Tên đăng nhập duy nhất
- `password_hash` - Hash password SHA256
- `email` - Email đăng ký (optional)
- `full_name` - Tên đầy đủ (optional)
- `avatar_url` - Link ảnh đại diện (optional)
- `balance` - Số dư tài khoản (VND)
- `status` - ACTIVE/INACTIVE/BANNED
- `created_at` - Thời điểm tạo tài khoản
- `updated_at` - Lần cập nhật cuối
- `last_login` - Lần đăng nhập cuối

---

### 2. **game_sessions** - Các bàn chơi
Lưu thông tin về mỗi ván chơi

```sql
CREATE TABLE game_sessions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(50) UNIQUE NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  bet_amount BIGINT NOT NULL CHECK (bet_amount > 0),
  max_players INT DEFAULT 4,
  current_players INT DEFAULT 0,
  status ENUM('WAITING', 'PLAYING', 'ENDED', 'CANCELLED') DEFAULT 'WAITING',
  total_pot BIGINT DEFAULT 0,
  winner_id INT,
  created_by INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMP NULL,
  ended_at TIMESTAMP NULL,
  FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL,
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_session_id (session_id),
  INDEX idx_status (status),
  INDEX idx_created (created_at),
  INDEX idx_winner (winner_id)
);
```

**Columns:**
- `id` - Khóa chính (dùng để reference)
- `session_id` - UUID của ván (unique)
- `display_name` - Tên bàn hiển thị
- `bet_amount` - Mức cược mỗi ván
- `max_players` - Số người tối đa (4)
- `current_players` - Số người hiện tại
- `status` - WAITING/PLAYING/ENDED/CANCELLED
- `total_pot` - Tổng tiền trong ván
- `winner_id` - FK đến người thắng
- `created_by` - FK người tạo bàn
- `timestamps` - Thời gian tạo, bắt đầu, kết thúc

---

### 3. **game_records** - Kết quả chi tiết ván chơi
Lưu kết quả chi tiết cho mỗi người chơi trong ván

```sql
CREATE TABLE game_records (
  id INT PRIMARY KEY AUTO_INCREMENT,
  session_id INT NOT NULL,
  player_id INT NOT NULL,
  result ENUM('WIN', 'LOSE', 'DRAW') NOT NULL,
  bet_amount BIGINT NOT NULL,
  amount_won BIGINT DEFAULT 0,
  amount_lost BIGINT DEFAULT 0,
  final_balance BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (session_id) REFERENCES game_sessions(id) ON DELETE CASCADE,
  FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_session (session_id),
  INDEX idx_player (player_id),
  INDEX idx_result (result),
  INDEX idx_date (created_at)
);
```

**Columns:**
- `id` - Primary key
- `session_id` - FK đến game_sessions
- `player_id` - FK đến users
- `result` - WIN/LOSE/DRAW
- `bet_amount` - Tiền cược
- `amount_won` - Tiền thắng
- `amount_lost` - Tiền thua
- `final_balance` - Số dư sau ván

---

### 4. **transactions** - Lịch sử giao dịch tiền
Tracking tất cả giao dịch tiền: cược, thắng, nạp, rút

```sql
CREATE TABLE transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  type ENUM('BET', 'WIN', 'DEPOSIT', 'WITHDRAW', 'BONUS', 'PENALTY') NOT NULL,
  amount BIGINT NOT NULL,
  balance_before BIGINT NOT NULL,
  balance_after BIGINT NOT NULL,
  description VARCHAR(255),
  reference_id VARCHAR(100),
  status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'COMPLETED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user (user_id),
  INDEX idx_type (type),
  INDEX idx_date (created_at),
  INDEX idx_reference (reference_id)
);
```

**Columns:**
- `type` - Loại giao dịch
- `amount` - Số tiền
- `balance_before/after` - Số dư trước/sau
- `reference_id` - Tham chiếu ván chơi
- `status` - PENDING/COMPLETED/FAILED

---

### 5. **player_statistics** - Thống kê người chơi
Lưu thống kê tổng hợp cho mỗi người chơi

```sql
CREATE TABLE player_statistics (
  id INT PRIMARY KEY AUTO_INCREMENT,
  player_id INT UNIQUE NOT NULL,
  total_games INT DEFAULT 0,
  total_wins INT DEFAULT 0,
  total_losses INT DEFAULT 0,
  total_draws INT DEFAULT 0,
  win_rate FLOAT DEFAULT 0,
  total_bet BIGINT DEFAULT 0,
  total_won BIGINT DEFAULT 0,
  net_profit BIGINT DEFAULT 0,
  highest_pot BIGINT DEFAULT 0,
  longest_streak INT DEFAULT 0,
  last_game_at TIMESTAMP NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_win_rate (win_rate),
  INDEX idx_profit (net_profit)
);
```

**Columns:**
- `total_games` - Tổng ván chơi
- `total_wins/losses/draws` - Chiến thắng/thua/hòa
- `win_rate` - Tỷ lệ thắng %
- `total_bet/won` - Tổng tiền cược/thắng
- `net_profit` - Lợi nhuận ròng
- `highest_pot` - Pot lớn nhất từng thắng
- `longest_streak` - Chuỗi thắng dài nhất

---

### 6. **player_sessions** - Tracking online sessions
Lưu thông tin session khi người chơi online

```sql
CREATE TABLE player_sessions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  player_id INT NOT NULL,
  session_token VARCHAR(100),
  ip_address VARCHAR(45),
  device_info VARCHAR(255),
  status ENUM('ONLINE', 'AWAY', 'OFFLINE') DEFAULT 'ONLINE',
  login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  logout_at TIMESTAMP NULL,
  last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_player (player_id),
  INDEX idx_status (status),
  INDEX idx_login (login_at)
);
```

**Columns:**
- `session_token` - Token để verify session
- `ip_address` - IP của người chơi
- `device_info` - Thông tin thiết bị
- `status` - ONLINE/AWAY/OFFLINE
- `login_at/logout_at` - Thời gian đăng nhập/đăng xuất
- `last_activity` - Hoạt động cuối cùng

---

### 7. **system_log** - Audit logging
Tracking tất cả hành động quan trọng trên hệ thống

```sql
CREATE TABLE system_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  action VARCHAR(100) NOT NULL,
  user_id INT,
  details TEXT,
  ip_address VARCHAR(45),
  status ENUM('SUCCESS', 'FAILED', 'WARNING') DEFAULT 'SUCCESS',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_action (action),
  INDEX idx_user (user_id),
  INDEX idx_date (created_at),
  INDEX idx_status (status)
);
```

**Hành động được track:**
- GAME_SESSION_CREATED - Tạo ván
- PLAYER_LOGIN - Đăng nhập
- PLAYER_LOGOUT - Đăng xuất
- GAME_STARTED - Bắt đầu ván
- GAME_ENDED - Kết thúc ván

---

### 8. **server_statistics** - Thống kê server
Lưu thống kê hàng ngày của server

```sql
CREATE TABLE server_statistics (
  id INT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL UNIQUE,
  total_players INT DEFAULT 0,
  active_players INT DEFAULT 0,
  peak_concurrent_players INT DEFAULT 0,
  total_games INT DEFAULT 0,
  total_pot BIGINT DEFAULT 0,
  average_bet BIGINT DEFAULT 0,
  total_commission BIGINT DEFAULT 0,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX idx_date (stat_date)
);
```

**Columns:**
- `stat_date` - Ngày thống kê
- `total_players` - Tổng người chơi trong ngày
- `active_players` - Người chơi hoạt động
- `peak_concurrent_players` - Peak người online cùng lúc
- `total_games` - Tổng ván chơi
- `total_pot` - Tổng tiền trong các ván
- `average_bet` - Mức cược trung bình
- `total_commission` - Hoa hồng thu được

---

## 🔑 Relationships & Constraints

### Foreign Keys
```
game_sessions.winner_id → users.id
game_sessions.created_by → users.id
game_records.session_id → game_sessions.id
game_records.player_id → users.id
transactions.user_id → users.id
player_statistics.player_id → users.id
player_sessions.player_id → users.id
system_log.user_id → users.id
```

### Check Constraints
```
users.balance >= 0
game_sessions.bet_amount > 0
game_sessions.current_players >= 0 AND <= 4
```

### ON DELETE Behavior
- `CASCADE` - Xóa dữ liệu liên quan (game_records)
- `SET NULL` - Để NULL (game_sessions.winner_id)

---

## 📈 Query Examples

### 1. Lấy thống kê người chơi
```sql
SELECT 
  u.username,
  ps.total_games,
  ps.total_wins,
  ps.win_rate,
  ps.net_profit
FROM users u
JOIN player_statistics ps ON u.id = ps.player_id
ORDER BY ps.net_profit DESC;
```

### 2. Lấy lịch sử ván chơi của người chơi
```sql
SELECT 
  gs.display_name,
  gr.result,
  gr.bet_amount,
  gr.amount_won,
  gr.final_balance,
  gr.created_at
FROM game_records gr
JOIN game_sessions gs ON gr.session_id = gs.id
WHERE gr.player_id = ?
ORDER BY gr.created_at DESC
LIMIT 50;
```

### 3. Lấy lịch giao dịch người chơi
```sql
SELECT 
  type,
  amount,
  balance_before,
  balance_after,
  description,
  created_at
FROM transactions
WHERE user_id = ?
ORDER BY created_at DESC
LIMIT 100;
```

### 4. Thống kê server hôm nay
```sql
SELECT * FROM server_statistics
WHERE stat_date = CURDATE();
```

### 5. Top 10 người chơi
```sql
SELECT 
  u.username,
  ps.total_games,
  ps.total_wins,
  ps.win_rate,
  ps.net_profit
FROM users u
JOIN player_statistics ps ON u.id = ps.player_id
WHERE ps.total_games >= 5
ORDER BY ps.win_rate DESC
LIMIT 10;
```

---

## 🔄 Data Flow

### Khi người chơi tham gia ván:
1. `game_sessions` - INSERT (WAITING)
2. `player_sessions` - INSERT (ONLINE)

### Khi ván bắt đầu:
1. `game_sessions` - UPDATE status → PLAYING
2. `transactions` - INSERT (BET) cho mỗi người

### Khi ván kết thúc:
1. `game_sessions` - UPDATE status → ENDED, winner_id
2. `game_records` - INSERT (chi tiết cho mỗi người)
3. `transactions` - INSERT (WIN) cho người thắng
4. `player_statistics` - UPDATE (update stats)
5. `system_log` - INSERT (log hành động)

### Hàng ngày:
1. `server_statistics` - INSERT/UPDATE (thống kê ngày)

---

## 🛡️ Best Practices Áp Dụng

✅ **Normalization** - Tách dữ liệu theo chức năng rõ ràng
✅ **Indexing** - Index các cột hay truy vấn (username, status, dates)
✅ **Constraints** - CHECK, FOREIGN KEY, UNIQUE để đảm bảo data integrity
✅ **Audit Logging** - Track tất cả hành động quan trọng
✅ **Transactions** - Lưu chi tiết giao dịch tiền
✅ **Statistics** - Cache thống kê để query nhanh
✅ **Timestamps** - created_at, updated_at, last_activity
✅ **Enums** - Status, result, type để hạn chế giá trị không hợp lệ
✅ **Cascading** - Xóa dữ liệu liên quan khi xóa session/player
✅ **Engine InnoDB** - Hỗ trợ transactions và foreign keys

---

## 🚀 Performance Optimizations

1. **Partitioning** (optional):
   - `game_records` - PARTITION BY MONTH(created_at)
   - `transactions` - PARTITION BY MONTH(created_at)

2. **Materialized Views** (optional):
   - Daily aggregates cho server_statistics
   - Player rankings cache

3. **Replication** (cho production):
   - Master-Slave replication
   - Read replicas cho analytics

4. **Backup Strategy**:
   - Nightly full backup
   - Hourly incremental backup
   - 30-day retention

---

**Database Version:** MySQL 8.0+
**Charset:** utf8mb4 (hỗ trợ emoji, ký tự đặc biệt)
**Collation:** utf8mb4_unicode_ci
