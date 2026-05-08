package myApp;
import game.Game;
public class Main {
    public static void main(String[] args) {
     Game game=new Game();
     String name1="Samuel";
     String name2="abel";
     game.addPlayers(name1);
     game.addPlayers(name2);
     game.startGame();
    }
}