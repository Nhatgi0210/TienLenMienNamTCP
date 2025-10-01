import tienlen.model.Message;
import tienlen.utils.Protocol;

public class TestProtocol {
    public static void main(String[] args) {
        Message msg = new Message("PLAY", "[3♠, 3♦, 3♣]");
        String encoded = Protocol.encode(msg);
        System.out.println("Encoded: " + encoded);

        Message decoded = Protocol.decode(encoded);
        System.out.println("Decoded: " + decoded);
    }
}
