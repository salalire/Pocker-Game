package game;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private List<Player> players;
    private List<Card> discardPile=new ArrayList<>();
   private Deck deck;
   private Card topCard;
   private boolean reversed=false;
   private int currentPlayerIndex=1;
   private boolean skipNext=false;
   private boolean activeAce=false;
   private int pendingPenality=0;
   private boolean activeTwo=false;
   private int pendingTwo=0;
   private boolean changePlay=false;
   private Suit forcedSuit=null;


    public Game(){
        deck=new Deck();
        players=new ArrayList<>();
    }

    public void addPlayers(String name){
        players.add(new Player(name));

    }


    public Card getTopCard(){
        return topCard;
    }

    public List<Card> getCard(){
        return getCurrentPlayer().getHand();
    }

    public Player getCurrentPlayer(){
        return players.get(currentPlayerIndex);
    }


    public void drawForCurrentPlayer(){
        Card drawn = drawCard();

        if (drawn != null){
            getCurrentPlayer().receiveCards(drawn);
        }
    }



    private Card drawCard() {
        if (deck.isEmpty()) {
            reshuffleDeck();
        }

        if (deck.isEmpty()) {
            return null; // no cards left even after reshuffle
        }

        return deck.dealCard();
    }



    private void reshuffleDeck() {
        System.out.println("Reshuffling discard pile...");

        if (discardPile.size() <= 1) {
            System.out.println("Not enough cards to reshuffle. Game ends.");
            return;
        }

        // Keep top card
        Card top = discardPile.remove(discardPile.size() - 1);

        // Move rest to deck
        deck.cards.addAll(discardPile);
        deck.shuffle();

        // Reset discard pile
        discardPile.clear();
        discardPile.add(top);
    }


    public void dealCards(){
        for(Player player :players){
            for (int i=0;i<6;i++){
                player.receiveCards(drawCard());
            }

        }
        players.get(0).receiveCards(drawCard());

    }

    public void firstMove(){
        Player firstPlayer= players.get(0);
        Card firstCard=firstPlayer.playCard(0);
        topCard=firstCard;
        discardPile.add(topCard);
        System.out.println("The First Player Is "+ firstPlayer.getName()+" Starts with "+topCard);
    }


    public void showPlayerCards(){
        for (Player player:players){
            System.out.println(player.getName() +" Cards");
            player.showCardInHand();
            System.out.println();
        }
    }

    public void playGame() {
        while (true) {
            Player currentPlayer = players.get(currentPlayerIndex);
            System.out.println("Current Card: " + topCard);
            System.out.println(currentPlayer.getName() + "  's Turn");
            currentPlayer.showCardInHand();

            if (activeAce){
                System.out.println(currentPlayer.getName()+"Under Ace of Spades Penality!");
                Card defenceCard=currentPlayer.getDefence(Order.TWO,Suit.Spades);
                if (defenceCard!=null){
                    currentPlayer.dropCard(defenceCard);
                    System.out.println(currentPlayer.getName()+ " Defended Himself with Two of Spades");
                    pendingPenality=7;
                    moveToNext();
                    continue;
                }
                else {
                    for (int i=0;i<pendingPenality;i++){
                        Card drawn = drawCard();
                        if (drawn == null) break;
                        currentPlayer.receiveCards(drawn);
                    }
                    System.out.println(currentPlayer.getName()+" Draw "+pendingPenality+" Cards");
                    activeAce=false;
                    pendingPenality=0;
                    moveToNext();
                    continue;
                }

            }

            if (activeTwo){
                System.out.println(currentPlayer.getName() + " is under TWO penalty!");

                Card defence = currentPlayer.getDefence(Order.TWO);

                if (defence != null){
                    currentPlayer.dropCard(defence);
                    topCard = defence;
                    discardPile.add(defence);

                    System.out.println(currentPlayer.getName() + " Dropped TWO!");

                    pendingTwo += 2;
                    moveToNext();
                    continue;
                } else {
                    for (int i = 0; i < pendingTwo; i++){
                        Card drawn = drawCard();
                        if (drawn == null) break;
                        currentPlayer.receiveCards(drawn);
                    }

                    System.out.println(currentPlayer.getName() + " draws " + pendingTwo + " cards!");

                    activeTwo = false;
                    pendingTwo = 0;

                    moveToNext();
                    continue;
                }
            }



            Card played=findPlayableCard(currentPlayer);
            if (played!=null){
                currentPlayer.dropCard(played);
                topCard=played;
                discardPile.add(played);
                System.out.println(currentPlayer.getName()+" Played "+played);
                if (forcedSuit != null){
                    forcedSuit = null;
                }
                handleSpecialCard(played);
            }
            else {
                System.out.println(currentPlayer.getName()+" Drow A Card");
                Card drawn = drawCard();
                if (drawn == null) break;
                currentPlayer.receiveCards(drawn);
            }
            if (currentPlayer.getHandSize()==0){
                System.out.println(currentPlayer.getName()+"  Wins the Game");
                break;
            }
            moveToNext();
        }
    }

    public void handleSpecialCard(Card card){
        if (card.getOrder()==Order.FIVE){
            System.out.println("Next Player Will Be Skipped");
            skipNext=true;
        }

        if (card.getOrder()==Order.SEVEN){
            System.out.println("The Direction Of the Play Reversed");
            reversed=!reversed;
        }
        if(card.getOrder()==Order.ONE && card.getSuit()==Suit. Spades){
            activeAce=true;
            pendingPenality=5;
        }
        if (card.getOrder()==Order.TWO){
            activeTwo=true;
            pendingTwo+=2;
        }
        if (card.getOrder() == Order.J || card.getOrder() == Order.EIGHT){
            forcedSuit = chooseSuit();
            System.out.println("Suit changed to " + forcedSuit);
        }


    }



    private Suit chooseSuit(){
        System.out.println("Choosing new suit...");

        return Suit.values()[(int)(Math.random() * Suit.values().length)];
    }


    public void moveToNext(){
        if (!reversed){
            currentPlayerIndex=(currentPlayerIndex+1)%players.size();
        }
        else {
            currentPlayerIndex=(currentPlayerIndex -1 +players.size())%players.size();
        }

        if (skipNext){
            System.out.println("Skipping "+players.get(currentPlayerIndex).getName());
            if(!reversed){
                currentPlayerIndex=(currentPlayerIndex+1) % players.size();
            }
            else {
                currentPlayerIndex=(currentPlayerIndex-1 +players.size()) % players.size();
            }
            skipNext=false;
        }
    }


    private Card findPlayableCard(Player player){
        for (Card card : player.getHand()){

            if (forcedSuit != null){
                if (card.getSuit() == forcedSuit ||
                        card.getOrder() == topCard.getOrder()){
                    return card;
                }
            } else {
                if (card.getSuit() == topCard.getSuit() ||
                        card.getOrder() == topCard.getOrder()){
                    return card;
                }
            }
        }
        return null;
    }

    private boolean isValidMove(Card card){
        if (forcedSuit!=null){
            return card.getSuit()==forcedSuit||card.getOrder()==topCard.getOrder();
        }

        return card.getSuit()==topCard.getSuit()||card.getOrder()==topCard.getOrder();
    }

    public boolean playCard(Card card){
        Player player=getCurrentPlayer();
        if (isValidMove(card)){
            player.dropCard(card);
            topCard=card;
            discardPile.add(card);
            handleSpecialCard(card);
            moveToNext();
            return true;

        }
        return false;
    }



    public void startGame(){
        deck.shuffle();
        dealCards();
        firstMove();
    }
}
