package tienlen.model;

public class Message {
    private String action; // PLAY, PASS, CHAT, JOIN, EXIT...
    private String data;   // JSON hoặc String chứa dữ liệu

    public Message(String action, String data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "action='" + action + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
