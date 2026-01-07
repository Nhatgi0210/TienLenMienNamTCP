package tienlen.utils;

import tienlen.model.Message;

public class Protocol {
    // Authentication
    public static final String REGISTER = "REGISTER";
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String REGISTER_FAILED = "REGISTER_FAILED";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    
    // Server Commands
    public static final String GET_SESSIONS = "GET_SESSIONS";
    public static final String SESSION_LIST = "SESSION_LIST";
    public static final String SELECT_SESSION = "SELECT_SESSION";
    public static final String SESSION_INFO = "SESSION_INFO";
    
    // Game Commands
    public static final String JOIN = "JOIN";
    public static final String PLAY = "PLAY";
    public static final String PASS = "PASS";
    public static final String HAND = "HAND";
    public static final String PLAYER_LIST = "PLAYER_LIST";
    public static final String GAME_START = "GAME_START";
    public static final String NEWGAME = "NEWGAME";
    public static final String CHAT = "CHAT";
    public static final String CHAT_VOICE = "CHAT_VOICE";
    public static final String SESSION_CLOSED = "SESSION_CLOSED";
    
    // Money/Betting Commands
    public static final String BET = "BET";
    public static final String BET_RESULT = "BET_RESULT"; // Kết quả cược (thắng/thua/hòa)
    public static final String BALANCE = "BALANCE"; // Gửi số dư tiền hiện tại
    public static final String GET_BALANCE = "GET_BALANCE"; // Yêu cầu lấy số dư tiền
    public static final String PLAYER_INFO = "PLAYER_INFO"; // Gửi thông tin player (tên, tiền, ...)
    public static final String GAME_RESULT = "GAME_RESULT"; // Kết quả ván (người thắng, tiền thắng, ...)
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE"; // Không đủ tiền để cược
    
    // Statistics Commands
    public static final String GET_PLAYER_STATS = "GET_PLAYER_STATS"; // Yêu cầu thống kê người chơi
    public static final String PLAYER_STATS = "PLAYER_STATS"; // Trả về thống kê
    
    // Session Detail
    public static final String GET_SESSION_DETAILS = "GET_SESSION_DETAILS"; // Lấy thông tin chi tiết session
    public static final String SESSION_DETAILS = "SESSION_DETAILS"; // Trả về thông tin chi tiết session
    public static String encode(Message msg) {
        return msg.getAction() + "|" + (msg.getData() == null ? "" : msg.getData());
    }

    public static Message decode(String raw) {
        String[] parts = raw.split("\\|", 2); 
        String action = parts[0];
        String data = parts.length > 1 ? parts[1] : "";
        return new Message(action, data);
    }
}