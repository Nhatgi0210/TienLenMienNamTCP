package tienlen.utils;

import tienlen.model.Message;

public class Protocol {

   
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
