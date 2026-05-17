package myApp;
import game.Card;
import game.Game;
import javafx.scene.layout.BorderPane;

import javax.swing.*;
import java.awt.*;

public class MainUi {
    public static void main(String[] args){

        Game game=new Game();
        String name1="Samuel";
        String name2="abel";
        String name3="Dani";
        game.addPlayers(name1);
        game.addPlayers(name2);
        game.addPlayers(name3);
        game.startGame();

        JFrame frame=new JFrame("Crazy Card Game");
        frame.setSize(500,400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JPanel topPanel=new JPanel();
        JLabel topLabel=new JLabel("Current Card: "+game.getTopCard());
        topPanel.add(topLabel);

        JPanel centerPanel=new JPanel();
        JLabel centerLabel=new JLabel("Your Cards:");
        centerPanel.add(centerLabel);

        JPanel bottomPanel=new JPanel();
        JButton draw=new JButton("Draw");
        JButton play=new JButton("Play");
        bottomPanel.add(draw);
        bottomPanel.add(play);

        centerPanel.setLayout(new FlowLayout());
        for (Card card:game.getCard()){
            JButton cardButton=new JButton(card.toString());
            cardButton.addActionListener(e->{
                boolean isValidMove=game.playCard(card);
                if (isValidMove){
                    topLabel.setText("Current Card: "+game.getTopCard());
                }
                else{
                    topLabel.setText("Invalid move!");
                }
            });
            centerPanel.add(cardButton);
        }



        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel,BorderLayout.CENTER);
        frame.add(bottomPanel,BorderLayout.SOUTH);
        frame.setVisible(true);

    }
}
