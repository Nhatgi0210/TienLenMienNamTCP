package tienlen.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import tienlen.model.Message;
import tienlen.utils.Protocol;

import java.io.*;
import java.net.Socket;

public class ClientFX extends Application {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private TextField ipField;
    private Button connectBtn;
    private TextField nameField;
    private Button joinBtn;
    private TextArea logArea;
    private String myname;
    @Override
    public void start(Stage stage) {

        // Tiêu đề game
        Label title = new Label("Tiến Lên Miền Nam");
        title.setFont(Font.font("Segoe UI", 30));
        title.setTextFill(Color.web("#FF6F61")); // màu hồng cam tươi

        // Các thành phần nhập IP và Connect
        ipField = new TextField("localhost");
        ipField.setPrefWidth(150);
        connectBtn = new Button("Connect");
        styleButton(connectBtn, "#4CAF50", "#66BB6A"); // màu xanh tươi

        HBox ipBox = new HBox(10, new Label("Server IP:"), ipField, connectBtn);
        ipBox.setAlignment(Pos.CENTER);

        // Các thành phần nhập tên và JOIN
        nameField = new TextField();
        nameField.setPrefWidth(150);
        joinBtn = new Button("JOIN");
        styleButton(joinBtn, "#2196F3", "#42A5F5"); // màu xanh dương tươi
        joinBtn.setDisable(true);

        HBox nameBox = new HBox(10, new Label("Tên bạn:"), nameField, joinBtn);
        nameBox.setAlignment(Pos.CENTER);

        // TextArea log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle("-fx-control-inner-background: #FFFFFF; -fx-font-family: 'Consolas'; -fx-font-size: 12pt; -fx-background-radius: 10;");

        // Layout chính
        VBox root = new VBox(20, title, ipBox, nameBox, logArea);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #E0F7FA;"); // nền pastel xanh nhạt

        connectBtn.setOnAction(e -> connectToServer());
        joinBtn.setOnAction(e -> joinGame());

        Scene scene = new Scene(root, 480, 380);
        stage.setScene(scene);
        stage.setTitle("Tiến Lên Client");
        stage.show();
    }

    // Hàm style nút đẹp hơn với hover
    private void styleButton(Button btn, String color, String hoverColor) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"));
    }

  
    private void connectToServer() {
        String host = ipField.getText().trim();
        int port = 12345;

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            log("✅ Kết nối thành công tới server " + host + ":" + port);

            joinBtn.setDisable(false);
            connectBtn.setDisable(true);
            ipField.setDisable(true);

        } catch (IOException ex) {
            log("❌ Kết nối thất bại: " + ex.getMessage());
        }
    }

  
 // Hàm tạo giao diện bàn chơi
    private void showGameTable() {
        TienLenClientUI gameTable = new TienLenClientUI(out, in, myname);
        Scene gameScene = new Scene(gameTable.createRootPane(), 1200, 800);
        Stage stage = (Stage) ipField.getScene().getWindow();
        gameScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(gameScene);
        ServerListener listener = new ServerListener(
                in,
                gameTable,
                this.myname
        );
        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    private void joinGame() {
        String username = nameField.getText().trim();
        if (username.isEmpty()) {
            log("⚠ Vui lòng nhập tên.");
            return;
        }

        Message ms = new Message("JOIN", username);
        out.println(Protocol.encode(ms));
        log("▶ Đã gửi JOIN với tên: " + username);
        this.myname = username;
        joinBtn.setDisable(true);
        nameField.setDisable(true);

        // Chuyển sang giao diện bàn chơi
        Platform.runLater(() -> showGameTable());
    }
    
//    private void listenServer() {
//        String line;
//        try {
//            while ((line = in.readLine()) != null) {
//                String message = line;
//                Platform.runLater(() -> log("Server: " + message));
//            }
//        } catch (IOException e) {
//            Platform.runLater(() -> log("❌ Lỗi kết nối server: " + e.getMessage()));
//        }
//    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    public static void main(String[] args) {
        launch();
    }
}
