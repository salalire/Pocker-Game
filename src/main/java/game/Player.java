package game;
import java.util.*;
public class Player {
    private String name;
    public Player(String name ){
        this.name=name;
    }
    public String getName(){
        return name;
    }
    private List<Card> cardInPlayerHand=new ArrayList<>();
    public void receiveCards(Card card){
        cardInPlayerHand.add(card);
    }

    public void showCardInHand(){
        for (Card card :cardInPlayerHand){
            System.out.println(card +" ,");
        }
    }

}
