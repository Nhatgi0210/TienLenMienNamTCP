package tienlen.logic;

import tienlen.model.Card;
import tienlen.model.Move;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GameLogic {

	 public static Move.ComboType identifyType(List<Card> cards) {
         if (cards.isEmpty()) return Move.ComboType.INVALID;

         
         Collections.sort(cards);;

         int size = cards.size();

         // 1. Đánh 1 lá
         if (size == 1) return Move.ComboType.SINGLE;

         // 2. Đôi
         if (size == 2) {
             if (cards.get(0).getRank() == cards.get(1).getRank()) return Move.ComboType.PAIR;
            
         }

         // 3. Bộ 3
         if (size == 3) {
        	 if (cards.get(0).getRank() == cards.get(1).getRank() && cards.get(1).getRank() == cards.get(2).getRank()) return Move.ComboType.TRIPLE;
         }

         // 4. Tứ quý
         if (size == 4) {
             boolean allSame = cards.stream().allMatch(c -> c.getRank() == cards.get(0).getRank());
             if (allSame) return Move.ComboType.FOUR_KIND;
         }
         
         if (size == 6) {
        	    boolean isThreePairs = true;
        	    for (int i = 0; i < 6; i += 2) {
        	        if (cards.get(i).getRank() != cards.get(i + 1).getRank()) {
        	            isThreePairs = false;
        	            break;
        	        }
        	        if (i > 0 && cards.get(i).getRank() != cards.get(i - 2).getRank() + 1) {
        	            isThreePairs = false;
        	            break;
        	        }
        	    }
        	    if (isThreePairs) return Move.ComboType.THREE_PAIRS;
        	}

        	// 4 đôi thông
        if (size == 8) {
        	    boolean isFourPairs = true;
        	    for (int i = 0; i < 8; i += 2) {
        	        if (cards.get(i).getRank() != cards.get(i + 1).getRank()) {
        	            isFourPairs = false;
        	            break;
        	        }
        	        if (i > 0 && cards.get(i).getRank() != cards.get(i - 2).getRank() + 1) {
        	            isFourPairs = false;
        	            break;
        	        }
        	    }
        	    if (isFourPairs) return Move.ComboType.FOUR_PAIRS;
        	}
        	// sảnh
         if (size >= 3) {
        	 for(int i = 1; i < cards.size(); i++){
        		 if (cards.get(i).getRank() == 15) return Move.ComboType.INVALID;
        		 if (cards.get(i).getRank() != cards.get(i-1).getRank()+1) return Move.ComboType.INVALID;
        	 }
        	 return Move.ComboType.STRAIGHT;
         }
          return Move.ComboType.INVALID;
    }

	 public static boolean canBeat(Move prev, Move next) {
	        if (prev == null) return true;
	        if (!next.isValid()) return false;

	        // Cùng loại và cùng độ dài
	        if (prev.getComboType() == next.getComboType() &&
	            prev.getLength() == next.getLength()) {
	            return next.getHighest().compareTo(prev.getHighest()) > 0;
	        }
	        // 2 bị chặt
	        if (prev.getComboType() == Move.ComboType.SINGLE && prev.getHighest().getRank() == 15) {
	            return next.getComboType() == Move.ComboType.FOUR_KIND
	                || next.getComboType() == Move.ComboType.THREE_PAIRS
	                || next.getComboType() == Move.ComboType.FOUR_PAIRS;
	        }
	        //Đôi 2 bị chặt 
	        if (prev.getComboType() == Move.ComboType.PAIR && prev.getHighest().getRank() == 15) {
	            return next.getComboType() == Move.ComboType.FOUR_KIND
	                || next.getComboType() == Move.ComboType.FOUR_PAIRS;
	        }
	        if (prev.getComboType() == Move.ComboType.FOUR_KIND && next.getComboType() == Move.ComboType.FOUR_PAIRS)
	            return true;

	        return false;
	    }
}
