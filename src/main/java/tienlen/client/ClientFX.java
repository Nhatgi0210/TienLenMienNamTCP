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
    private TableSelectionUI tableSelectionUI;
    private Stage primaryStage;
    private ServerListener universalListener;  // Listener duy nhất cho tất cả

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

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
        loginBtn.setOnAction(e -> loginGame());
        registerBtn.setOnAction(e -> showRegisterScreen());

        Scene scene = new Scene(root, 520, 410);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Tiến Lên Client");
        primaryStage.show();
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

            // Khởi động ServerListener duy nhất để xử lý TẤT CẢ tin nhắn từ server
            // (bao gồm cả register/login/table selection/game messages)
            universalListener = new ServerListener(in, this);
            Thread listenerThread = new Thread(universalListener);
            listenerThread.setDaemon(true);
            listenerThread.start();

            loginBtn.setDisable(false);
            registerBtn.setDisable(false);

            connectBtn.setDisable(true);
            ipField.setDisable(true);

        } catch (IOException ex) {
            log("❌ Kết nối thất bại: " + ex.getMessage());
        }
    }

    private void loginGame() {
        String username = nameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            log("⚠ Vui lòng nhập đầy đủ tên và mật khẩu.");
            return;
        }

        String hashedPassword = PasswordUtil.hashSHA256(password);
 
        Message ms = new Message(
        	    Protocol.LOGIN,
        	    "user=" + username + "&pass=" + hashedPassword
        	);

        out.println(Protocol.encode(ms));

        log("▶ Đang đăng nhập...");

        loginBtn.setDisable(true);
        registerBtn.setDisable(true);
        nameField.setDisable(true);
        passwordField.setDisable(true);
    }

    public void showRegisterScreen() {
        Label title = new Label("📝 Đăng Ký Tài Khoản");
        title.setFont(Font.font("Segoe UI", 28));
        title.setTextFill(Color.web("#FF6F61"));

        TextField regUsername = new TextField();
        regUsername.setPromptText("Tên đăng nhập");
        regUsername.setPrefWidth(200);

        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Mật khẩu");
        regPassword.setPrefWidth(200);

        PasswordField regPasswordConfirm = new PasswordField();
        regPasswordConfirm.setPromptText("Xác nhận mật khẩu");
        regPasswordConfirm.setPrefWidth(200);

        TextArea regStatus = new TextArea();
        regStatus.setEditable(false);
        regStatus.setPrefHeight(100);
        regStatus.setStyle("-fx-font-size: 12pt; -fx-control-inner-background: #FFFFFF;");

        GridPane regGrid = new GridPane();
        regGrid.setHgap(10);
        regGrid.setVgap(10);
        regGrid.setAlignment(Pos.CENTER);
        regGrid.add(new Label("Tên đăng nhập:"), 0, 0);
        regGrid.add(regUsername, 1, 0);
        regGrid.add(new Label("Mật khẩu:"), 0, 1);
        regGrid.add(regPassword, 1, 1);
        regGrid.add(new Label("Xác nhận mật khẩu:"), 0, 2);
        regGrid.add(regPasswordConfirm, 1, 2);

        Button submitRegBtn = new Button("ĐĂNG KÝ");
        styleButton(submitRegBtn, "#4CAF50", "#66BB6A");

        Button backBtn = new Button("QUAY LẠI");
        styleButton(backBtn, "#F44336", "#EF5350");

        HBox btnBox = new HBox(15, submitRegBtn, backBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, title, regGrid, regStatus, btnBox);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #E0F7FA;");

        submitRegBtn.setOnAction(e -> {
            String username = regUsername.getText().trim();
            String password = regPassword.getText().trim();
            String passwordConfirm = regPasswordConfirm.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                regStatus.setText("❌ Vui lòng nhập đầy đủ thông tin");
                return;
            }

            if (!password.equals(passwordConfirm)) {
                regStatus.setText("❌ Mật khẩu xác nhận không khớp");
                return;
            }

            if (password.length() < 6) {
                regStatus.setText("❌ Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }

            regStatus.setText("⏳ Đang đăng ký...");
            submitRegBtn.setDisable(true);

            String hashedPassword = PasswordUtil.hashSHA256(password);
            Message msg = new Message(Protocol.REGISTER, "user=" + username + "&pass=" + hashedPassword);
            out.println(Protocol.encode(msg));
        });

        backBtn.setOnAction(e -> showLoginScreen());

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
    }

    public void showLoginScreen() {
        // Tạo lại giao diện login
        Label title = new Label("Tiến Lên Miền Nam");
        title.setFont(Font.font("Segoe UI", 30));
        title.setTextFill(Color.web("#FF6F61"));

        ipField = new TextField("localhost");
        ipField.setPrefWidth(150);
        connectBtn = new Button("Connect");
        styleButton(connectBtn, "#4CAF50", "#66BB6A");

        HBox ipBox = new HBox(10, new Label("Server IP:"), ipField, connectBtn);
        ipBox.setAlignment(Pos.CENTER);

        nameField = new TextField();
        nameField.setPrefWidth(150);

        passwordField = new PasswordField();
        passwordField.setPrefWidth(150);

        loginBtn = new Button("ĐĂNG NHẬP");
        styleButton(loginBtn, "#2196F3", "#42A5F5");
        loginBtn.setDisable(true);

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

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle("-fx-control-inner-background: #FFFFFF; -fx-font-family: 'Consolas'; -fx-font-size: 12pt; -fx-background-radius: 10;");

        VBox root = new VBox(20, title, ipBox, loginBox, logArea);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #E0F7FA;");

        connectBtn.setOnAction(e -> connectToServer());
        loginBtn.setOnAction(e -> loginGame());
        registerBtn.setOnAction(e -> showRegisterScreen());

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        log("▶ Quay lại màn hình đăng nhập");
    }

    // Callback từ AuthListener khi đăng nhập thành công
    public void onLoginSuccess(String username) {
        this.myname = username;
        log("✅ Đăng nhập thành công: " + username);
        showTableSelection();
    }

    // Re-enable login controls khi đăng nhập thất bại
    public void enableLoginControls() {
        loginBtn.setDisable(false);
        registerBtn.setDisable(false);
        nameField.setDisable(false);
        passwordField.setDisable(false);
    }

    // Re-enable register controls khi đăng ký thất bại
    public void enableRegisterControls() {
        // Gọi lại showRegisterScreen để refresh (hoặc có thể implement cách khác)
        // Bây giờ chỉ cần để trống vì showRegisterScreen sẽ được gọi lại nếu cần
    }

  
// Hàm hiển thị giao diện chọn bàn chơi
    private void showTableSelection() {
        tableSelectionUI = new TableSelectionUI(out, in, myname);
        VBox tableSelectionPane = tableSelectionUI.createRootPane();
        
        // Cập nhật listener để xử lý TABLE_SELECTION
        universalListener.setTableSelectionUI(tableSelectionUI, myname);
        
        tableSelectionUI.setOnTableSelected(() -> {
            // Sau khi chọn bàn, chuyển sang giao diện game
            Platform.runLater(this::showGameTable);
        });
        
        tableSelectionUI.setOnQuit(() -> {
            // Quay lại màn hình login
            showLoginScreen();
        });
        
        Scene tableScene = new Scene(tableSelectionPane, 600, 500);
        primaryStage.setScene(tableScene);
        log("▶ Hiển thị giao diện chọn bàn chơi");
        
        // Yêu cầu danh sách bàn sau khi UI đã sẵn sàng
        tableSelectionUI.refreshTablesList();
    }

    // Hàm tạo giao diện bàn chơi
    private void showGameTable() {
        // Try to get selected table info (displayName & bet) from previous selection UI
        String displayName = "";
        long bet = 10000;
        if (tableSelectionUI != null) {
            TableSelectionUI.TableInfo info = tableSelectionUI.getSelectedTableInfo();
            if (info != null) {
                displayName = info.displayName;
                bet = info.betAmount;
            }
        }

        TienLenClientUI gameTable = new TienLenClientUI(out, in, myname, displayName, bet);
        // Set callback when user exits the table
        gameTable.setOnExit(() -> {
            // Return to table selection screen
            Platform.runLater(() -> showTableSelection());
        });

        Scene gameScene = new Scene(gameTable.createRootPane(), 1300, 865);
        gameScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(gameScene);
        
        // Cập nhật listener để xử lý GAME messages
        universalListener.setGameUI(gameTable, myname);
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
