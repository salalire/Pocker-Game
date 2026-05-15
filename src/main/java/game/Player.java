package game;
import java.util.*;
public class Player {
    private String name;
    private Suit forcedSuit=null;
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


    public List<Card> getHand(){
        return cardInPlayerHand;
    }



    public Card playCard(int cardIndex){
        Card card= cardInPlayerHand.remove(cardIndex);
        return card;
    }
    public int getHandSize(){
        return cardInPlayerHand.size();
    }

//    public Card playFirstValidCard(Card topCard){
//        for (int i=0;i<cardInPlayerHand.size();i++){
//            Card card=cardInPlayerHand.get(i);
//            if (forcedSuit != null){
//                if (card.getSuit() == forcedSuit ||
//                        card.getOrder() == topCard.getOrder()){
//                    return card;
//                }
//            } else {
//                if (card.getSuit() == topCard.getSuit() ||
//                        card.getOrder() == topCard.getOrder()){
//                    return card;
//                }
//            }
//
//        }
//     return null;
//    }

    public Card getDefence(Order order, Suit suit){
        for (Card defence:cardInPlayerHand){
            if (defence.getSuit()==suit && defence.getOrder()== order){
                return defence;
            }
        }
       return null;
    }


    public Card getDefence(Order order){
        for (Card c : cardInPlayerHand){
            if (c.getOrder() == order){
                return c;
            }
        }
        return null;
    }

    public void dropCard(Card dropped){
        cardInPlayerHand.remove(dropped);
    }

}



