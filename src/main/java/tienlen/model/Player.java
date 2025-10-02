package tienlen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Player {
    private final UUID id;
    private String name;
    private final List<Card> hand = new ArrayList<>();

    public Player(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
    }
    private boolean isPlaying;
    

    public boolean isPlaying() {
		return isPlaying;
	}


	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}


	public List<Card> getHand() {
        return hand;
    }
    

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addCards(List<Card> cards) {
        hand.addAll(cards);
        sortHand();
    }

    public void removeCards(List<Card> cards) {
        hand.removeAll(cards);
    }

    public void sortHand() {
        Collections.sort(hand);
    }

    public boolean hasNoCards() {
        return hand.isEmpty();
    }
}

