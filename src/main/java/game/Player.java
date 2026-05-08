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

    public Card playCard(int cardIndex){
        Card card= cardInPlayerHand.remove(cardIndex);
        return card;
    }
    public int getHandSize(){
        return cardInPlayerHand.size();
    }

    public Card playFirstValidCard(Card topCard){
        for (int i=0;i<cardInPlayerHand.size();i++){
            Card card=cardInPlayerHand.get(i);
            if (card.getSuit()==topCard.getSuit()||card.getOrder()==topCard.getOrder()){
                return cardInPlayerHand.remove(i);

            }

        }
     return null;
    }

}
