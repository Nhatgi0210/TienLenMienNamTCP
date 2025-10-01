package tienlen.client;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tienlen.model.Message;
import tienlen.utils.Protocol;

public class ServerListener implements Runnable {

    private  BufferedReader in;
    private String myName;
    private TienLenClientUI gameTable;
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

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message ms = Protocol.decode(line);

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

                    case "TURN": {
                    	String currentPlayer = ms.getData();
                        Platform.runLater(() -> {
                            gameTable.highlightTurn(currentPlayer);
                        });
                        
                        break;
                    }

                    case "PLAY": {
                        String moveData = ms.getData(); // nhat,3♠,4♠
                        Platform.runLater(() -> {
                            gameTable.showPlayedCards(moveData);
                        });
                      
                        break;
                    }
                    case "PASS":{
                    	gameTable.setLastMove(null);
                    	Platform.runLater(() -> {
                    		gameTable.getPlayedCardsPane().getChildren().clear();
                        });
                    	
                    	break;
                    }
                    case "PLAYER_LIST": {
                        String[] names = ms.getData().split(",");
                        Platform.runLater(() -> {
                            gameTable.updatePlayerList(names, myName);
                        });
                        break;
                    }
                    
                    case "CHAT": {
                        String msg = ms.getData();
                        Platform.runLater(() -> gameTable.getChatArea().appendText(msg + "\n"));
                        break;
                    }
                    
                    case "NOTIFICATION":{
                    	System.out.println(ms.getData());
                    	break;
                    }
                    case "WIN":{
                    	 	String winnerName = ms.getData();
                    	    Platform.runLater(() -> {

                    	        // Nếu chính mình thắng
                    	        if (winnerName.equals(myName)) {
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
                    	            alert.setContentText("Người chơi " + winnerName + " đã thắng!");
                    	            alert.showAndWait();
                    	        }
                    	        gameTable.disableButton();
                    	        
                    	    });
                    	    break;
                    }
                    default: {
                        System.out.println("Unknown message type: " + ms.getAction());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
