package tienlen.server;

import tienlen.logic.GameLogic;
import tienlen.model.Card;
import tienlen.model.Deck;
import tienlen.model.Message;
import tienlen.model.Move;
import tienlen.model.Player;
import tienlen.utils.Protocol;

import java.util.*;

public class GameSession {
    private final String sessionId;
    private String displayName = "";
    private final List<Player> players = new ArrayList<>();
    private int currentTurnIndex = 0;
    private Move lastMove = null;
    private final int MAX_PLAYERS = 4;
    private final Set<Player> passedPlayers = new HashSet<>(); // Người đã PASS vòng này
    private Map<Player, ClientHandler> connections = new HashMap<>();
    private int winnerIndex = -1;
    private long betAmount = 10000; // Số tiền cược mỗi ván (10k VND)
    private long totalPot = 0; // Tổng tiền trong pot
    
    private boolean gameRunning = false; 

    public synchronized boolean isGameRunning() {
        return gameRunning;
    }

    private synchronized void setGameRunning(boolean running) {
        gameRunning = running;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long amount) {
        this.betAmount = amount;
    }

    public long getTotalPot() {
        return totalPot;
    }

    public synchronized void addToPot(long amount) {
        this.totalPot += amount;
    }

    public synchronized void resetPot() {
        this.totalPot = 0;
    }

    public synchronized void addPlayer(Player player,ClientHandler handler) {
        players.add(player);
        connections.put(player, handler);
        
        // Load balance từ database khi player join
        UserManager userManager = UserManager.getInstance();
        long balance = userManager.getBalance(player.getName());
        player.setBalance(balance);
        
        broadcastPlayerList();
        
    }
    public void broadcastPlayerList() {
        List<String> playerInfos = new ArrayList<>();
        for (Player p : players) {
            playerInfos.add(p.getName() + ":" + p.getBalance());
        }
        Message msg = new Message("PLAYER_LIST", String.join(",", playerInfos));
        for (Player p : players) {
            connections.get(p).sendMessage(Protocol.encode(msg));
        }
    }

    public GameSession() {
        this.sessionId = UUID.randomUUID().toString();
        // Save session to database
        UserManager userManager = UserManager.getInstance();
        userManager.createGameSession(this.sessionId, this.displayName, this.betAmount);
    }

    // Constructor with custom display name and bet amount
    public GameSession(String displayName, long betAmount) {
        this.sessionId = UUID.randomUUID().toString();
        this.displayName = displayName;
        this.betAmount = betAmount;
        // Save session to database
        UserManager userManager = UserManager.getInstance();
        userManager.createGameSession(this.sessionId, this.displayName, this.betAmount);
    }


    public GameSession(String displayName) {
        this.sessionId = UUID.randomUUID().toString();
        this.displayName = displayName;
        // Save session to database
        UserManager userManager = UserManager.getInstance();
        userManager.createGameSession(this.sessionId, this.displayName, this.betAmount);
    }

    // Constructor để load session từ database (không save lại)
    public GameSession(String sessionId, String displayName, long betAmount) {
        this.sessionId = sessionId;
        this.displayName = displayName;
        this.betAmount = betAmount;
        // Session đã tồn tại trong database, không cần save lại
    }

    public String getDisplayName() {
        return displayName == null || displayName.isEmpty() ? ("Bàn " + sessionId.substring(0, 6)) : displayName;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public String getSessionId() {
        return sessionId;
    }

    
    public Move getLastMove() {
		return lastMove;
	}

	public void setLastMove(Move lastMove) {
		this.lastMove = lastMove;
	}

	public int getCurrentTurnIndex() {
		return currentTurnIndex;
	}

	public Set<Player> getPassedPlayers() {
		return passedPlayers;
	}

    public List<Player> getPlayers() {
        return players;
    }

    public void startGame() {
    	if (gameRunning) return;
      	if (players.size() < 2) return;
      	gameRunning = true;
      	
      	// Update session status to PLAYING in database
      	UserManager userManager = UserManager.getInstance();
      	userManager.updateSessionStatus(sessionId, "PLAYING");
      	
    	lastMove = null;
    	for (Player p : players) {
            p.getHand().clear();
            p.setPlaying(true);
        }
        Deck deck = new Deck();
        deck.shuffle();

        List<List<Card>> hands = deck.deal(players.size());
        for (int i = 0; i < players.size(); i++) {
            players.get(i).addCards(hands.get(i));
        }
        //gửi bài cho từng người chơi
        for (Player player : players) {
        	connections.get(player).sendHand(player.getHand());
           
        }
        if (winnerIndex == -1)
        for (int i = 0; i < players.size(); i++) {
            for (Card c : players.get(i).getHand()) {
                if (c.getRank() == 3 && c.getSuit() == Card.Suit.SPADES) {
                    currentTurnIndex = i;
                    break;
                }
            }
        }
        else currentTurnIndex = winnerIndex;
        for (Player player : players) {
        	Message ms = new Message("TURN", players.get(currentTurnIndex).getName());
        	connections.get(player).sendMessage(Protocol.encode(ms));
           
        }
        
    }
  

    public Player getCurrentPlayer() {
        return players.get(currentTurnIndex);
    }

    public void nextTurn() {
    	  do {
              currentTurnIndex = (currentTurnIndex + 1) % players.size();
          } while (passedPlayers.contains(players.get(currentTurnIndex)) || !players.get(currentTurnIndex).isPlaying());
    }

    public void handlePass(Player player) {
        passedPlayers.add(player);
        nextTurn();
        // Nếu tất cả (trừ 1) đều PASS → reset vòng
        if (passedPlayers.size() == players.size() - 1) {
        	passedPlayers.clear();
        	lastMove = null;
        	broadcastMove(player, null);
        } 
        else broadcastTurn();
    }
    public void broadcastMove(Player player, Move move) {
        Message msplay;
        if (move == null) msplay = new Message("PASS", "");
        else msplay = new Message("PLAY", player.getName() +"," + move.toString());
        
        for (Player p : players) {
        		connections.get(p).sendMessage(Protocol.encode(msplay));
        }
        broadcastTurn();
    }
    public void broadcastTurn() {
    	Message msturn = new Message("TURN", players.get(currentTurnIndex).getName());
        for (Player p : players) {
            connections.get(p).sendMessage(Protocol.encode(msturn));
        }
    }
    public synchronized void processMove(ClientHandler sender, String moveData) {
    	Player player = sender.getPlayer();

        if (!getCurrentPlayer().equals(player)) {
        	Message ms = new Message("NOTIFICATION", "ERROR:Not your turn!");
            sender.sendMessage(Protocol.encode(ms));
            return;
        }

        if (moveData.equals("PASS")) {
        	handlePass(player);
            System.out.println(player.getName() + " passed.");
            return;
        }

        // Chuyển từ string sang Move
        Move move = parseMove(moveData);

        // Check hợp lệ
        if (!move.isValid()) {
        	Message ms = new Message("NOTIFICATION", "ERROR:Invalid move!");
            sender.sendMessage(Protocol.encode(ms));
            return;
        }

        if (!GameLogic.canBeat(getLastMove(), move)) {
        	Message ms = new Message("NOTIFICATION", "ERROR:Your move is too weak!");
            sender.sendMessage(Protocol.encode(ms));
            return;
        }
        
        // Nếu ok → remove bài trong tay người chơi
        player.removeCards(move.getCards());
        System.out.println(player.getName()+"play: "+moveData);
        setLastMove(move); 
        System.out.println(player.getName() +": " + player.getHand());
        if (player.hasNoCards()) {
        	endGame(player.getName());
        	winnerIndex = players.indexOf(player);
            return;
        }

        nextTurn();
        broadcastMove(player, move);
    }
    private Move parseMove(String moveData) {
        String[] parts = moveData.split(",");
        List<Card> cards = new ArrayList<>();

        for (String part : parts) {
            if (part.isEmpty()) continue;

            String rankStr = part.substring(0, part.length() - 1);
            String suitStr = part.substring(part.length() - 1);

            // Xử lý chất (suit)
            Card.Suit suit;
            switch (suitStr) {
                case "♠": suit = Card.Suit.SPADES; break;
                case "♥": suit = Card.Suit.HEARTS; break;
                case "♦": suit = Card.Suit.DIAMONDS; break;
                case "♣": suit = Card.Suit.CLUBS; break;
                default: throw new IllegalArgumentException("Invalid suit: " + suitStr);
            }

            // Xử lý giá trị (rank)
            int rank;
            switch (rankStr.toUpperCase()) {
                case "A": rank = 14; break;
                case "K": rank = 13; break;
                case "Q": rank = 12; break;
                case "J": rank = 11; break;
                default:
                    try {
                        rank = Integer.parseInt(rankStr); 
                        if(rank == 2) rank = 15;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid rank: " + rankStr);
                    }
            }

            cards.add(new Card(rank, suit));
        }

        return new Move(cards);
    }


//    public boolean isGameOver() {
//        for (Player p : players) {
//            if (p.hasNoCards()) {
//                return true;
//            }
//        }
//        return false;
//    }

    public Player getWinner() {
        for (Player p : players) {
            if (p.hasNoCards()) {
                return p;
            }
        }
        return null;
    }
    public void chat(String msg) {
    	for (Player p : players) {
	        connections.get(p).sendMessage(Protocol.encode(new Message("CHAT", msg)));
	    }
    }

    public void chatVoice(String data, ClientHandler sender) {
        // data expected: senderName|base64AudioWav
        // Send to all players except the sender
        for (Player p : players) {
            if (connections.get(p) != sender) {
                connections.get(p).sendMessage(Protocol.encode(new Message(Protocol.CHAT_VOICE, data)));
            }
        }
    }
    
    public synchronized void removePlayer(Player player) {
        players.remove(player);
        connections.remove(player);

       
        Message msg = new Message("NOTIFICATION", player.getName() + " đã thoát.");
        for (Player p : players) {
            connections.get(p).sendMessage(Protocol.encode(msg));
        }
    }
    public void endGame(String winnerName) {
        // Xử lý tiền khi ván kết thúc
        if (!winnerName.equals("END")) {
            // Tìm người thắng
            Player winner = null;
            for (Player p : players) {
                if (p.getName().equals(winnerName)) {
                    winner = p;
                    break;
                }
            }

            if (winner != null) {
                // Tính tiền: Người thắng nhận tất cả tiền từ người thua
                long winningSAmount = betAmount * (players.size() - 1);
                
                // Cộng tiền cho người thắng
                winner.addBalance(winningSAmount);
                
                // Trừ tiền từ những người thua
                UserManager userManager = UserManager.getInstance();
                for (Player p : players) {
                    if (!p.getName().equals(winnerName)) {
                        p.subtractBalance(betAmount);
                        userManager.subtractBalance(p.getName(), betAmount);
                        // Lưu kết quả người thua vào database
                        userManager.saveGameResult(sessionId, p.getName(), "LOSE", betAmount, 0);
                    }
                }
                // Cộng tiền cho người thắng trong database
                userManager.addBalance(winnerName, winningSAmount);
                // Lưu kết quả người thắng vào database
                userManager.saveGameResult(sessionId, winnerName, "WIN", betAmount, winningSAmount);
                
                // Update game session status in database
                userManager.endGameSession(sessionId, winnerName, totalPot);
                
                System.out.println("🏆 " + winnerName + " won! Earned: " + winningSAmount + " VND");
            }
        }

        // Gửi thông báo kết thúc ván kèm theo balances cập nhật
        StringBuilder balanceData = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            balanceData.append(players.get(i).getName()).append(":").append(players.get(i).getBalance());
            if (i < players.size() - 1) balanceData.append(",");
        }
        Message ms = new Message("WIN", winnerName + "|" + balanceData.toString());
        for (Player p : players) {
            p.setPlaying(false);
            connections.get(p).sendMessage(Protocol.encode(ms));
        }
        
        // Broadcast updated player list with new balances
        broadcastPlayerList();
        
        setGameRunning(false);
        lastMove = null;
        passedPlayers.clear();
        resetPot();
    }
}
