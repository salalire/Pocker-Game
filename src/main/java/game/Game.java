package game;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private List<Player> players;
   private Deck deck;
   private Card topCard;
   private boolean reversed=false;
   private int currentPlayerIndex=0;

    public Game(){
        deck=new Deck();
        players=new ArrayList<>();
    }

    public void addPlayers(String name){
        players.add(new Player(name));

    }

    public void dealCards(){
        for(Player player :players){
            for (int i=0;i<6;i++){
                player.receiveCards(deck.dealCard());
            }
            players.get(0).receiveCards(deck.dealCard());
        }
    }

    public void firstMove(){
        Player firstPlayer= players.get(0);
        Card firstCard=firstPlayer.playCard(0);
        topCard=firstCard;
        System.out.println("The First Player Is "+ firstPlayer.getName()+"Starts with "+toCard);
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
            System.out.println(currentPlayer.getName() + "'s Turn");
            currentPlayer.showCardInHand();

            Card played=currentPlayer.playFirstValidCard(topCard);
            if (played!=null){
                topCard=played;
                System.out.println(currentPlayer.getName()+"Played"+played);
            }
            else {
                System.out.println(currentPlayer.getName()+"Drow A Card");
                currentPlayer.receiveCards(deck.dealCard());
            }
            if (currentPlayer.getHandSize()==0){
                System.out.println(currentPlayer.getName()+"Wins the Game");
            }
            moveToNext();
        }
    }

    public void moveToNext(){
        if (!reversed){
            currentPlayerIndex=(currentPlayerIndex+1)%players.size();
        }
        else {
            currentPlayerIndex=(currentPlayerIndex -1 +players.size())%players.size();
        }
    }
    public void startGame(){
        deck.shuffle();
        dealCards();
        showPlayerCards();
        firstMove();
        playGame();
    }
}
