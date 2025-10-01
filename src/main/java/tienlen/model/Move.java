package tienlen.model;

import java.util.*;
import tienlen.logic.*;
/**
 * Đại diện cho một nước đi trong Tiến Lên.
 */
public class Move {
    private final List<Card> cards;
    private final ComboType comboType;
    private final int length;     // số lá
    private final Card highest;  // lá mạnh nhất trong bộ

    public enum ComboType {
        SINGLE, PAIR, TRIPLE, FOUR_KIND, STRAIGHT, INVALID
    }

    public Move(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
        this.cards.sort(Comparator.naturalOrder());

        // Xác định combo type và high card
        this.comboType =  GameLogic.identifyType(this.cards);
        this.length = this.cards.size();
        this.highest = this.cards.isEmpty() ? null : this.cards.get(this.cards.size() - 1);
    }

    public List<Card> getCards() {
        return cards;
    }

    public ComboType getComboType() {
        return comboType;
    }

    public int getLength() {
        return length;
    }

    public Card getHighest() {
        return highest;
    }

    public boolean isValid() {
        return comboType != ComboType.INVALID;
    }

    @Override
    public String toString() {
    	if (cards.isEmpty()) return "";
        String string = cards.get(0).toString();
        for(int i = 1; i < cards.size();i++) {
        	string = string + "," + cards.get(i).toString();
        }
        return string;
    }
}
