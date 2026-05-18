package myApp;
import game.Card;
import game.Game;
import javafx.scene.layout.BorderPane;

import javax.swing.*;
import java.awt.*;


public class MainUi {

    private static void refreshCards(JPanel centerPanel, Game game, JLabel currentCardLabel,JLabel currentPlayer){
        centerPanel.removeAll();

        for (Card card : game.getCard()){
            JButton cardButton = new JButton(card.toString());

            cardButton.addActionListener(e -> {
                boolean success = game.playCard(card);

                if (success){
                    currentCardLabel.setText("Current Card: " + game.getTopCard());
                    game.moveToNext();
                    refreshCards(centerPanel, game, currentCardLabel,currentPlayer);
                } else {
                    currentCardLabel.setText("Invalid Move!");
                }
            });

            centerPanel.add(cardButton);
        }
        currentPlayer.setText(game.getCurrentPlayer().getName()+"'s Turn");

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
        JLabel currentPlayer=new JLabel();
//        currentPlayer.setText(game.getCurrentPlayer().getName()+"'s Turn");
        topPanel.add(currentPlayer);
        topPanel.add(topLabel);

        JPanel centerPanel=new JPanel();
        JLabel centerLabel=new JLabel("Your Cards:");
        centerPanel.add(centerLabel);

        JPanel bottomPanel=new JPanel();
        JButton draw=new JButton("Draw");
        JButton pass = new JButton("Pass");

        pass.addActionListener(e->{
            game.moveToNext();
            refreshCards(centerPanel,game,topLabel,currentPlayer);
        });


        draw.addActionListener(e->{
            Card drawn = game.drawForCurrentPlayer();

            if (drawn != null){

                boolean canPlay = game.isValidMove(drawn);

                if (!canPlay){
                    game.moveToNext();
                }
            }
            refreshCards(centerPanel,game,topLabel,currentPlayer);
        });



        JButton play=new JButton("Play");
        bottomPanel.add(draw);
        bottomPanel.add(play);
        bottomPanel.add(pass);

        centerPanel.setLayout(new FlowLayout());
        refreshCards(centerPanel, game, topLabel,currentPlayer);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel,BorderLayout.CENTER);
        frame.add(bottomPanel,BorderLayout.SOUTH);
//        frame.add(currentPlayer,BorderLayout.NORTH);
        frame.setVisible(true);

    }
}
