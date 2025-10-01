import java.util.List;

import tienlen.model.Card;
import tienlen.model.Deck;

public class Test {
    public static void main(String[] args) {
        Deck deck = new Deck();
        deck.shuffle();
        List<List<Card>> hands = deck.deal(4);
        for (int i = 0; i < hands.size(); i++) {
            System.out.println("Player " + (i+1) + ": " + hands.get(i));
        }
    }
}

