package game;
import java.util.*;

public class Deck {
    List<Card> cards=new ArrayList<>();
    public Deck(){

        for(Suit suit: Suit.values()){
            for(Order order :Order.values()){
            cards.add(new Card(suit,order));
            }
        }
    }
    public void shuffle(){
        Collections.shuffle(cards);
    }

    public Card dealCard(){
        return cards.remove(cards.size()-1);
    }

}
