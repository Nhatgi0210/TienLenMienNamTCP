package tienlen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Player {
    private final UUID id;
    private String name;
    private final List<Card> hand = new ArrayList<>();
    private long balance; // Số dư tiền của người chơi (VND)
    private long betAmount = 0; // Số tiền cược trong ván hiện tại

    public Player(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0;
    }

    public Player(String name, long balance) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = balance;
    }

    private boolean isPlaying;
    

    public boolean isPlaying() {
		return isPlaying;
	}


	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public void addBalance(long amount) {
        this.balance += amount;
    }

    public void subtractBalance(long amount) {
        this.balance -= amount;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long amount) {
        this.betAmount = amount;
    }

    public void resetBet() {
        this.betAmount = 0;
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

