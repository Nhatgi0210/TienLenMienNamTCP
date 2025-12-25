package tienlen.client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import tienlen.model.Message;
import tienlen.utils.Protocol;

import java.io.BufferedReader;

public class AuthListener implements Runnable {
    private BufferedReader in;
    private ClientFX clientFX;

    public AuthListener(BufferedReader in, ClientFX clientFX) {
        this.in = in;
        this.clientFX = clientFX;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message ms = Protocol.decode(line);
                System.out.println("AuthListener received: " + ms.getAction());

                switch (ms.getAction()) {
                    case Protocol.REGISTER_SUCCESS: {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Đăng ký thành công");
                            alert.setHeaderText(null);
                            alert.setContentText("✅ Đăng ký thành công! Vui lòng đăng nhập.");
                            alert.showAndWait();
                            clientFX.showLoginScreen();
                        });
                        break;
                    }
                    case Protocol.REGISTER_FAILED: {
                        String error = ms.getData();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Đăng ký thất bại");
                            alert.setHeaderText(null);
                            alert.setContentText("❌ " + error);
                            alert.showAndWait();
                            clientFX.enableRegisterControls();
                        });
                        break;
                    }
                    case Protocol.LOGIN_SUCCESS: {
                        String username = ms.getData();
                        Platform.runLater(() -> {
                            clientFX.onLoginSuccess(username);
                        });
                        break;
                    }
                    case Protocol.LOGIN_FAILED: {
                        String error = ms.getData();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Đăng nhập thất bại");
                            alert.setHeaderText(null);
                            alert.setContentText("❌ " + error);
                            alert.showAndWait();
                            clientFX.enableLoginControls();
                        });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
