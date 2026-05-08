package game;

import java.util.List;

public class Game {
    List<Player> players;
    Deck deck;
    public void startGame(){
        deck.shuffle();
        for (Player player:players){
            player.receiveCards(deck.dealCard());
        }
    }
}
