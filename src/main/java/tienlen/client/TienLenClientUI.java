package tienlen.client;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tienlen.model.Card;
import tienlen.model.Message;
import tienlen.model.Move;
import tienlen.utils.Protocol;
import tienlen.logic.GameLogic;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class TienLenClientUI{

    private HBox playerHand;              // Lá bài của người chơi
    private HBox playedCardsPane;         // Các lá bài đã đánh
    private VBox chatBox;                 // Khung chat
    private VBox playerListBox;           // Danh sách người chơi
    private TextArea chatArea;
    private TextField chatInput;
    private Button sendButton;
    private Button voiceButton;
    private Button playButton;
    private Button passButton;
    private Move lastMove = null;
    // Các khung hiển thị người chơi
    private VBox leftPlayerBox;
    private VBox topPlayerBox;
    private VBox rightPlayerBox;
    private Button newGameButton;
    private long myBalance = 0;     // Lưu số tiền hiện tại
        private VBox myPlayerBox;       // Khung hiển thị tên + tiền của bản thân (góc dưới trái)
        private Label myNameLabel;      // Tên của bản thân
        private Label myBalanceLabel;   // Số tiền của bản thân
        public HBox getPlayerHand() { return playerHand; }
    public HBox getPlayedCardsPane() { return playedCardsPane; }
    public VBox getPlayerListBox() { return playerListBox; }
    public TextArea getChatArea() { return chatArea; }
    public List<VBox> getPlayerBoxes(){return playerBoxes;}
    private String currentTurn = null;
    private javafx.animation.Animation currentTurnAnimation = null;

    // Map tên -> VBox playerBox
    private final List<VBox> playerBoxes = new ArrayList<>();

    private final List<ToggleButton> cardButtons = new ArrayList<>();
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String sessionDisplayName = "";
    private long sessionBet = 10000;
    private Runnable onExit;

    // Audio recording
    private volatile boolean recording = false;
    private TargetDataLine targetLine;

    public TienLenClientUI(PrintWriter out, BufferedReader in, String username) {
        this.out = out;
        this.in = in;
        this.username = username;
    }

    public TienLenClientUI(PrintWriter out, BufferedReader in, String username, String displayName, long bet) {
        this.out = out;
        this.in = in;
        this.username = username;
        this.sessionDisplayName = displayName == null ? "" : displayName;
        this.sessionBet = bet;
    }

    public void setOnExit(Runnable r) { this.onExit = r; }
    
    
    public Move getLastMove() {
		return lastMove;
	}
	public void setLastMove(Move lastMove) {
		this.lastMove = lastMove;
	}
	
    // Tạo avatar + tên + số dư cho người chơi
    private VBox createPlayerBox(String name) {
        Circle avatar = new Circle(30, Color.web("#2196F3"));
        avatar.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);");
        
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("player-name");
        
        Label balanceLabel = new Label("0 VND");
        balanceLabel.getStyleClass().add("player-balance");
        
        Label statusLabel = new Label("");
        statusLabel.getStyleClass().add("player-status");

        VBox box = new VBox(5, avatar, nameLabel, balanceLabel, statusLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("player-box");

        return box;
    }

    // Thêm bài đã đánh vào giữa bàn
    private void addPlayedCards(List<String> cards) {
        playedCardsPane.getChildren().clear();
        for (String c : cards) {
            Label card = new Label(c);
            card.getStyleClass().add("played-card");
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(65);
            card.setPrefHeight(95);
            card.setMinWidth(65);
            card.setMinHeight(95);
            card.setMaxWidth(65);
            card.setMaxHeight(95);

            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            playedCardsPane.getChildren().add(card);
        }
    }
    public BorderPane createRootPane() {
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");


        // === Trung tâm bàn chơi ===
        StackPane tablePane = new StackPane();
        tablePane.setPadding(new Insets(10));

        playedCardsPane = new HBox(10);
        playedCardsPane.setAlignment(Pos.CENTER);
        playedCardsPane.getStyleClass().add("played-cards");

        tablePane.getChildren().add(playedCardsPane);
        root.setCenter(tablePane);

        // === Người chơi trái / trên / phải ===
        leftPlayerBox = createPlayerBox("Player Left");
        topPlayerBox = createPlayerBox("Player Top");
        rightPlayerBox = createPlayerBox("Player Right");
       

        // Lưu tất cả để tiện update
        playerBoxes.add(leftPlayerBox);
        playerBoxes.add(topPlayerBox);
        playerBoxes.add(rightPlayerBox);
        


        BorderPane.setAlignment(leftPlayerBox, Pos.CENTER_LEFT);
        BorderPane.setAlignment(topPlayerBox, Pos.TOP_CENTER);
        BorderPane.setAlignment(rightPlayerBox, Pos.CENTER_RIGHT);

        root.setLeft(leftPlayerBox);
        
        // Create a top bar with info box on the left and the top player box to its right
        HBox infoBox = new HBox(15);
        infoBox.setPadding(new Insets(10, 15, 10, 15));
        infoBox.getStyleClass().add("info-box");
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(sessionDisplayName == null || sessionDisplayName.isEmpty() ? "🎴 Bàn" : "🎴 " + sessionDisplayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label betLabel = new Label("💰 Cược: " + sessionBet + " VND");
        betLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        Button exitBtn = new Button("Thoát");
        exitBtn.getStyleClass().add("info-button");
        exitBtn.setOnAction(e -> {
            try { out.println(Protocol.encode(new Message("LEAVE_SESSION", ""))); } catch (Exception ex) {}
            if (onExit != null) onExit.run();
        });
        
        infoBox.getChildren().addAll(nameLabel, betLabel, exitBtn);

        HBox topBar = new HBox(15, infoBox, topPlayerBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8));
        topBar.setFillHeight(true);
        HBox.setHgrow(topBar, Priority.ALWAYS);
        
        // Add a spacer and rightPlayerBox to the right of topBar
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(spacer, rightPlayerBox);

        root.setTop(topBar);

        // === Khu vực bài của người chơi ===
        playerHand = new HBox(3);
        playerHand.setAlignment(Pos.CENTER_LEFT);
        playerHand.setPadding(new Insets(10, 15, 10, 15));
        playerHand.getStyleClass().add("player-hand");
        playerHand.setPrefHeight(160);
        
        // ScrollPane for hand
        ScrollPane handScroll = new ScrollPane(playerHand);
        handScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        handScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handScroll.setFitToHeight(true);
        handScroll.setPannable(true);

        // Nút Play / Pass
        HBox controlBox = new HBox(12);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(10));
        controlBox.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-border-radius: 8; -fx-background-radius: 8;");
        
        playButton = new Button("Đánh Bài");
        passButton = new Button("Bỏ Qua");
        playButton.getStyleClass().add("control-btn");
        passButton.getStyleClass().add("control-btn");
        passButton.setOnAction(e -> passTurn());
        playButton.setOnAction(e -> playSelectedCards());
        
        newGameButton = new Button("Ván Mới");
        newGameButton.getStyleClass().add("control-btn");
        newGameButton.setVisible(true);    
        newGameButton.setManaged(true);   

        newGameButton.setOnAction(e -> {
            Message ms = new Message("NEWGAME", username);
            lastMove = null;
            out.println(Protocol.encode(ms));
            newGameButton.setVisible(false);
            newGameButton.setManaged(false);
        });
        
        controlBox.getChildren().addAll(playButton, passButton, newGameButton);

        // === Tạo player box cho chính mình (góc dưới trái) ===
        Circle myAvatar = new Circle(30, Color.web("#00BCD4"));
        myAvatar.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);");
        
        myNameLabel = new Label(username);
        myNameLabel.getStyleClass().add("player-name");
        
        myBalanceLabel = new Label("0 VND");
        myBalanceLabel.getStyleClass().add("player-balance");
        
        Label myStatusLabel = new Label("Sẵn sàng");
        myStatusLabel.getStyleClass().add("player-status");
        
        myPlayerBox = new VBox(5, myAvatar, myNameLabel, myBalanceLabel, myStatusLabel);
        myPlayerBox.setAlignment(Pos.CENTER);
        myPlayerBox.setPadding(new Insets(12));
        myPlayerBox.getStyleClass().add("player-box");
        playerBoxes.add(myPlayerBox); // Thêm vào danh sách để nhận hiệu ứng turn

        // Layout dưới cùng: player box bên trái + kiểm soát ở giữa + bài ở dưới
        VBox centerControlBox = new VBox(12, controlBox, handScroll);
        centerControlBox.setAlignment(Pos.CENTER);
        centerControlBox.setFillWidth(true);
        VBox.setVgrow(handScroll, Priority.ALWAYS);

        HBox bottomBox = new HBox(15);
        bottomBox.setAlignment(Pos.BOTTOM_LEFT);
        bottomBox.setPadding(new Insets(10));
        bottomBox.getChildren().addAll(myPlayerBox, centerControlBox);
        HBox.setHgrow(centerControlBox, Priority.ALWAYS);

        root.setBottom(bottomBox);

        // === Khung Chat + DS người chơi (chia đôi dọc) ===
        VBox rightPane = new VBox(10);
        rightPane.setPrefWidth(280);

        Label chatLabel = new Label("💬 TRÒ CHUYỆN");
        chatLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        chatArea = new TextArea("🎮 Trò chơi sẽ bắt đầu khi có đủ 2 người chơi\n");
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.getStyleClass().add("chat-area");

        chatInput = new TextField();
        chatInput.getStyleClass().add("chat-input");
        chatInput.setPromptText("Nhập tin nhắn...");
        
        sendButton = new Button("Gửi");
        sendButton.getStyleClass().add("chat-button");
        sendButton.setOnAction(e -> sendChat());
        chatInput.setOnAction(e -> sendChat());

        voiceButton = new Button("🎙️");
        voiceButton.getStyleClass().add("voice-button");
        voiceButton.setPrefWidth(45);
        voiceButton.setOnAction(e -> {
            if (!recording) startRecording();
            else stopRecordingAndSend();
        });

        HBox chatInputBox = new HBox(8, chatInput, sendButton, voiceButton);
        chatInputBox.setAlignment(Pos.CENTER);
        chatInputBox.setPadding(new Insets(8));
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        chatBox = new VBox(8, chatLabel, chatArea, chatInputBox);
        chatBox.setAlignment(Pos.TOP_LEFT);
        chatBox.setPadding(new Insets(12));
        chatBox.getStyleClass().add("chat-box");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        Label playerListLabel = new Label("👥 NGƯỜI CHƠI");
        playerListLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        playerListBox = new VBox(8);
        playerListBox.setAlignment(Pos.TOP_LEFT);
        playerListBox.setPadding(new Insets(12));
        playerListBox.getStyleClass().add("player-list");
        
        ScrollPane playerListScroll = new ScrollPane(playerListBox);
        playerListScroll.setFitToWidth(true);
        playerListScroll.setPrefHeight(150);
        
        VBox playerListContainer = new VBox(5, playerListLabel, playerListScroll);
        VBox.setVgrow(playerListScroll, Priority.ALWAYS);

        VBox.setVgrow(chatBox, Priority.ALWAYS);
        rightPane.getChildren().addAll(chatBox, playerListContainer);

        /// Gom player bên phải + chat + danh sách vào chung VBox
        VBox outerRight = new VBox(12, rightPlayerBox, rightPane);
        outerRight.setAlignment(Pos.TOP_CENTER);
        outerRight.setPadding(new Insets(8));
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        root.setRight(outerRight);

        disableButton(); 
        
        return root;
    }
    private void playSelectedCards() {
    	
    	if (!currentTurn.equals(username)) {
        	chatArea.appendText("Không phải lượt của bạn!\n");
            return;
        }
    	List<Card> selectedCards = new ArrayList<>();

        for (ToggleButton btn : cardButtons) {
            if (btn.isSelected()) {
                Card card = new Card(btn.getText()); 
                selectedCards.add(card);
            }
        }

        if (selectedCards.isEmpty()) {
            chatArea.appendText("⚠ Bạn chưa chọn lá nào!\n");
            return;
        }
     // Xác định loại combo
        Move.ComboType type = GameLogic.identifyType(selectedCards);
        if (type == Move.ComboType.INVALID) {
            chatArea.appendText("❌ Bộ bài không hợp lệ!\n");
            return;
        }

//         Kiểm tra có chặt được hay không (nếu đã có bàn trước đó)
        if (lastMove != null) {
            Move prev = lastMove;
            Move next = new Move(selectedCards);
            if (!GameLogic.canBeat(prev, next)) {
                chatArea.appendText("❌ Bộ này không chặt được bài trên bàn!\n");
                return;
            }
        }
       

        // Gửi đến server
        String data = selectedCards.get(0).toString();
        for(int i = 1; i < selectedCards.size(); i++) {
        	data = data + "," + selectedCards.get(i).toString();
        }
        Message ms = new Message("PLAY", data);
        out.println(Protocol.encode(ms));
        
        cardButtons.removeIf(btn -> {
            if (btn.isSelected()) {
                playerHand.getChildren().remove(btn);
                return true;
            }
            return false;
        });
    }

    public void highlightTurn(String currentPlayer) {
    	if(currentPlayer == null) return;
    	this.currentTurn = currentPlayer;
    	
    	// Dừng animation cũ nếu có
    	if (currentTurnAnimation != null) {
    		currentTurnAnimation.stop();
    	}
    	
    	for (VBox box : playerBoxes) {
            Label nameLabel = (Label) box.getChildren().get(1); 
            if (nameLabel.getText().equals(currentPlayer)) {
                // Áp dụng style với glow effect - background nhạt để text dễ đọc
                box.setStyle("-fx-border-color: #ffeb3b; -fx-border-width: 3; " +
                    "-fx-background-color: linear-gradient(to bottom, rgba(255,235,59,0.08), rgba(255,193,7,0.03)); " +
                    "-fx-effect: dropshadow(gaussian, #ffeb3b, 15, 0.8, 0, 0);");
                
                // Thêm animation scale với glow
                ScaleTransition st = new ScaleTransition(Duration.millis(1200), box);
                st.setFromX(1.0);
                st.setToX(1.08);
                st.setFromY(1.0);
                st.setToY(1.08);
                st.setCycleCount(javafx.animation.Animation.INDEFINITE);
                st.setAutoReverse(true);
                currentTurnAnimation = st;
                st.play();
                
                // Thông báo
                if (nameLabel.getText().equals(username)) {
                    chatArea.appendText("\n🎯 ĐẾN LƯỢT CỦA BẠN!\n");
                    chatArea.setStyle("-fx-control-inner-background: #fff3cd;");
                } else {
                    chatArea.appendText("\n▶ Lượt của: " + currentPlayer + "\n");
                    chatArea.setStyle("-fx-control-inner-background: transparent;");
                }
            } else {
                box.setStyle(""); // reset lại style
            }
        }
    }
    public void updatePlayerList(String[] playersWithBalances, String myName) {
        playerListBox.getChildren().clear();
        playerListBox.getChildren().add(new Label("Players"));

        // Update my balance in header and fill player list
        for (String entry : playersWithBalances) {
            String[] parts = entry.split(":");
            String name = parts[0];
            String bal = parts.length > 1 ? parts[1] : "0";
            if (name.equals(myName)) {
                myBalance = Long.parseLong(bal);
                    myBalanceLabel.setText(bal + " VND");
            }
            playerListBox.getChildren().add(new Label(name + " - " + bal));
        }

        int myIndex = -1;
        String[] names = new String[playersWithBalances.length];
        for (int i = 0; i < playersWithBalances.length; i++) {
            String[] parts = playersWithBalances[i].split(":");
            names[i] = parts[0];
            if (names[i].equals(myName)) myIndex = i;
        }

        if (myIndex != -1) {
            for (int offset = 1; offset < names.length; offset++) {
                int pos = (myIndex + offset) % names.length;
                String[] parts = playersWithBalances[pos].split(":");
                String playerName = parts[0];
                String balance = parts.length > 1 ? parts[1] : "0";

                if (offset == 1 && playerBoxes.size() > 0) {
                    Label nameLabel = (Label) playerBoxes.get(0).getChildren().get(1);
                    Label balLabel = (Label) playerBoxes.get(0).getChildren().get(2);
                    nameLabel.setText(playerName);
                    balLabel.setText(balance);
                } else if (offset == 2 && playerBoxes.size() > 1) {
                    Label nameLabel = (Label) playerBoxes.get(1).getChildren().get(1);
                    Label balLabel = (Label) playerBoxes.get(1).getChildren().get(2);
                    nameLabel.setText(playerName);
                    balLabel.setText(balance);
                } else if (offset == 3 && playerBoxes.size() > 2) {
                    Label nameLabel = (Label) playerBoxes.get(2).getChildren().get(1);
                    Label balLabel = (Label) playerBoxes.get(2).getChildren().get(2);
                    nameLabel.setText(playerName);
                    balLabel.setText(balance);
                }
            }
        }
        highlightTurn(currentTurn);
    }
    public void addCardToHand(String cardText) {
        ToggleButton card = new ToggleButton(cardText);
        card.getStyleClass().add("card");
        
        // Thêm animation khi chọn/bỏ chọn
        card.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Khi chọn: nâng lên
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), card);
                tt.setByY(-15);
                tt.play();
            } else {
                // Khi bỏ chọn: hạ xuống
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), card);
                tt.setByY(15);
                tt.play();
            }
        });
        
        cardButtons.add(card);
        playerHand.getChildren().add(card);
    }

    public void setHand(List<String> cards) {
        playerHand.getChildren().clear();
        cardButtons.clear();
        for (String c : cards) {
            addCardToHand(c);
        }
        playButton.setDisable(false);
        passButton.setDisable(false);
        playerHand.setDisable(false);
        newGameButton.setVisible(false);
        newGameButton.setManaged(false);
    }

    public List<ToggleButton> getCardButtons() {
        return cardButtons;
    }

   
    public void showPlayedCards(String moveData) {
        String[] parts = moveData.split(",");
        String playerName = parts[0];
        List<String> cards = new ArrayList<>();
        List<Card> listCards = new ArrayList<>();
        for(int i = 1; i <parts.length;i++) {
        	cards.add(parts[i]);
        	Card card = new Card(parts[i]); 
            listCards.add(card);
        }
        lastMove = new Move(listCards);
        addPlayedCards(cards);
    }
    private void passTurn() {
        // Nếu không có lastMove (tức mình được đánh đầu) thì không thể Pass
        if (lastMove == null) {
            chatArea.appendText("⚠ Không thể bỏ lượt khi chưa có ai đánh!\n");
            return;
        }

        // Gửi thông điệp PASS lên server
        Message ms = new Message("PASS", ""); 
        out.println(Protocol.encode(ms));

        
    }

    public void disableButton() {
    	 playButton.setDisable(true);
         passButton.setDisable(true);
         playerHand.setDisable(true);
	}
    public void showNewGameButton() {
        newGameButton.setVisible(true);
        newGameButton.setManaged(true);
    }
    
    public void updateBalance(long newBalance) {
        this.myBalance = newBalance;
            myBalanceLabel.setText(newBalance + " VND");
    }
    
    public long getMyBalance() {
        return myBalance;
    }
    
    private void sendChat() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            Message ms = new Message("CHAT", username + ": " + text);
            out.println(Protocol.encode(ms));
            chatInput.clear();
        }
    }

    private void startRecording() {
        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();
            recording = true;
            voiceButton.setText("⏹");

            Thread t = new Thread(() -> {
                try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    while (recording) {
                        int count = targetLine.read(buffer, 0, buffer.length);
                        if (count > 0) outStream.write(buffer, 0, count);
                    }
                    // build wav bytes
                    byte[] pcm = outStream.toByteArray();
                    byte[] wav = toWav(pcm, format);
                    String b64 = Base64.getEncoder().encodeToString(wav);
                    Message ms = new Message(Protocol.CHAT_VOICE, username + "|" + b64);
                    out.println(Protocol.encode(ms));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingAndSend() {
        recording = false;
        voiceButton.setText("🎙");
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
    }

    private byte[] toWav(byte[] pcm, AudioFormat format) throws IOException {
        int channels = format.getChannels();
        int sampleRate = (int) format.getSampleRate();
        int bitsPerSample = format.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // RIFF header
        out.write(new byte[]{'R','I','F','F'});
        int dataLen = pcm.length + 36;
        out.write(intToLittleEndian(dataLen));
        out.write(new byte[]{'W','A','V','E'});
        // fmt chunk
        out.write(new byte[]{'f','m','t',' '});
        out.write(intToLittleEndian(16)); // subchunk1 size
        out.write(shortToLittleEndian((short)1)); // PCM
        out.write(shortToLittleEndian((short)channels));
        out.write(intToLittleEndian(sampleRate));
        out.write(intToLittleEndian(byteRate));
        out.write(shortToLittleEndian((short)(channels * bitsPerSample/8)));
        out.write(shortToLittleEndian((short)bitsPerSample));
        // data chunk
        out.write(new byte[]{'d','a','t','a'});
        out.write(intToLittleEndian(pcm.length));
        out.write(pcm);

        return out.toByteArray();
    }

    private byte[] intToLittleEndian(int val) {
        return new byte[]{(byte)(val & 0xff), (byte)((val>>8)&0xff), (byte)((val>>16)&0xff), (byte)((val>>24)&0xff)};
    }

    private byte[] shortToLittleEndian(short val) {
        return new byte[]{(byte)(val & 0xff), (byte)((val>>8)&0xff)};
    }


}

