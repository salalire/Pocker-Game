package game;

import java.util.ArrayList;
import java.util.List;

public class Game {
    List<Player> players;
    Deck deck;
    public Game(){
        deck=new Deck();
        players=new ArrayList<>();
    }

    public void addPlayers(String name){
        players.add(new Player(name));

    }

    public void dealCards(){
        for(Player player :players){
            player.receiveCards(deck.dealCard());
            player.receiveCards(deck.dealCard());
        }
    }
    public void showPlayerCards(){
        for (Player player:players){
            System.out.println(player.getName() +" Cards");
            player.showCardInHand();
            System.out.println();
        }
    }

    public void startGame(){
        deck.shuffle();
        dealCards();
        showPlayerCards();
    }
}
