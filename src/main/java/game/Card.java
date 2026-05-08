package game;

public class Card {
    private Suit suit;
    private Order order;
    public Card(Suit suit,Order order){
        this.suit=suit;
        this.order=order;
    }
    public String toString(){
        return order+" of "+suit;
    }
    public Suit getSuit(){
        return suit;
    }
    public Order getOrder(){
        return order;
    }
}
