package tienlen.server;

import tienlen.model.Card;
import tienlen.model.Message;
import tienlen.model.Player;
import tienlen.utils.PasswordUtil;
import tienlen.utils.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
	private GameSession gameSession;
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;
    private ServerMain server;
    private ServerBackend backend;
    private String currentUsername;

    // Constructor cũ để hỗ trợ GameSession được truyền trực tiếp
    public ClientHandler(Socket socket, GameSession gameSession) {
        this.socket = socket;
        this.gameSession = gameSession;
        this.server = null;
        this.backend = null;
    }

    // Constructor mới cho ServerMain
    public ClientHandler(Socket socket, ServerMain server) {
        this.socket = socket;
        this.server = server;
        this.gameSession = null;
        this.backend = null;
    }

    // Constructor cho ServerBackend
    public ClientHandler(Socket socket, ServerBackend backend) {
        this.socket = socket;
        this.backend = backend;
        this.server = null;
        this.gameSession = null;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void closeConnection() {
        try {
            if (currentUsername != null && backend != null) {
                backend.unregisterPlayer(currentUsername);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void sendHand(List<Card> hand) {
        StringBuilder sb = new StringBuilder("HAND|");
        for (Card c : hand) {
            sb.append(c.toString()).append(",");
        }
        sendMessage(sb.toString());
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
            	Message ms = Protocol.decode(line);
            	System.out.println("Received: " + ms.getAction() + " | " + ms.getData());
            	
            	switch (ms.getAction()) {
            	case Protocol.REGISTER: {
            		handleRegister(ms.getData());
            		break;
            	}
            	case Protocol.LOGIN:{
            		handleLogin(ms.getData());
            		break;
            	}
            	case Protocol.GET_SESSIONS: {
            		handleGetSessions();
            		break;
            	}
            	case Protocol.SELECT_SESSION: {
            		String sessionId = ms.getData();
            		handleSelectSession(sessionId);
            		break;
            	}
            	case Protocol.GET_SESSION_DETAILS: {
            		String sessionId = ms.getData();
            		handleGetSessionDetails(sessionId);
            		break;
            	}
            	case "CREATE_SESSION": {
                    // Có thể gửi data: name=...&bet=...
                    String data = ms.getData();
                    String name = "";
                    long bet = 10000;
                    if (data != null && !data.isEmpty()) {
                        var map = parseData(data);
                        if (map.containsKey("name")) name = map.get("name");
                        if (map.containsKey("bet")) {
                            try { bet = Long.parseLong(map.get("bet")); } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (server != null) {
                        server.createNewSession(name, bet);
                    } else if (backend != null) {
                        backend.createNewSession(name, bet);
                    }
                    handleGetSessions();
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
				    if (gameSession != null) {
				    	gameSession.chat(chatMsg);
				    }
				    break;
                    case "CHAT_VOICE": {
                        String voiceData = ms.getData();
                        if (gameSession != null) {
                            gameSession.chatVoice(voiceData, this);
                        }
                        break;
                    }
                case "LEAVE_SESSION": {
                    if (gameSession != null && player != null) {
                        gameSession.removePlayer(player);
                        gameSession.broadcastPlayerList();
                        if (backend != null && currentUsername != null) backend.unregisterPlayerFromSession(currentUsername);
                        this.gameSession = null;
                        // send updated sessions list to client
                        handleGetSessions();
                    }
                    break;
                }

				default:
					System.out.println("Unknown action: " + ms.getAction());
				}
            	
            }
        } catch (IOException e) {
            System.out.println("⚠ Kết nối không thành công với " + currentUsername + ": " + e.getMessage());
        } finally {
            // Xóa player session khỏi database
            if (currentUsername != null) {
                UserManager.getInstance().deletePlayerSession(currentUsername);
            }
            
            // Luôn remove khi client thoát
            if (gameSession != null && player != null) {
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

    private void handleGetSessions() {
        // Gửi balance cho client trước tiên
        UserManager userManager = UserManager.getInstance();
        long balance = userManager.getBalance(currentUsername);
        sendMessage(Protocol.encode(new Message(Protocol.BALANCE, String.valueOf(balance))));
        
        String sessionInfo = "";
        
        if (server != null) {
            sessionInfo = server.getSessionsInfo();
        } else if (backend != null) {
            // Xây dựng danh sách session từ backend
            StringBuilder sb = new StringBuilder();
            var sessions = backend.getAllSessions();
            for (int i = 0; i < sessions.size(); i++) {
                var sessionInfo_ = sessions.get(i);
                if (i > 0) sb.append(";");
                                        sb.append(sessionInfo_.getSessionId()).append(",")
                                            .append(sessionInfo_.getPlayerCount()).append(",")
                                            .append(sessionInfo_.getStatus()).append(",")
                                            .append(sessionInfo_.getDisplayName()).append(",")
                                            .append(sessionInfo_.getBetAmount());
            }
            sessionInfo = sb.toString();
        } else {
            sendMessage("ERROR|Server not initialized");
            return;
        }

        Message response = new Message(Protocol.SESSION_LIST, sessionInfo);
        sendMessage(Protocol.encode(response));
        System.out.println("Sent SESSION_LIST: " + sessionInfo);
    }

    private void handleSelectSession(String sessionId) {
        GameSession session = null;
        boolean isAvailable = false;
        
        if (server != null) {
            isAvailable = server.isSessionAvailable(sessionId);
            session = server.findSessionById(sessionId);
        } else if (backend != null) {
            session = backend.findSessionById(sessionId);
            if (session != null) {
                isAvailable = (session.getPlayers().size() < 4 && !session.isGameRunning());
            }
        } else {
            sendMessage(Protocol.encode(new Message("ERROR", "Server not initialized")));
            return;
        }

        if (!isAvailable) {
            sendMessage(Protocol.encode(new Message("ERROR", "Table not available")));
            return;
        }

        if (session == null) {
            sendMessage(Protocol.encode(new Message("ERROR", "Session not found")));
            return;
        }

        this.gameSession = session;
        
        // Tự động JOIN vào session đã chọn
        player = new Player(currentUsername);
        gameSession.addPlayer(player, this);
        
        // Cập nhật trạng thái player trong backend nếu có
        if (backend != null) {
            backend.registerPlayerInSession(currentUsername, sessionId);
        }
        
        Message confirmMsg = new Message("SESSION_SELECTED", sessionId);
        sendMessage(Protocol.encode(confirmMsg));
        
        // Gửi lại PLAYER_LIST sau một chút delay để đảm bảo client đã sẵn sàng nhận
        new Thread(() -> {
            try {
                Thread.sleep(200);  // Delay 200ms
                gameSession.broadcastPlayerList();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        System.out.println("Player " + currentUsername + " joined session " + sessionId);
    }

    private void handleRegister(String data) {
        Map<String, String> map = parseData(data);
        String username = map.get("user");
        String password = map.get("pass");

        if (username == null || password == null || 
            username.trim().isEmpty() || password.trim().isEmpty()) {
            sendMessage(Protocol.encode(new Message(Protocol.REGISTER_FAILED, "Vui lòng nhập đầy đủ thông tin")));
            return;
        }

        UserManager userManager = UserManager.getInstance();
        
        // Kiểm tra username đã tồn tại
        if (userManager.userExists(username)) {
            sendMessage(Protocol.encode(new Message(Protocol.REGISTER_FAILED, "Tên đăng nhập đã tồn tại")));
            return;
        }

        // Đăng ký
        if (userManager.registerUser(username, password)) {
            sendMessage(Protocol.encode(new Message(Protocol.REGISTER_SUCCESS, "Đăng ký thành công")));
        } else {
            sendMessage(Protocol.encode(new Message(Protocol.REGISTER_FAILED, "Đăng ký thất bại")));
        }
    }

    private void handleLogin(String data) {
        Map<String, String> map = parseData(data);
        String username = map.get("user");
        String password = map.get("pass");

        if (username == null || password == null || 
            username.trim().isEmpty() || password.trim().isEmpty()) {
            sendMessage(Protocol.encode(new Message(Protocol.LOGIN_FAILED, "Vui lòng nhập đầy đủ thông tin")));
            return;
        }

        UserManager userManager = UserManager.getInstance();
        
        // Đăng nhập
        if (userManager.loginUser(username, password)) {
            this.currentUsername = username;
            
            // Tạo player session trong database
            String clientIp = socket.getInetAddress().getHostAddress();
            userManager.createPlayerSession(username, clientIp);
            
            // Đăng ký player trong backend nếu có
            if (backend != null) {
                backend.registerPlayer(username, clientIp, this);
            }
            
            sendMessage(Protocol.encode(new Message(Protocol.LOGIN_SUCCESS, username)));
            
            // Gửi balance cho client
            long balance = userManager.getBalance(username);
            sendMessage(Protocol.encode(new Message(Protocol.BALANCE, String.valueOf(balance))));
            
            // Gửi danh sách bàn cho client
            handleGetSessions();
        } else {
            sendMessage(Protocol.encode(new Message(Protocol.LOGIN_FAILED, "Tên đăng nhập hoặc mật khẩu không đúng")));
        }
    }

    private Map<String, String> parseData(String data) {
        Map<String, String> map = new HashMap<>();
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
    
    private void handleGetSessionDetails(String sessionId) {
        GameSession session = null;
        
        if (server != null) {
            session = server.findSessionById(sessionId);
        } else if (backend != null) {
            session = backend.findSessionById(sessionId);
        }
        
        if (session == null) {
            sendMessage(Protocol.encode(new Message("ERROR", "Session not found")));
            return;
        }
        
        // Xây dựng dữ liệu chi tiết session: playerName1,balance1;playerName2,balance2;...
        StringBuilder sb = new StringBuilder();
        List<Player> players = session.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(";");
            Player p = players.get(i);
            sb.append(p.getName()).append(",").append(p.getBalance());
        }
        
        Message response = new Message(Protocol.SESSION_DETAILS, sb.toString());
        sendMessage(Protocol.encode(response));
    }
}
