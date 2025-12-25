package tienlen.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
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
    
    @Override
    public void start(Stage primaryStage) {
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
        
        centerPane.getChildren().addAll(
            createPlayersPanel(),
            createSessionsPanel()
        );
        HBox.setHgrow(centerPane.getChildren().get(0), javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(centerPane.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        
        root.setCenter(centerPane);
        
        // Bottom - Logs
        root.setBottom(createLogsPanel());
        
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setTitle("Tiến Lên Server Management");
        primaryStage.setScene(scene);
        primaryStage.show();
        
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
        List<PlayerInfo> players = serverBackend.getAllPlayers();
        Platform.runLater(() -> {
            playersTable.getItems().setAll(players);
            totalPlayersLabel.setText("Total Players: " + players.size());
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
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
