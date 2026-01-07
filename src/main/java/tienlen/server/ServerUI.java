package tienlen.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServerUI extends Application {
    private ServerBackend serverBackend;
    private Stage primaryStage;
    
    // UI Components - Players
    private TableView<PlayerInfo> playersTable;
    private TableColumn<PlayerInfo, String> usernameCol;
    private TableColumn<PlayerInfo, String> ipCol;
    private TableColumn<PlayerInfo, String> statusCol;
    private TableColumn<PlayerInfo, String> sessionCol;
    private TableColumn<PlayerInfo, String> connectTimeCol;
    private Label totalPlayersLabel;
    
    // UI Components - Sessions
    private TableView<SessionInfo> sessionsTable;
    private TableColumn<SessionInfo, String> sessionIdCol;
    private TableColumn<SessionInfo, String> displayNameCol;
    private TableColumn<SessionInfo, Integer> playerCountCol;
    private TableColumn<SessionInfo, String> gameStatusCol;
    private TableColumn<SessionInfo, String> createdTimeCol;
    private TableColumn<SessionInfo, Long> betAmountCol;
    private Label totalSessionsLabel;
    
    // UI Components - Logs
    private TextArea logsArea;
    private Label serverStatusLabel;
    private VBox statisticsPanel;
    private Map<String, Label> statCardLabels = new HashMap<>();
    private TabPane tabPane;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Khởi tạo server backend
        serverBackend = new ServerBackend();
        
        // Tạo layout
        BorderPane root = new BorderPane();
        
        // Header
        root.setTop(createHeader());
        
        // Center - Two panels for Players and Sessions
        HBox centerPane = new HBox(10);
        centerPane.setPadding(new Insets(10));
        centerPane.setStyle("-fx-background-color: #f0f0f0;");
        
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab playersTab = new Tab("👥 Players", createPlayersPanel());
        Tab sessionsTab = new Tab("🎮 Sessions", createSessionsPanel());
        Tab playerStatsTab = new Tab("📊 Player Stats", createPlayerStatsPanel());
        playersTab.setClosable(false);
        sessionsTab.setClosable(false);
        playerStatsTab.setClosable(false);
        
        tabPane.getTabs().addAll(playersTab, sessionsTab, playerStatsTab);
        centerPane.getChildren().add(tabPane);
        HBox.setHgrow(tabPane, javafx.scene.layout.Priority.ALWAYS);
        
        root.setCenter(centerPane);
        
        // Bottom - Logs
        root.setBottom(createLogsPanel());
        
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setTitle("Tiến Lên Server Management");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // After scene is set, add statistics panel
        createStatisticsPanel();
        
        // Khởi động server thread
        startServerThread();
        
        // Khởi động update UI thread
        startUIUpdateThread();
    }
    
    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #2196F3;");
        
        Label titleLabel = new Label("🎮 Tiến Lên Server Management");
        titleLabel.setFont(Font.font("Segoe UI", 28));
        titleLabel.setTextFill(Color.WHITE);
        
        HBox statusBox = new HBox(20);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        
        serverStatusLabel = new Label("● Server: STARTING...");
        serverStatusLabel.setFont(Font.font("Segoe UI", 14));
        serverStatusLabel.setTextFill(Color.WHITE);
        
        Label portLabel = new Label("Port: 12345");
        portLabel.setFont(Font.font("Segoe UI", 12));
        portLabel.setTextFill(Color.LIGHTGRAY);
        
        statusBox.getChildren().addAll(serverStatusLabel, portLabel);
        
        header.getChildren().addAll(titleLabel, statusBox);
        return header;
    }
    
    private VBox createPlayersPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");
        
        // Title
        Label titleLabel = new Label("👥 Danh Sách Người Chơi Online");
        titleLabel.setFont(Font.font("Segoe UI", 16));
        titleLabel.setTextFill(Color.web("#2196F3"));
        
        // Table
        playersTable = new TableView<>();
        playersTable.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");
        
        usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(120);
        
        ipCol = new TableColumn<>("IP Address");
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        ipCol.setPrefWidth(130);
        
        statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        sessionCol = new TableColumn<>("Session");
        sessionCol.setCellValueFactory(new PropertyValueFactory<>("currentSession"));
        sessionCol.setPrefWidth(100);
        
        connectTimeCol = new TableColumn<>("Connect Time");
        connectTimeCol.setCellValueFactory(new PropertyValueFactory<>("connectTimeFormatted"));
        connectTimeCol.setPrefWidth(120);
        
        playersTable.getColumns().addAll(Arrays.asList(usernameCol, ipCol, statusCol, sessionCol, connectTimeCol));
        
        // Add double-click listener to view player details
        playersTable.setRowFactory(tv -> {
            TableRow<PlayerInfo> row = new TableRow<PlayerInfo>() {
                @Override
                protected void updateItem(PlayerInfo item, boolean empty) {
                    super.updateItem(item, empty);
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    PlayerInfo selectedPlayer = row.getItem();
                    showPlayerDetailWindow(selectedPlayer.getUsername());
                }
            });
            return row;
        });
        
        // Info label
        totalPlayersLabel = new Label("Total Players: 0");
        totalPlayersLabel.setFont(Font.font("Segoe UI", 12));
        totalPlayersLabel.setTextFill(Color.web("#4CAF50"));
        
        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button kickBtn = new Button("🚫 Kick Player");
        kickBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
        kickBtn.setOnAction(e -> kickSelectedPlayer());
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> updatePlayersTable());
        
        btnBox.getChildren().addAll(refreshBtn, kickBtn);
        
        panel.getChildren().addAll(titleLabel, playersTable, totalPlayersLabel, btnBox);
        VBox.setVgrow(playersTable, javafx.scene.layout.Priority.ALWAYS);
        
        return panel;
    }
    
    private VBox createSessionsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");
        
        // Title
        Label titleLabel = new Label("🎯 Danh Sách Bàn Chơi");
        titleLabel.setFont(Font.font("Segoe UI", 16));
        titleLabel.setTextFill(Color.web("#FF9800"));
        
        // Table
        sessionsTable = new TableView<>();
        sessionsTable.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");
        
        sessionIdCol = new TableColumn<>("Session ID");
        sessionIdCol.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        sessionIdCol.setPrefWidth(150);

        displayNameCol = new TableColumn<>("Display Name");
        displayNameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        displayNameCol.setPrefWidth(180);
        
        betAmountCol = new TableColumn<>("Bet");
        betAmountCol.setCellValueFactory(new PropertyValueFactory<>("betAmount"));
        betAmountCol.setPrefWidth(120);
        
        playerCountCol = new TableColumn<>("Players");
        playerCountCol.setCellValueFactory(new PropertyValueFactory<>("playerCount"));
        playerCountCol.setPrefWidth(80);
        
        gameStatusCol = new TableColumn<>("Status");
        gameStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        gameStatusCol.setPrefWidth(100);
        
        createdTimeCol = new TableColumn<>("Created");
        createdTimeCol.setCellValueFactory(new PropertyValueFactory<>("createdTimeFormatted"));
        createdTimeCol.setPrefWidth(120);
        
        sessionsTable.getColumns().addAll(Arrays.asList(sessionIdCol, displayNameCol, playerCountCol, gameStatusCol, betAmountCol, createdTimeCol));
        
        // Info label
        totalSessionsLabel = new Label("Total Sessions: 0");
        totalSessionsLabel.setFont(Font.font("Segoe UI", 12));
        totalSessionsLabel.setTextFill(Color.web("#FF9800"));
        
        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button detailsBtn = new Button("📋 View Details");
        detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        detailsBtn.setOnAction(e -> viewSessionDetails());
        
        Button closeBtn = new Button("🔒 Close Session");
        closeBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> closeSelectedSession());
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> updateSessionsTable());
        
        btnBox.getChildren().addAll(refreshBtn, detailsBtn, closeBtn);
        
        panel.getChildren().addAll(titleLabel, sessionsTable, totalSessionsLabel, btnBox);
        VBox.setVgrow(sessionsTable, javafx.scene.layout.Priority.ALWAYS);
        
        return panel;
    }

    private VBox createPlayerStatsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");
        
        // Title
        Label titleLabel = new Label("📊 Thống Kê Người Chơi");
        titleLabel.setFont(Font.font("Segoe UI", 16));
        titleLabel.setTextFill(Color.web("#4CAF50"));
        
        // Buttons for different rankings
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        
        Label filterLabel = new Label("📈 Xem thống kê theo:");
        filterLabel.setFont(Font.font("Segoe UI", 12));
        
        Button allPlayersBtn = new Button("👥 Tất Cả");
        allPlayersBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        allPlayersBtn.setOnAction(e -> updatePlayerStatsTable("all"));
        
        Button winRateBtn = new Button("🏆 Tỉ Lệ Thắng");
        winRateBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        winRateBtn.setOnAction(e -> updatePlayerStatsTable("winRate"));
        
        Button profitBtn = new Button("💰 Lợi Nhuận");
        profitBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        profitBtn.setOnAction(e -> updatePlayerStatsTable("profit"));
        
        Button totalWonBtn = new Button("🎯 Tiền Thắng");
        totalWonBtn.setStyle("-fx-background-color: #E91E63; -fx-text-fill: white; -fx-font-weight: bold;");
        totalWonBtn.setOnAction(e -> updatePlayerStatsTable("totalWon"));
        
        Button gamesBtn = new Button("📋 Số Trận");
        gamesBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");
        gamesBtn.setOnAction(e -> updatePlayerStatsTable("games"));
        
        filterBox.getChildren().addAll(filterLabel, allPlayersBtn, winRateBtn, profitBtn, totalWonBtn, gamesBtn);
        
        // Table
        TableView<javafx.collections.ObservableMap<String, Object>> playerStatsTable = new TableView<>();
        playerStatsTable.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> nameCol = new TableColumn<>("Tên Player");
        nameCol.setPrefWidth(150);
        nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(param.getValue().get("playerName") != null ? param.getValue().get("playerName") : param.getValue().get("username"))));
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> gamesCol = new TableColumn<>("Tổng Trận");
        gamesCol.setPrefWidth(100);
        gamesCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(param.getValue().get("totalGames") != null ? param.getValue().get("totalGames") : "0")));
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> winsCol = new TableColumn<>("Thắng");
        winsCol.setPrefWidth(80);
        winsCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(param.getValue().get("totalWins") != null ? param.getValue().get("totalWins") : "0")));
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> lossesCol = new TableColumn<>("Thua");
        lossesCol.setPrefWidth(80);
        lossesCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(param.getValue().get("totalLosses") != null ? param.getValue().get("totalLosses") : "0")));
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> winRateCol = new TableColumn<>("Tỉ Lệ %");
        winRateCol.setPrefWidth(100);
        winRateCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            String.format("%.2f%%", (Double) param.getValue().get("winRate") != null ? (Double) param.getValue().get("winRate") : 0.0)));
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> balanceCol = new TableColumn<>("Số Dư");
        balanceCol.setPrefWidth(120);
        balanceCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            formatCurrency((Long) param.getValue().get("balance") != null ? (Long) param.getValue().get("balance") : 0L)));
        
        TableColumn<javafx.collections.ObservableMap<String, Object>, String> profitCol = new TableColumn<>("Lợi Nhuận");
        profitCol.setPrefWidth(120);
        profitCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            formatCurrency((Long) param.getValue().get("netProfit") != null ? (Long) param.getValue().get("netProfit") : 0L)));
        
        @SuppressWarnings("unchecked")
        TableColumn<javafx.collections.ObservableMap<String, Object>, ?>[] columns = new TableColumn[] {
            nameCol, gamesCol, winsCol, lossesCol, winRateCol, balanceCol, profitCol
        };
        playerStatsTable.getColumns().addAll(columns);
        
        // Add double-click listener to view player details
        playerStatsTable.setRowFactory(tv -> {
            TableRow<javafx.collections.ObservableMap<String, Object>> row = new TableRow<javafx.collections.ObservableMap<String, Object>>() {
                @Override
                protected void updateItem(javafx.collections.ObservableMap<String, Object> item, boolean empty) {
                    super.updateItem(item, empty);
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    String playerName = String.valueOf(row.getItem().get("playerName") != null ? row.getItem().get("playerName") : row.getItem().get("username"));
                    showPlayerDetailWindow(playerName);
                }
            });
            return row;
        });
        
        panel.getChildren().addAll(titleLabel, filterBox, playerStatsTable);
        VBox.setVgrow(playerStatsTable, javafx.scene.layout.Priority.ALWAYS);
        
        // Store reference for updating
        this.playerStatsTable = playerStatsTable;
        
        // Initial load
        updatePlayerStatsTable("all");
        
        return panel;
    }

    private TableView<javafx.collections.ObservableMap<String, Object>> playerStatsTable;

    private void updatePlayerStatsTable(String sortType) {
        new Thread(() -> {
            UserManager userMgr = UserManager.getInstance();
            java.util.List<java.util.Map<String, Object>> playerStats = null;
            
            switch(sortType) {
                case "winRate":
                    playerStats = userMgr.getTopPlayersByWinRate(100);
                    break;
                case "profit":
                    playerStats = userMgr.getTopPlayersByProfit(100);
                    break;
                case "totalWon":
                    playerStats = userMgr.getTopPlayersByTotalWon(100);
                    break;
                case "games":
                    playerStats = userMgr.getTopPlayersByGamesPlayed(100);
                    break;
                default: // "all"
                    playerStats = userMgr.getAllPlayerStats();
                    break;
            }
            
            final java.util.List<java.util.Map<String, Object>> finalStats = playerStats;
            Platform.runLater(() -> {
                if (playerStatsTable != null) {
                    javafx.collections.ObservableList<javafx.collections.ObservableMap<String, Object>> data = 
                        javafx.collections.FXCollections.observableArrayList();
                    
                    if (finalStats != null) {
                        for (java.util.Map<String, Object> stat : finalStats) {
                            javafx.collections.ObservableMap<String, Object> row = 
                                javafx.collections.FXCollections.observableHashMap();
                            row.putAll(stat);
                            data.add(row);
                        }
                    }
                    
                    playerStatsTable.setItems(data);
                }
            });
        }).start();
    }
    
    private VBox createLogsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefHeight(150);
        panel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");
        
        Label titleLabel = new Label("📝 Server Logs");
        titleLabel.setFont(Font.font("Segoe UI", 12));
        titleLabel.setTextFill(Color.web("#555555"));
        
        logsArea = new TextArea();
        logsArea.setEditable(false);
        logsArea.setWrapText(true);
        logsArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas'; -fx-font-size: 10;");
        
        panel.getChildren().addAll(titleLabel, logsArea);
        VBox.setVgrow(logsArea, javafx.scene.layout.Priority.ALWAYS);
        
        return panel;
    }
    
    private void updatePlayersTable() {
        List<PlayerInfo> players = serverBackend.getAllPlayersWithOffline();
        Platform.runLater(() -> {
            playersTable.getItems().setAll(players);
            long onlineCount = players.stream().filter(p -> !p.getStatus().equals("OFFLINE")).count();
            totalPlayersLabel.setText("Total Players: " + players.size() + " (Online: " + onlineCount + ")");
        });
    }
    
    private void updateSessionsTable() {
        List<SessionInfo> sessions = serverBackend.getAllSessions();
        Platform.runLater(() -> {
            sessionsTable.getItems().setAll(sessions);
            totalSessionsLabel.setText("Total Sessions: " + sessions.size());
        });
    }
    
    private void kickSelectedPlayer() {
        PlayerInfo selected = playersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a player to kick");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm");
        confirmAlert.setHeaderText("Kick Player?");
        confirmAlert.setContentText("Are you sure you want to kick " + selected.getUsername() + "?");
        
        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            serverBackend.kickPlayer(selected.getUsername());
            appendLog("🚫 Kicked player: " + selected.getUsername());
            updatePlayersTable();
        }
    }
    
    private void viewSessionDetails() {
        SessionInfo selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a session");
            return;
        }
        
        String details = serverBackend.getSessionDetails(selected.getSessionId());
        // Append display name if available
        details = "Display Name: " + (selected.getDisplayName() != null ? selected.getDisplayName() : "") + "\n" + "Bet: " + selected.getBetAmount() + " VND\n" + details;
        showAlert(Alert.AlertType.INFORMATION, "Session Details", details);
    }
    
    private void closeSelectedSession() {
        SessionInfo selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a session");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm");
        confirmAlert.setHeaderText("Close Session?");
        confirmAlert.setContentText("Are you sure you want to close session " + selected.getSessionId() + "?");
        
        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            serverBackend.closeSession(selected.getSessionId());
            appendLog("🔒 Closed session: " + selected.getSessionId());
            updateSessionsTable();
        }
    }
    
    public void appendLog(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logsArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void startServerThread() {
        Thread serverThread = new Thread(() -> {
            try {
                serverBackend.start(this);
                Platform.runLater(() -> {
                    serverStatusLabel.setText("● Server: RUNNING");
                    serverStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                });
            } catch (Exception e) {
                appendLog("❌ Server Error: " + e.getMessage());
                Platform.runLater(() -> {
                    serverStatusLabel.setText("● Server: ERROR");
                    serverStatusLabel.setStyle("-fx-text-fill: #F44336;");
                });
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }
    
    private void startUIUpdateThread() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000); // Update every 2 seconds
                    updatePlayersTable();
                    updateSessionsTable();
                    updateStatisticsPanel();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void createStatisticsPanel() {
        // Create Statistics Tab
        VBox statsContent = new VBox(15);
        statsContent.setPadding(new Insets(15));
        statsContent.setStyle("-fx-background-color: #0a0e27;");
        
        // Row 1: Player Statistics
        HBox playerStatsRow = new HBox(15);
        playerStatsRow.setPrefHeight(120);
        playerStatsRow.getChildren().addAll(
            createStatCard("👥 Người Chơi Online", "0", "#FF6B6B"),
            createStatCard("📈 Peak Hôm Nay", "0", "#4ECDC4"),
            createStatCard("⏱️ Hoạt Động (30m)", "0", "#45B7D1"),
            createStatCard("🚪 Offline", "0", "#95A5A6")
        );
        
        // Row 2: Session Statistics
        HBox sessionStatsRow = new HBox(15);
        sessionStatsRow.setPrefHeight(120);
        sessionStatsRow.getChildren().addAll(
            createStatCard("🎮 Bàn Đang Chơi", "0", "#F38181"),
            createStatCard("⏳ Chờ Người", "0", "#AA96DA"),
            createStatCard("✅ Kết Thúc Hôm Nay", "0", "#FCBAD3"),
            createStatCard("📊 Tổng Bàn", "0", "#A8D8EA")
        );
        
        // Row 3: Financial Statistics
        HBox financialStatsRow = new HBox(15);
        financialStatsRow.setPrefHeight(120);
        financialStatsRow.getChildren().addAll(
            createStatCard("💰 Tổng Tiền Cược", "0", "#FFE66D"),
            createStatCard("📊 Cược Trung Bình", "0", "#95E1D3"),
            createStatCard("💸 Lưu Thông", "0", "#C7CEEA"),
            createStatCard("🏆 Top Player Tiền", "N/A", "#FF8C42")
        );
        
        // Row 4: System Statistics
        HBox systemStatsRow = new HBox(15);
        systemStatsRow.setPrefHeight(120);
        systemStatsRow.getChildren().addAll(
            createStatCard("⚡ Uptime (giờ)", "0", "#B4A7D6"),
            createStatCard("🔌 Tổng Kết Nối", "0", "#73A580"),
            createStatCard("⚠️ Lỗi (1h)", "0", "#F0646E"),
            createStatCard("🕐 Response (ms)", "0", "#D6CDA4")
        );
        
        statisticsPanel = new VBox(15);
        statisticsPanel.setPadding(new Insets(15));
        statisticsPanel.setStyle("-fx-background-color: #0a0e27;");
        statisticsPanel.getChildren().addAll(
            createSectionTitle("📈 THỐNG KÊ NGƯỜI CHƠI"),
            playerStatsRow,
            createSectionTitle("🎯 THỐNG KÊ BÀN CHƠI"),
            sessionStatsRow,
            createSectionTitle("💳 THỐNG KÊ TÀI CHÍNH"),
            financialStatsRow,
            createSectionTitle("🔧 THỐNG KÊ HỆ THỐNG"),
            systemStatsRow
        );
        
        ScrollPane scrollPane = new ScrollPane(statisticsPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0a0e27; -fx-control-inner-background: #0a0e27;");
        
        // Use field tabPane which is already initialized
        Tab statsTab = new Tab("📊 Thống Kê", scrollPane);
        statsTab.setClosable(false);
        tabPane.getTabs().add(statsTab);
    }

   

    private VBox createStatCard(String title, String value, String bgColor) {
        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setPrefHeight(110);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-border-radius: 8; " +
                     "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        
        // Store reference using title as key (use the title string directly for consistency)
        statCardLabels.put(title, valueLabel);
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFD700; " +
                      "-fx-padding: 10 0 5 0;");
        return label;
    }

    private void updateStatisticsPanel() {
        if (statisticsPanel == null) return;
        
        Platform.runLater(() -> {
            UserManager userMgr = UserManager.getInstance();
            
            // Player Stats
            int onlinePlayers = playersTable.getItems().size();
            updateStatCardValue("👥 Người Chơi Online", String.valueOf(onlinePlayers));
            
            int peakToday = onlinePlayers; // TODO: Track peak from database
            updateStatCardValue("📈 Peak Hôm Nay", String.valueOf(peakToday));
            
            int activePlayers = userMgr.getActivePlayers30Min();
            updateStatCardValue("⏱️ Hoạt Động (30m)", String.valueOf(activePlayers));
            
            int offlinePlayers = userMgr.getOfflinePlayerCount();
            updateStatCardValue("🚪 Offline", String.valueOf(offlinePlayers));
            
            // Session Stats
            int activeSessions = userMgr.getActiveSessionCount();
            updateStatCardValue("🎮 Bàn Đang Chơi", String.valueOf(activeSessions));
            
            int awaitingSessions = userMgr.getWaitingSessionCount();
            updateStatCardValue("⏳ Chờ Người", String.valueOf(awaitingSessions));
            
            int completedToday = userMgr.getCompletedSessionCountToday();
            updateStatCardValue("✅ Kết Thúc Hôm Nay", String.valueOf(completedToday));
            
            int totalSessions = activeSessions + completedToday;
            updateStatCardValue("📊 Tổng Bàn", String.valueOf(totalSessions));
            
            // Financial Stats - Fixed to use game_records data
            long totalBetToday = userMgr.getTotalBetToday();
            updateStatCardValue("💰 Tổng Tiền Cược", formatCurrency(totalBetToday));
            
            long avgBet = userMgr.getAverageBetToday();
            updateStatCardValue("📊 Cược Trung Bình", formatCurrency(avgBet));
            
            // Total flow = Total bet today
            long totalFlow = userMgr.getTotalFlow();
            updateStatCardValue("💸 Lưu Thông", formatCurrency(totalFlow));
            
            String topPlayer = userMgr.getTopPlayerToday();
            updateStatCardValue("🏆 Top Player Tiền", topPlayer);
            
            // System Stats
            long uptime = System.currentTimeMillis() / 3600000;
            updateStatCardValue("⚡ Uptime (giờ)", String.valueOf(uptime));
            
            updateStatCardValue("🔌 Tổng Kết Nối", String.valueOf(onlinePlayers));
            updateStatCardValue("⚠️ Lỗi (1h)", "0"); // TODO: Track errors
            updateStatCardValue("🕐 Response (ms)", "45"); // TODO: Calculate actual latency
        });
    }

    private void updateStatCardValue(String title, String value) {
        Label label = statCardLabels.get(title);
        if (label != null) {
            label.setText(value);
        }
    }

    private String formatCurrency(long amount) {
        if (amount >= 1_000_000) {
            return String.format("%.1f M", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1f K", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }

    // =================== PLAYER DETAIL WINDOW ===================

    private void showPlayerDetailWindow(String playerName) {
        Stage detailStage = new Stage();
        detailStage.setTitle("🎮 Thông Tin Chi Tiết - " + playerName);
        detailStage.setWidth(600);
        detailStage.setHeight(700);
        detailStage.initStyle(StageStyle.DECORATED);

        new Thread(() -> {
            UserManager userMgr = UserManager.getInstance();
            java.util.Map<String, Object> playerInfo = userMgr.getPlayerInfo(playerName);

            Platform.runLater(() -> {
                BorderPane root = new BorderPane();
                root.setStyle("-fx-background-color: #f5f5f5;");

                // Header
                VBox headerBox = new VBox(10);
                headerBox.setPadding(new Insets(20));
                headerBox.setStyle("-fx-background-color: #2196F3; -fx-border-radius: 10;");
                headerBox.setAlignment(Pos.CENTER_LEFT);

                Label playerNameLabel = new Label(playerName);
                playerNameLabel.setFont(Font.font("Segoe UI", FontPosture.REGULAR, 28));
                playerNameLabel.setTextFill(Color.WHITE);

                Label statusLabel = new Label("🟢 Online");
                statusLabel.setFont(Font.font("Segoe UI", 14));
                statusLabel.setTextFill(Color.WHITE);

                headerBox.getChildren().addAll(playerNameLabel, statusLabel);
                root.setTop(headerBox);

                // Content
                VBox contentBox = new VBox(15);
                contentBox.setPadding(new Insets(20));
                contentBox.setStyle("-fx-background-color: #ffffff;");

                // Info cards
                VBox infoSection = createPlayerInfoSection(playerInfo);
                VBox statsSection = createPlayerStatsSection(playerInfo);
                VBox actionSection = createPlayerActionSection(playerName);

                ScrollPane scrollPane = new ScrollPane();
                VBox scrollContent = new VBox(15);
                scrollContent.setPadding(new Insets(10));
                scrollContent.getChildren().addAll(infoSection, statsSection, actionSection);
                scrollPane.setContent(scrollContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background-color: #ffffff;");

                root.setCenter(scrollPane);

                Scene scene = new Scene(root);
                detailStage.setScene(scene);
                detailStage.show();
            });
        }).start();
    }

    private VBox createPlayerInfoSection(java.util.Map<String, Object> playerInfo) {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 15;");
        section.setStyle("-fx-background-color: #fafafa; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 15;");

        Label titleLabel = new Label("💰 Thông Tin Tài Khoản");
        titleLabel.setFont(Font.font("Segoe UI", 14));
        titleLabel.setTextFill(Color.web("#1976D2"));

        HBox balanceBox = createInfoRow("Số Dư Hiện Tại:", 
            formatCurrency((Long) playerInfo.getOrDefault("balance", 0L)), "#4CAF50");
        
        HBox createdBox = createInfoRow("Ngày Tạo:", 
            String.valueOf(playerInfo.getOrDefault("createdAt", "N/A")), "#FF9800");

        section.getChildren().addAll(titleLabel, balanceBox, createdBox);
        return section;
    }

    private VBox createPlayerStatsSection(java.util.Map<String, Object> playerInfo) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: #fafafa; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 15;");

        Label titleLabel = new Label("📊 Thống Kê Trận Đấu");
        titleLabel.setFont(Font.font("Segoe UI", 14));
        titleLabel.setTextFill(Color.web("#1976D2"));

        HBox gamesBox = createInfoRow("Tổng Trận Chơi:", 
            String.valueOf(playerInfo.getOrDefault("totalGames", 0)), "#2196F3");
        HBox winsBox = createInfoRow("Trận Thắng:", 
            String.valueOf(playerInfo.getOrDefault("totalWins", 0)), "#4CAF50");
        HBox lossesBox = createInfoRow("Trận Thua:", 
            String.valueOf(playerInfo.getOrDefault("totalLosses", 0)), "#F44336");
        
        double winRate = (Double) playerInfo.getOrDefault("winRate", 0.0);
        HBox winRateBox = createInfoRow("Tỉ Lệ Thắng:", 
            String.format("%.2f%%", winRate), "#FF9800");

        section.getChildren().addAll(titleLabel, gamesBox, winsBox, lossesBox, winRateBox);
        return section;
    }

    private VBox createPlayerActionSection(String playerName) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: #fafafa; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 15;");

        Label titleLabel = new Label("💳 Thống Kê Tài Chính");
        titleLabel.setFont(Font.font("Segoe UI", 14));
        titleLabel.setTextFill(Color.web("#1976D2"));

        UserManager userMgr = UserManager.getInstance();
        java.util.Map<String, Object> playerInfo = userMgr.getPlayerInfo(playerName);

        HBox totalBetBox = createInfoRow("Tổng Tiền Cược:", 
            formatCurrency((Long) playerInfo.getOrDefault("totalBet", 0L)), "#9C27B0");
        HBox totalWonBox = createInfoRow("Tổng Tiền Thắng:", 
            formatCurrency((Long) playerInfo.getOrDefault("totalWon", 0L)), "#4CAF50");
        
        long netProfit = (Long) playerInfo.getOrDefault("netProfit", 0L);
        String profitColor = netProfit >= 0 ? "#4CAF50" : "#F44336";
        HBox profitBox = createInfoRow("Lợi Nhuận Ròng:", 
            formatCurrency(netProfit), profitColor);

        section.getChildren().addAll(titleLabel, totalBetBox, totalWonBox, profitBox);
        return section;
    }

    private HBox createInfoRow(String label, String value, String valueColor) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 3; -fx-border-color: #f0f0f0;");

        Label labelText = new Label(label);
        labelText.setFont(Font.font("Segoe UI", 12));
        labelText.setTextFill(Color.web("#555555"));
        labelText.setPrefWidth(150);

        Label valueText = new Label(value);
        valueText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        valueText.setTextFill(Color.web(valueColor));
        HBox.setHgrow(valueText, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(labelText, valueText);
        return row;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
