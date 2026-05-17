package myApp;
import game.Card;
import game.Game;
import javafx.scene.layout.BorderPane;

import javax.swing.*;
import java.awt.*;


public class MainUi {

    private static void refreshCards(JPanel centerPanel, Game game, JLabel currentCardLabel){
        centerPanel.removeAll();

        for (Card card : game.getCard()){
            JButton cardButton = new JButton(card.toString());

            cardButton.addActionListener(e -> {
                boolean success = game.playCard(card);

                if (success){
                    currentCardLabel.setText("Current Card: " + game.getTopCard());
                    refreshCards(centerPanel, game, currentCardLabel);
                } else {
                    currentCardLabel.setText("Invalid Move!");
                }
            });

            centerPanel.add(cardButton);
        }

        centerPanel.revalidate();
        centerPanel.repaint();
    }


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
        draw.addActionListener(e->{
            game.drawForCurrentPlayer();
           refreshCards(centerPanel,game,topLabel);
        });
        JButton play=new JButton("Play");
        bottomPanel.add(draw);
        bottomPanel.add(play);

        centerPanel.setLayout(new FlowLayout());
        refreshCards(centerPanel, game, topLabel);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel,BorderLayout.CENTER);
        frame.add(bottomPanel,BorderLayout.SOUTH);
        frame.setVisible(true);

    }
}
