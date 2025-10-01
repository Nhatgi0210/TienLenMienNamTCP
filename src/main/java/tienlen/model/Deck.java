package tienlen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Card.Suit s : Card.Suit.values()) {
            for (int r = 3; r <= 15; r++) {
                cards.add(new Card(r, s));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public List<List<Card>> deal(int players) {
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            hands.add(new ArrayList<>());
        }

        if (players == 4) {
            // Chia hết 52 lá cho 4 người
            int idx = 0;
            for (Card c : cards) {
                hands.get(idx % players).add(c);
                idx++;
            }
        } else {
            // Chỉ chia 13 lá cho mỗi người
            int totalCards = players * 13;
            for (int i = 0; i < totalCards; i++) {
                hands.get(i % players).add(cards.get(i));
            }
        }

        // Sắp xếp bài từng người
        for (List<Card> h : hands) {
            Collections.sort(h);
        }

        return hands;
    }

}
