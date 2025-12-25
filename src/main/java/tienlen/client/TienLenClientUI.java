package tienlen.client;

import javafx.animation.FadeTransition;
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

public class TienLenClientUI{

    private HBox playerHand;              // Lá bài của người chơi
    private HBox playedCardsPane;         // Các lá bài đã đánh
    private VBox chatBox;                 // Khung chat
    private VBox playerListBox;           // Danh sách người chơi
    private TextArea chatArea;
    private TextField chatInput;
    private Button sendButton;
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

    // Map tên -> VBox playerBox
    private final List<VBox> playerBoxes = new ArrayList<>();

    private final List<ToggleButton> cardButtons = new ArrayList<>();
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String sessionDisplayName = "";
    private long sessionBet = 10000;
    private Runnable onExit;

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
        Circle avatar = new Circle(25, Color.LIGHTGRAY);
        Label nameLabel = new Label(name);
        Label balanceLabel = new Label("0");

        VBox box = new VBox(5, avatar, nameLabel, balanceLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("player-box");

        return box;
    }

    // Thêm bài đã đánh vào giữa bàn
    private void addPlayedCards(List<String> cards) {
        playedCardsPane.getChildren().clear();
        for (String c : cards) {
            Label card = new Label(c);
            card.getStyleClass().add("played-card");

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
        HBox infoBox = new HBox(8);
        infoBox.setPadding(new Insets(6));
        infoBox.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-border-color: #ccc; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label nameLabel = new Label(sessionDisplayName == null || sessionDisplayName.isEmpty() ? "Bàn" : sessionDisplayName);
        nameLabel.setStyle("-fx-font-weight: bold;");
        Label betLabel = new Label("💰 " + sessionBet + " VND");
        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> {
            try { out.println(Protocol.encode(new Message("LEAVE_SESSION", ""))); } catch (Exception ex) {}
            if (onExit != null) onExit.run();
        });
        infoBox.getChildren().addAll(nameLabel, betLabel, exitBtn);

        HBox topBar = new HBox(12, infoBox, topPlayerBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(4));
        topBar.setFillHeight(true);
        HBox.setHgrow(topBar, Priority.ALWAYS);
        
        // Add a spacer and rightPlayerBox to the right of topBar
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(spacer, rightPlayerBox);

        root.setTop(topBar);

        // === Khu vực bài của người chơi ===
        playerHand = new HBox(10);
        playerHand.setAlignment(Pos.CENTER);
        playerHand.setPadding(new Insets(10));
        playerHand.getStyleClass().add("player-hand");

        // Test card
       

        // Nút Play / Pass
        HBox controlBox = new HBox(10);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(5));
        playButton = new Button("Play");
        passButton = new Button("Pass");
        playButton.getStyleClass().add("control-btn");
        passButton.getStyleClass().add("control-btn");
        passButton.setOnAction(e -> passTurn());
        playButton.setOnAction(e -> playSelectedCards());
        
        newGameButton = new Button("New Game");
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
        
        controlBox.getChildren().addAll(playButton, passButton,newGameButton);

            // === Tạo player box cho chính mình (góc dưới trái) ===
            Circle myAvatar = new Circle(25, Color.LIGHTBLUE);
            myNameLabel = new Label(username);
            myNameLabel.setStyle("-fx-font-weight: bold;");
            myBalanceLabel = new Label("0 VND");
            myPlayerBox = new VBox(5, myAvatar, myNameLabel, myBalanceLabel);
            myPlayerBox.setAlignment(Pos.CENTER);
            myPlayerBox.setPadding(new Insets(10));
            myPlayerBox.getStyleClass().add("player-box");
            myPlayerBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5;");

            // Layout dưới cùng: player box bên trái + kiểm soát ở giữa + bài ở dưới
            VBox centerControlBox = new VBox(10, controlBox, playerHand);
            centerControlBox.setAlignment(Pos.CENTER);
            centerControlBox.setFillWidth(true);

            HBox bottomBox = new HBox(15);
            bottomBox.setAlignment(Pos.BOTTOM_LEFT);
            bottomBox.setPadding(new Insets(10));
            bottomBox.getChildren().addAll(myPlayerBox, centerControlBox);
            HBox.setHgrow(centerControlBox, Priority.ALWAYS);

            root.setBottom(bottomBox);

        // === Khung Chat + DS người chơi (chia đôi dọc) ===
        VBox rightPane = new VBox();
        rightPane.setPrefWidth(250);

        chatArea = new TextArea("Trò chơi sẽ bắt đầu khi có hơn 2 người chơi\n");
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        chatInput = new TextField();
        sendButton = new Button("Send");

     // Trong createRootPane() sau khi tạo sendButton
        sendButton.setOnAction(e -> sendChat());
        chatInput.setOnAction(e -> sendChat());

        HBox chatInputBox = new HBox(5, chatInput, sendButton);
        chatInputBox.setAlignment(Pos.CENTER);

        chatBox = new VBox(5, new Label("Chat"), chatArea, chatInputBox);
        chatBox.setAlignment(Pos.CENTER_LEFT);
        chatBox.setPadding(new Insets(10));
        chatBox.getStyleClass().add("chat-box");

        playerListBox = new VBox(5);
        playerListBox.setAlignment(Pos.TOP_LEFT);
        playerListBox.setPadding(new Insets(10));
        playerListBox.getStyleClass().add("player-list");

        VBox.setVgrow(chatBox, Priority.ALWAYS);
        VBox.setVgrow(playerListBox, Priority.ALWAYS);
        rightPane.getChildren().addAll(chatBox, playerListBox);

        /// Gom player bên phải + chat + danh sách vào chung VBox
        VBox outerRight = new VBox(10, rightPlayerBox, rightPane);
        outerRight.setAlignment(Pos.TOP_CENTER);
        outerRight.setPadding(new Insets(5));

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
    	for (VBox box : playerBoxes) {
            Label nameLabel = (Label) box.getChildren().get(1); 
            if (nameLabel.getText().equals(currentPlayer)) {
                box.setStyle("-fx-border-color: gold; -fx-border-width: 3; -fx-background-color: #fff8dc;");
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
        card.setOnAction(e -> {
            if (card.isSelected()) card.setTranslateY(-20);
            else card.setTranslateY(0);
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


}

