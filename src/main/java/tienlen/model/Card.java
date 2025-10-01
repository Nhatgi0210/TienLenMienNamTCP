package tienlen.model;

public class Card implements Comparable<Card> {

    public enum Suit {
    	   SPADES,CLUBS,DIAMONDS,HEARTS
    }

    private final int rank;
    private final Suit suit; 

    public Card(int rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }
    public Card(String card) {
    	String rankStr = card.substring(0, card.length() - 1);
        String suitStr = card.substring(card.length() - 1);
        switch (suitStr.toUpperCase()) {
            case "♠": suit = Card.Suit.SPADES; break;
            case "♥": suit = Card.Suit.HEARTS; break;
            case "♦": suit = Card.Suit.DIAMONDS; break;
            case "♣": suit = Card.Suit.CLUBS; break;
            default: throw new IllegalArgumentException("Invalid suit: " + suitStr);
        }
       
        switch (rankStr.toUpperCase()) {
       
        case "J":  this.rank = 11; break;
        case "Q":  this.rank = 12; break;
        case "K":  this.rank = 13; break;
        case "A":  this.rank = 14; break; 
        default:
            try {
                int temp = Integer.parseInt(rankStr); // 2–10
                if (temp == 2) this.rank = 15;
                else this.rank = temp;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid rank: " + rankStr);
            }
        }
    }
    public int getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }

    @Override
    public String toString() {
        String r;
        switch (rank) {
            case 11: r = "J"; break;
            case 12: r = "Q"; break;
            case 13: r = "K"; break;
            case 14: r = "A"; break;
            case 15: r = "2"; break;
            default: r = String.valueOf(rank);
        }

        String s;
        switch (suit) {
            case CLUBS:    s = "♣"; break;
            case DIAMONDS: s = "♦"; break;
            case HEARTS:   s = "♥"; break;
            default:       s = "♠";
        }

        return r + s;
    }

    @Override
    public int compareTo(Card o) {
        if (this.rank != o.rank) {
            return Integer.compare(this.rank, o.rank);
        }
        return Integer.compare(this.suit.ordinal(), o.suit.ordinal());
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return this.rank == card.rank && this.suit == card.suit;
    }

}
