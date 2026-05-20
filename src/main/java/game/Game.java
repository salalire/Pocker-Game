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
    private boolean hasDrawn = false;
    private boolean gameOver=false;
    private Player winner=null;

    public boolean isGameOver(){
     return gameOver;
    }

    public Player getWinner(){
        return winner;
    }


    public Game(){
        deck=new Deck();
        players=new ArrayList<>();
    }

    public void addPlayers(String name){
        players.add(new Player(name));

    }

    public void setHasDrawn(boolean value){
        hasDrawn = value;
    }

    public boolean hasDrawn(){
        return hasDrawn;
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


    public Card drawForCurrentPlayer(){
        Card drawn = drawCard();

        if (drawn != null){
            getCurrentPlayer().receiveCards(drawn);
        }

        return drawn;
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


    }

    public void setSuit(Suit suit){
        this.forcedSuit=suit;
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



    public boolean isValidMove(Card card){
        if (card.getOrder()==Order.EIGHT||card.getOrder()==Order.J) return true;
        if (forcedSuit!=null){
            return card.getSuit()==forcedSuit||card.getOrder()==topCard.getOrder();
        }

        return card.getSuit()==topCard.getSuit()||card.getOrder()==topCard.getOrder();
    }


    public boolean handleSpecialCardForCurrentPlayer(){
        Player currentPlayer=getCurrentPlayer();
        if (activeAce){
            System.out.println(currentPlayer.getName()+" is under ace penality ");
            Card defenceCard=currentPlayer.getDefence(Order.TWO,Suit.Spades);

            if (defenceCard != null){
                currentPlayer.dropCard(defenceCard);
                topCard = defenceCard;
                discardPile.add(defenceCard);
                System.out.println(currentPlayer.getName()+" defended with Two of Spades");
                pendingPenality = 7;
//                moveToNext();
                return true;
            } else {
                for (int i = 0; i < pendingPenality; i++){
                    Card drawn = drawCard();
                    if (drawn == null) break;
                    currentPlayer.receiveCards(drawn);
                }
                System.out.println(currentPlayer.getName()+" draws "+pendingPenality);
                activeAce = false;
                pendingPenality = 0;
                hasDrawn = false;
//                moveToNext();
                return true;
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
//                    moveToNext();
                    return true;
                } else {
                    for (int i = 0; i < pendingTwo; i++){
                        Card drawn = drawCard();
                        if (drawn == null) break;
                        currentPlayer.receiveCards(drawn);
                    }

                    System.out.println(currentPlayer.getName() + " draws " + pendingTwo + " cards!");

                    activeTwo = false;
                    pendingTwo = 0;
                    hasDrawn=false;
//                    moveToNext();
                    return true;

                }
            }


        return false;
    }




    public boolean playCard(Card card){
        Player player=getCurrentPlayer();
        if (isValidMove(card)){
            player.dropCard(card);
            topCard=card;
            discardPile.add(card);
            handleSpecialCard(card);
            hasDrawn = false;

            if (player.getHandSize()==0){
                gameOver=true;
                winner=player;
                return true;
            }
            moveToNext();
            if (forcedSuit != null){
                forcedSuit = null;
            }
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
