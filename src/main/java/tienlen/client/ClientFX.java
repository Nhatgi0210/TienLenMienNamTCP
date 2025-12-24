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
import tienlen.utils.PasswordUtil;
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
    private Button loginBtn; 
    private TextArea logArea;
    private String myname;
    private PasswordField passwordField;
    private Button registerBtn;

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

     // Nhập tên đăng nhập
        nameField = new TextField();
        nameField.setPrefWidth(150);

        // Nhập mật khẩu
        passwordField = new PasswordField();
        passwordField.setPrefWidth(150);

        // Nút JOIN (đăng nhập)
        loginBtn = new Button("ĐĂNG NHẬP");
        styleButton(loginBtn, "#2196F3", "#42A5F5");
        loginBtn.setDisable(true);

        // Nút ĐĂNG KÝ
        registerBtn = new Button("ĐĂNG KÝ");
        styleButton(registerBtn, "#FF9800", "#FFB74D");
        registerBtn.setDisable(true);

        GridPane loginGrid = new GridPane();
        loginGrid.setHgap(10);
        loginGrid.setVgap(10);
        loginGrid.setAlignment(Pos.CENTER);

        loginGrid.add(new Label("Tên đăng nhập:"), 0, 0);
        loginGrid.add(nameField, 1, 0);
        loginGrid.add(new Label("Mật khẩu:"), 0, 1);
        loginGrid.add(passwordField, 1, 1);

        HBox btnBox = new HBox(15, loginBtn, registerBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox loginBox = new VBox(15, loginGrid, btnBox);
        loginBox.setAlignment(Pos.CENTER);


        // TextArea log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle("-fx-control-inner-background: #FFFFFF; -fx-font-family: 'Consolas'; -fx-font-size: 12pt; -fx-background-radius: 10;");

        // Layout chính
        VBox root = new VBox(20, title, ipBox, loginBox, logArea);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #E0F7FA;"); // nền pastel xanh nhạt

        connectBtn.setOnAction(e -> connectToServer());
        loginBtn.setOnAction(e -> joinGame());

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

            loginBtn.setDisable(false);
            registerBtn.setDisable(false);

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
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            log("⚠ Vui lòng nhập đầy đủ tên và mật khẩu.");
            return;
        }

        String hashedPassword = PasswordUtil.hashSHA256(password);
 
        Message ms = new Message(
        	    "LOGIN",
        	    "user=" + username + "&pass=" + hashedPassword
        	);

        out.println(Protocol.encode(ms));

        log("▶ Đã gửi LOGIN: " + username);
        this.myname = username;

        loginBtn.setDisable(true);
        registerBtn.setDisable(true);
        nameField.setDisable(true);
        passwordField.setDisable(true);

        Platform.runLater(this::showGameTable);
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
