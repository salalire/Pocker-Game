package myApp;
import javax.swing.*;

public class MainUi {
    public static void main(String[] args){
        JFrame frame=new JFrame("Crazy Card Game");
        frame.setSize(500,400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel=new JPanel();
        frame.add(panel);
        JLabel label=new JLabel("The Game Started: ");
        panel.add(label);
        frame.setVisible(true);

    }
}
