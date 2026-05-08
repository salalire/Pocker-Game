package game;
import java.util.*;
public class Player {
    String name;
    public Player(String name ){
        this.name=name;
    }
    private List<Card> cardInPlayerHand=new ArrayList<>();
    public void receiveCards(Card card){
        cardInPlayerHand.add(card);
    }

    public void showCardInHand(){
        System.out.println("Cards in the player Hand: ");
        for (Card card :cardInPlayerHand){
            System.out.println(card +" ,");
        }
    }

}
