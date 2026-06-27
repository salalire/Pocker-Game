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

    public List<Player> getPlayers(){
        return players;
    }

    public boolean isReversed(){
        return reversed;
    }

    public int getCurrentPlayerIndex(){
        return currentPlayerIndex;
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

    public Suit getForcedSuit(){
        return forcedSuit;
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
        // If a penalty is active, the ONLY valid moves are the defence responses.
        // Human players: the UI already blocks this via showPenaltyPrompt.
        // AI players: AiPlayer.takeTurn handles this path separately.
        // This guard is the safety net for any direct playCard() call during penalty.
        if (activeTwo) {
            // Only a 2 of any suit can be played to stack the penalty
            return card.getOrder() == Order.TWO;
        }
        if (activeAce) {
            // Only 2 of Spades can be played to defend the Ace of Spades
            return card.getOrder() == Order.TWO && card.getSuit() == Suit.Spades;
        }

        if (card.getOrder()==Order.EIGHT||card.getOrder()==Order.J) return true;
        if (forcedSuit!=null){
            return card.getSuit()==forcedSuit||card.getOrder()==topCard.getOrder();
        }

        return card.getSuit()==topCard.getSuit()||card.getOrder()==topCard.getOrder();
    }


    /**
     * Returns whether a penalty is currently active for the current player.
     */
    public boolean hasPendingPenalty() {
        return activeAce || activeTwo;
    }

    public boolean isActiveAce() { return activeAce; }
    public boolean isActiveTwo() { return activeTwo; }
    public int getPendingPenalty() { return activeAce ? pendingPenality : pendingTwo; }

    /**
     * Called when the current player has chosen to DEFEND against a TWO penalty
     * by dropping one of their own 2s. Returns true if defence was valid.
     */
    public boolean defendWithTwo(Card defence) {
        if (!activeTwo) return false;
        if (defence.getOrder() != Order.TWO) return false;
        Player player = getCurrentPlayer();
        if (!player.getHand().contains(defence)) return false;

        player.dropCard(defence);
        topCard = defence;
        discardPile.add(defence);
        pendingTwo += 2;
        // stack penalty passes on — do NOT move to next here
        return true;
    }

    /**
     * Called when the current player has chosen to DEFEND against an ACE OF SPADES
     * by dropping their 2 of Spades. Returns true if defence was valid.
     */
    public boolean defendWithTwoOfSpades(Card defence) {
        if (!activeAce) return false;
        if (defence.getOrder() != Order.TWO || defence.getSuit() != Suit.Spades) return false;
        Player player = getCurrentPlayer();
        if (!player.getHand().contains(defence)) return false;

        player.dropCard(defence);
        topCard = defence;
        discardPile.add(defence);
        pendingPenality = 7;
        // stack penalty passes on
        return true;
    }

    /**
     * Current player accepts the penalty (draws the penalty cards).
     * The turn does NOT advance — the player still gets to play their turn after drawing.
     * The UI should call refresh() after this so the player can play, draw one, or pass.
     */
    public void acceptPenalty() {
        Player player = getCurrentPlayer();
        if (activeAce) {
            for (int i = 0; i < pendingPenality; i++) {
                Card drawn = drawCard();
                if (drawn == null) break;
                player.receiveCards(drawn);
            }
            activeAce = false;
            pendingPenality = 0;
        } else if (activeTwo) {
            for (int i = 0; i < pendingTwo; i++) {
                Card drawn = drawCard();
                if (drawn == null) break;
                player.receiveCards(drawn);
            }
            activeTwo = false;
            pendingTwo = 0;
        }
        hasDrawn = false;
        // NOTE: moveToNext() intentionally removed — player plays their turn after drawing
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
                hasDrawn = false;
                return true;
            }
        }

        return false;
    }




    /**
     * "Crazy" challenge: when someone calls crazy on the current player
     * (e.g. after an invalid play attempt), that player draws 2 cards.
     */
    public void applyCrazyPenalty() {
        Player player = getCurrentPlayer();
        for (int i = 0; i < 2; i++) {
            Card drawn = drawCard();
            if (drawn != null) player.receiveCards(drawn);
        }
    }

    public boolean playCard(Card card){
        // Block all normal play when a penalty is pending — player must use defendWithTwo / acceptPenalty
        if (hasPendingPenalty()) return false;

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

    /**
     * Play a 7 along with up to 4 additional cards of the same suit.
     * The 7 must be a valid move. All extra cards must share the same suit as the 7.
     * The direction-reverse rule still applies once (triggered by the 7).
     *
     * @param seven      the 7 card being played
     * @param extraCards additional same-suit cards to drop (max 4)
     * @return true if the play was accepted, false otherwise
     */
    /**
     * Play a 7 along with up to 4 additional cards of the same suit.
     * The 7 must be a valid move. All extra cards must share the same suit as the 7.
     *
     * Key rule: when the 7 is bundled with extras it does NOT reverse direction
     * (the reverse only fires when the 7 is played alone).
     *
     * @param seven      the 7 card being played
     * @param extraCards additional same-suit cards to drop alongside it (max 4)
     * @return true if the play was accepted, false otherwise
     */
    public boolean playSevenWithExtras(Card seven, List<Card> extraCards){
        if (seven.getOrder() != Order.SEVEN) return false;
        if (!isValidMove(seven)) return false;
        if (extraCards.size() > 4) return false;

        Suit sevenSuit = seven.getSuit();
        for (Card c : extraCards){
            if (c.getSuit() != sevenSuit) return false;
        }

        Player player = getCurrentPlayer();
        boolean bundled = !extraCards.isEmpty();

        // Drop the 7 — only trigger reverse when played alone
        player.dropCard(seven);
        topCard = seven;
        discardPile.add(seven);
        hasDrawn = false;

        if (bundled){
            // Bundled: no reverse — skip the reverse part of handleSpecialCard
            System.out.println("7 bundled with extras — direction NOT reversed");
        } else {
            // Solo 7: full special-card handling (reverse fires)
            handleSpecialCard(seven);
        }

        // Drop each extra card (no special effects from extras)
        for (Card c : extraCards){
            player.dropCard(c);
            topCard = c;
            discardPile.add(c);
        }

        if (player.getHandSize() == 0){
            gameOver = true;
            winner = player;
            return true;
        }

        moveToNext();
        if (forcedSuit != null){
            forcedSuit = null;
        }
        return true;
    }



    public void startGame(){
        deck.shuffle();
        dealCards();
        firstMove();
    }
}
