package tienlen.server;

import tienlen.model.Card;
import tienlen.model.Message;
import tienlen.model.Player;
import tienlen.utils.PasswordUtil;
import tienlen.utils.Protocol;

import java.awt.color.ICC_ColorSpace;
import java.io.*;
import java.net.Socket;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientHandler implements Runnable {
	private GameSession gameSession;
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;

    public ClientHandler(Socket socket, GameSession gameSession) {
        this.socket = socket;
        this.gameSession = gameSession;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void sendHand(List<Card> hand) {
        StringBuilder sb = new StringBuilder("HAND|");
        for (Card c : hand) {
            sb.append(c.toString()).append(",");
        }
        sendMessage(sb.toString());
    }

    public void sendMessage(String msg) {
    	 out.println(msg);
    	 out.flush();
    	 if (out.checkError()) {
    	        System.out.println("Error sending message to client!");
    	    }
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
            	Message ms = Protocol.decode(line);
            	switch (ms.getAction()) {
            	case "LOGIN":{
            		String data = ms.getData();
            		Map<String, String> map = new HashMap<>();

            		for (String pair : data.split("&")) {
            		    String[] kv = pair.split("=", 2);
            		    map.put(kv[0], kv[1]);
            		}

            		String username = map.get("user");
            		String password = map.get("pass");
            		
            		break;
            	}
				case "JOIN": {
					player = new Player(ms.getData());
					gameSession.addPlayer(player, this);
					break;
				}
				case "PLAY":{
					String moveData = ms.getData();
                    gameSession.processMove(this, moveData);
					break;
				}
				case "PASS":{
					gameSession.processMove(this, "PASS");
					break;
				}
				case "NEWGAME":{
					 if (!gameSession.isGameRunning()) {
					        gameSession.startGame();
					    } else {
					        Message msg = new Message("CHAT", "Ván đang diễn ra, không thể bắt đầu mới!");
					        sendMessage(Protocol.encode(msg));
					    }
					
					break;
				}
				case "CHAT":
				    String chatMsg = ms.getData(); 
				    gameSession.chat(chatMsg);
				    break;

				default:
					throw new IllegalArgumentException("Unexpected value: " + ms.getAction());
				}
            	
            }
        } catch (IOException e) {
            System.out.println("⚠ Kết nối không thành công với " + player);
        } finally {
            // Luôn remove khi client thoát
            if (player != null) {
                gameSession.removePlayer(player);
                gameSession.broadcastPlayerList();
                if (gameSession.getPlayers().size() < 2) {
                	gameSession.endGame("END");
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
