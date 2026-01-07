package tienlen.client;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import tienlen.model.Message;
import tienlen.utils.Protocol;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;

public class ServerListener implements Runnable {

    private BufferedReader in;
    private String myName;
    private TienLenClientUI gameTable;
    private TableSelectionUI tableSelectionUI;
    private ClientFX clientFX;

    // Constructor chính: dùng khi cần gọi callbacks từ ClientFX
    public ServerListener(BufferedReader in, ClientFX clientFX) {
        this.in = in;
        this.clientFX = clientFX;
    }
    
    public ServerListener(BufferedReader in) {
        this.in = in;
    }
    
    public ServerListener(BufferedReader in,
                          TienLenClientUI gameTable,
                          String myName 
                        
    		) {
        this.in = in;
        this.gameTable = gameTable;
        this.myName = myName;
    }

    public ServerListener(BufferedReader in,
                          TableSelectionUI tableSelectionUI,
                          String myName,
                          boolean isTableSelection
    		) {
        this.in = in;
        this.tableSelectionUI = tableSelectionUI;
        this.myName = myName;
    }

    // Cập nhật UI cho TableSelection
    public synchronized void setTableSelectionUI(TableSelectionUI ui, String username) {
        this.tableSelectionUI = ui;
        this.myName = username;
        this.gameTable = null;
    }

    // Cập nhật UI cho Game
    public synchronized void setGameUI(TienLenClientUI ui, String username) {
        this.gameTable = ui;
        this.myName = username;
        this.tableSelectionUI = null;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message ms = Protocol.decode(line);
                System.out.println("ServerListener received: " + ms.getAction());

                // ========== XỬ LÝ AUTHENTICATION MESSAGES ==========
                // (Bây giờ ServerListener xử lý register/login thay vì AuthListener)
                
                if (ms.getAction().equals(Protocol.REGISTER_SUCCESS)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Đăng ký thành công");
                        alert.setHeaderText(null);
                        alert.setContentText("✅ Đăng ký thành công! Vui lòng đăng nhập.");
                        alert.showAndWait();
                        if (clientFX != null) {
                            clientFX.showLoginScreen();
                        }
                    });
                    continue;
                }
                
                if (ms.getAction().equals(Protocol.REGISTER_FAILED)) {
                    String error = ms.getData();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Đăng ký thất bại");
                        alert.setHeaderText(null);
                        alert.setContentText("❌ " + error);
                        alert.showAndWait();
                        if (clientFX != null) {
                            clientFX.enableRegisterControls();
                        }
                    });
                    continue;
                }
                
                if (ms.getAction().equals(Protocol.LOGIN_SUCCESS)) {
                    String username = ms.getData();
                    Platform.runLater(() -> {
                        if (clientFX != null) {
                            clientFX.onLoginSuccess(username);
                        }
                    });
                    continue;
                }
                
                if (ms.getAction().equals(Protocol.BALANCE)) {
                    String balanceStr = ms.getData();
                    try {
                        long balance = Long.parseLong(balanceStr);
                        Platform.runLater(() -> {
                            if (tableSelectionUI != null) {
                                tableSelectionUI.updateBalance(balance);
                            }
                        });
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid balance: " + balanceStr);
                    }
                    continue;
                }
                
                if (ms.getAction().equals(Protocol.LOGIN_FAILED)) {
                    String error = ms.getData();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Đăng nhập thất bại");
                        alert.setHeaderText(null);
                        alert.setContentText("❌ " + error);
                        alert.showAndWait();
                        if (clientFX != null) {
                            clientFX.enableLoginControls();
                        }
                    });
                    continue;
                }

                // ========== XỬ LÝ TABLE SELECTION MESSAGES ==========
                if (tableSelectionUI != null) {
                    switch (ms.getAction()) {
                        case Protocol.SESSION_LIST: {
                            String sessionData = ms.getData();
                            Platform.runLater(() -> {
                                tableSelectionUI.updateSessionList(sessionData);
                            });
                            continue;
                        }
                        case Protocol.SESSION_DETAILS: {
                            String detailsData = ms.getData();
                            Platform.runLater(() -> {
                                tableSelectionUI.updateSessionDetails(detailsData);
                            });
                            continue;
                        }
                        case "SESSION_SELECTED": {
                            // Xử lý khi chọn bàn thành công
                            Platform.runLater(() -> {
                                System.out.println("Session selected: " + ms.getData());
                            });
                            continue;
                        }
                    }
                }

                // ========== XỬ LÝ GAME MESSAGES ==========
                if (gameTable != null) {
                    switch (ms.getAction()) {
                        case "HAND": {              
                            String[] cards = ms.getData().split(",");
                            Platform.runLater(() -> {
                                List<String> cardList = new ArrayList<>();
                                for (String c : cards) cardList.add(c.trim());
                                gameTable.setHand(cardList);
                            });
                            break;
                        }
                        case Protocol.PLAYER_LIST: {
                            // Hiển thị danh sách người chơi
                            String playerListData = ms.getData();
                            String[] players = playerListData.split(",");
                            Platform.runLater(() -> {
                                gameTable.updatePlayerList(players, myName);
                            });
                            break;
                        }
                        case "TURN": {
                            String currentPlayer = ms.getData();
                            Platform.runLater(() -> {
                                gameTable.highlightTurn(currentPlayer);
                            });
                            break;
                        }
                        case Protocol.PLAY: {
                            // Người chơi A đã đánh những quân bài gì
                            String moveData = ms.getData();
                            Platform.runLater(() -> {
                                gameTable.showPlayedCards(moveData);
                            });
                            break;
                        }
                        case Protocol.PASS: {
                            Platform.runLater(() -> {
                                gameTable.getPlayedCardsPane().getChildren().clear();
                                gameTable.setLastMove(null);
                            });
                            break;
                        }
                        case "WIN": {
                            String winData = ms.getData();
                            String[] parts = winData.split("\\|");
                            String winnerName = parts[0];
                            String balanceInfoStr = parts.length > 1 ? parts[1] : "";
                            
                            final String winnerFinal = winnerName;
                            final String balanceInfoFinal = balanceInfoStr;
                            
                            Platform.runLater(() -> {
                                // Update my balance from balance info
                                if (!balanceInfoFinal.isEmpty()) {
                                    String[] balanceEntries = balanceInfoFinal.split(",");
                                    for (String entry : balanceEntries) {
                                        String[] kv = entry.split(":");
                                        if (kv.length == 2 && kv[0].equals(myName)) {
                                            long newBalance = Long.parseLong(kv[1]);
                                            gameTable.updateBalance(newBalance);
                                        }
                                    }
                                }
                                
                                // Show win alert
                                if (winnerFinal.equals(myName)) {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Chiến thắng!");
                                    alert.setHeaderText(null);
                                    alert.setContentText("🎉 Bạn đã thắng ván này!");
                                    alert.showAndWait();
                                    gameTable.showNewGameButton();
                                } else {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Kết quả");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Người chơi " + winnerFinal + " đã thắng!");
                                    alert.showAndWait();
                                }
                                gameTable.disableButton();
                            });
                            break;
                        }
                        case "END": {
                            // Kết thúc ván chơi
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("End game!");
                                alert.setHeaderText(null);
                                alert.setContentText("🎉 Các người chơi đã thoát! ván chơi kết thúc!");
                                alert.showAndWait();
                                gameTable.showNewGameButton();
                                gameTable.disableButton();
                            });
                            break;
                        }
                        case Protocol.CHAT: {
                            String msg = ms.getData();
                            Platform.runLater(() -> gameTable.getChatArea().appendText(msg + "\n"));
                            break;
                        }
                        case Protocol.CHAT_VOICE: {
                            String data = ms.getData();
                            String[] parts = data.split("\\|", 2);
                            if (parts.length < 2) break;
                            String sender = parts[0];
                            String b64 = parts[1];
                            try {
                                byte[] wavBytes = Base64.getDecoder().decode(b64);
                                Path tmp = Files.createTempFile("tienlen_voice_", ".wav");
                                Files.write(tmp, wavBytes);
                                File f = tmp.toFile();
                                Platform.runLater(() -> {
                                    gameTable.getChatArea().appendText("[Voice] " + sender + " sent a voice message\n");
                                    try {
                                        Media media = new Media(f.toURI().toString());
                                        MediaPlayer player = new MediaPlayer(media);
                                        player.play();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        case "NOTIFICATION": {
                            System.out.println(ms.getData());
                            break;
                        }
                        case "KICKED": {
                            String reason = ms.getData();
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setTitle("Bị loại khỏi phòng");
                                alert.setHeaderText(null);
                                alert.setContentText(reason);
                                alert.showAndWait();
                                // Trở về màn hình chọn bàn
                                if (clientFX != null) {
                                    clientFX.returnToTableSelection();
                                }
                            });
                            break;
                        }
                        case Protocol.SESSION_CLOSED: {
                            String data = ms.getData();
                            String[] parts = data.split("\\|", 2);
                            String sessId = parts.length > 0 ? parts[0] : "";
                            String reason = parts.length > 1 ? parts[1] : "Session closed";
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Session Closed");
                                alert.setHeaderText(null);
                                alert.setContentText("Session " + sessId + " closed: " + reason);
                                alert.showAndWait();
                                if (clientFX != null) clientFX.returnToTableSelection();
                            });
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
