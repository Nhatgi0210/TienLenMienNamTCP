package tienlen.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tienlen.model.Message;
import tienlen.utils.Protocol;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TableSelectionUI {
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private VBox tableListBox;
    private Label statusLabel;
    private Map<String, TableInfo> tables = new HashMap<>();
    private String selectedSessionId = null;
    private Runnable onTableSelected;
    private Runnable onQuit;

    public static class TableInfo {
        public String sessionId;
        public int playerCount;
        public String status;
        public String displayName;
        public long betAmount;
        public TableInfo(String sessionId, int playerCount, String status, String displayName, long betAmount) {
            this.sessionId = sessionId;
            this.playerCount = playerCount;
            this.status = status;
            this.displayName = displayName;
            this.betAmount = betAmount;
        }
    }

    public TableSelectionUI(PrintWriter out, BufferedReader in, String username) {
        this.out = out;
        this.in = in;
        this.username = username;
    }

    public VBox createRootPane() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #E0F7FA;");

        // Tiêu đề
        Label title = new Label("🎮 Chọn Bàn Chơi");
        title.setFont(Font.font("Segoe UI", 28));
        title.setTextFill(Color.web("#FF6F61"));

        // Trạng thái
        statusLabel = new Label("Đang tải danh sách bàn...");
        statusLabel.setFont(Font.font("Segoe UI", 14));
        statusLabel.setStyle("-fx-text-fill: #0277BD;");

        // Danh sách bàn
        tableListBox = new VBox(10);
        tableListBox.setStyle("-fx-border-color: #0277BD; -fx-border-width: 2; -fx-padding: 10; -fx-border-radius: 8;");
        tableListBox.setPrefHeight(300);

        ScrollPane scrollPane = new ScrollPane(tableListBox);
        scrollPane.setFitToWidth(true);

        // Nút Tạo bàn mới
        Button createTableBtn = new Button("➕ Tạo Bàn Mới");
        styleButton(createTableBtn, "#4CAF50", "#66BB6A");
        createTableBtn.setOnAction(e -> requestNewTable());

        // Nút Quay lại
        Button quitBtn = new Button("❌ Quay Lại");
        styleButton(quitBtn, "#F44336", "#EF5350");
        quitBtn.setOnAction(e -> {
            if (onQuit != null) onQuit.run();
        });

        HBox bottomBox = new HBox(15);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.getChildren().addAll(createTableBtn, quitBtn);

        root.getChildren().addAll(
                title,
                new Separator(),
                statusLabel,
                new Label("Danh sách bàn chơi:"),
                scrollPane,
                bottomBox
        );

        // Yêu cầu danh sách bàn từ server sẽ được gọi từ ClientFX

        return root;
    }

    private void styleButton(Button btn, String color, String hoverColor) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;"));
    }

    private void requestTablesList() {
        new Thread(() -> {
            try {
                Message msg = new Message(Protocol.GET_SESSIONS, "");
                out.println(Protocol.encode(msg));
            } catch (Exception e) {
                Platform.runLater(() -> updateStatus("❌ Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    // Public method để refresh danh sách từ ngoài
    public void refreshTablesList() {
        requestTablesList();
    }

    private void requestNewTable() {
        // Hiển thị dialog để nhập tên bàn và mức cược
        Dialog<javafx.util.Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Tạo Bàn Mới");

        // Set the button types.
        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Create the name and bet fields.
        TextField nameField = new TextField();
        nameField.setPromptText("Tên bàn (ví dụ: bàn 1)");
        TextField betField = new TextField("10000");
        betField.setPromptText("Mức cược (VND)");

        VBox content = new VBox(10, new Label("Tên bàn:"), nameField, new Label("Mức cược (VND):"), betField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return new javafx.util.Pair<>(nameField.getText().trim(), betField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String name = result.getKey();
            String bet = result.getValue();
            if (name == null) name = "";
            if (bet == null || bet.isEmpty()) bet = "10000";
            Message msg = new Message("CREATE_SESSION", "name=" + name + "&bet=" + bet);
            out.println(Protocol.encode(msg));
            requestTablesList();
        });
    }

    // Được gọi từ ServerListener để cập nhật danh sách bàn
    public void updateSessionList(String sessionListData) {
        Platform.runLater(() -> {
            tableListBox.getChildren().clear();
            tables.clear();

            if (sessionListData == null || sessionListData.isEmpty()) {
                Label emptyLabel = new Label("📭 Không có bàn chơi nào. Tạo bàn mới?");
                emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");
                tableListBox.getChildren().add(emptyLabel);
                updateStatus("✅ Đã cập nhật danh sách (trống)");
                return;
            }

            // Parse dữ liệu: sessionId,playerCount,status;...
            String[] sessionArray = sessionListData.split(";");
            for (String sessionData : sessionArray) {
            	System.out.println("hehe: "+sessionData);
                if (sessionData.isEmpty()) continue;
                
                String[] parts = sessionData.split(",");
                    if (parts.length >= 5) {
                    String sessionId = parts[0];
                    int playerCount = Integer.parseInt(parts[1]);
                    String status = parts[2];
                    String displayName = parts[3];
                    long bet = 10000;
                    try { bet = Long.parseLong(parts[4]); } catch (NumberFormatException ex) {}

                    TableInfo info = new TableInfo(sessionId, playerCount, status, displayName, bet);
                    tables.put(sessionId, info);

                    HBox tableRow = createTableRow(info);
                    tableListBox.getChildren().add(tableRow);
                }
            }

            updateStatus("✅ Đã cập nhật danh sách (" + tables.size() + " bàn)");
        });
    }

    private HBox createTableRow(TableInfo info) {
        HBox row = new HBox(15);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #90CAF9; -fx-border-width: 1; -fx-background-color: #F1F8E9; -fx-border-radius: 5;");
        row.setAlignment(Pos.CENTER_LEFT);

        // Thông tin bàn
        VBox infoBox = new VBox(5);
        Label tableIdLabel = new Label(info.displayName != null && !info.displayName.isEmpty() ? info.displayName : ("Bàn #" + info.sessionId.substring(0, 8) + "..."));
        tableIdLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));

        String statusText = info.status.equals("RUNNING") ? "⏱️ Đang chơi" : "⏳ Đợi người";
        String statusColor = info.status.equals("RUNNING") ? "#F57C00" : "#388E3C";

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");

        Label playersLabel = new Label("👥 Người chơi: " + info.playerCount + "/4");
        Label betLabel = new Label("💰 Mức cược: " + info.betAmount + " VND");

        infoBox.getChildren().addAll(tableIdLabel, statusLabel, playersLabel, betLabel);

        // Nút chọn
        Button selectBtn = new Button("Chọn Bàn");
        selectBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        selectBtn.setPrefWidth(100);

        boolean canSelect = info.playerCount < 4 && !info.status.equals("RUNNING");
        selectBtn.setDisable(!canSelect);

        if (!canSelect) {
            selectBtn.setStyle("-fx-background-color: #BDBDBD; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        }

        selectBtn.setOnAction(e -> {
            selectedSessionId = info.sessionId;
            selectTable(info.sessionId);
        });

        row.getChildren().addAll(infoBox, new Separator(), selectBtn);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        return row;
    }

    private void selectTable(String sessionId) {
        Message msg = new Message(Protocol.SELECT_SESSION, sessionId);
        out.println(Protocol.encode(msg));

        if (onTableSelected != null) {
            // Gọi callback sau khi chọn bàn
            Platform.runLater(onTableSelected);
        }
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    public void setOnTableSelected(Runnable callback) {
        this.onTableSelected = callback;
    }

    public void setOnQuit(Runnable callback) {
        this.onQuit = callback;
    }

    public String getSelectedSessionId() {
        return selectedSessionId;
    }

    public TableInfo getSelectedTableInfo() {
        if (selectedSessionId == null) return null;
        return tables.get(selectedSessionId);
    }
}
