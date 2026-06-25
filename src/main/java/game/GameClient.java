package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * TCP client that connects to GameServer.
 * Parses server messages and fires callbacks to the UI.
 */
public class GameClient {

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;

    // Callbacks set by UI
    public Consumer<GameState> onStateUpdate;   // full state refresh
    public Consumer<String>    onYourTurn;       // "YOUR_TURN" or "YOUR_TURN|PENALTY|..."
    public Consumer<String>    onMessage;        // status messages
    public Consumer<String>    onWinner;         // winner name
    public Consumer<List<CardInfo>> onHandUpdate; // your own hand
    public Runnable            onChooseSuit;     // server asks you to choose suit

    public boolean isConnected() { return connected; }

    public void connect(String host, int port, String playerName) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;

        // Start listener thread
        Thread t = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    handleLine(line.trim());
                }
            } catch (IOException e) {
                connected = false;
                if (onMessage != null) onMessage.accept("Disconnected from server.");
            }
        }, "ClientListener");
        t.setDaemon(true);
        t.start();

        send("JOIN|" + playerName);
    }

    private void handleLine(String line) {
        if (line.startsWith("STATE|")) {
            GameState state = parseState(line.substring(6));
            if (onStateUpdate != null) onStateUpdate.accept(state);

        } else if (line.startsWith("YOUR_TURN")) {
            if (onYourTurn != null) onYourTurn.accept(line);

        } else if (line.startsWith("HAND|") || line.equals("HAND")) {
            List<CardInfo> hand = parseHand(line);
            if (onHandUpdate != null) onHandUpdate.accept(hand);

        } else if (line.startsWith("WINNER|")) {
            if (onWinner != null) onWinner.accept(line.substring(7));

        } else if (line.equals("CHOOSE_SUIT")) {
            if (onChooseSuit != null) onChooseSuit.run();

        } else if (line.startsWith("MSG|")) {
            if (onMessage != null) onMessage.accept(line.substring(4));
        }
    }

    // ── outbound commands ──────────────────────────────────────────────────────

    public void playCard(CardInfo card) {
        send("PLAY|" + card.suit + "|" + card.rank);
    }

    public void playSevenWithExtras(CardInfo seven, List<CardInfo> extras) {
        StringBuilder sb = new StringBuilder("PLAY_SEVEN|")
                .append(seven.suit).append("|").append(seven.rank);
        for (CardInfo e : extras) sb.append("|").append(e.suit).append("|").append(e.rank);
        send(sb.toString());
    }

    public void drawCard()       { send("DRAW"); }
    public void pass()           { send("PASS"); }
    public void acceptPenalty()  { send("ACCEPT_PENALTY"); }
    public void chooseSuit(String suit) { send("CHOOSE_SUIT|" + suit); }
    public void defend(CardInfo card)   { send("DEFEND|" + card.suit + "|" + card.rank); }

    private void send(String msg) {
        if (out != null) out.println(msg);
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ── parsers ───────────────────────────────────────────────────────────────

    private GameState parseState(String s) {
        GameState state = new GameState();
        for (String token : s.split(";")) {
            String[] kv = token.split(":", 2);
            if (kv.length < 2) continue;
            switch (kv[0]) {
                case "TOP"    -> { String[] p = kv[1].split(":"); state.topSuit = p[0]; state.topRank = p[1]; }
                case "DIR"    -> state.reversed = kv[1].equals("CCW");
                case "CUR"    -> state.currentPlayer = kv[1];
                case "OVER"   -> state.gameOver = Boolean.parseBoolean(kv[1]);
                case "WINNER" -> state.winner = kv[1];
                case "P"      -> {
                    String[] p = kv[1].split(":");
                    state.playerCardCounts.put(p[0], Integer.parseInt(p[1]));
                }
            }
        }
        return state;
    }

    private List<CardInfo> parseHand(String line) {
        List<CardInfo> hand = new ArrayList<>();
        String[] parts = line.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] sc = parts[i].split(":");
            if (sc.length == 2) hand.add(new CardInfo(sc[0], sc[1]));
        }
        return hand;
    }

    // ── data classes ──────────────────────────────────────────────────────────

    public static class GameState {
        public String topSuit, topRank, currentPlayer, winner;
        public boolean reversed, gameOver;
        public Map<String, Integer> playerCardCounts = new LinkedHashMap<>();
    }

    public static class CardInfo {
        public final String suit, rank;
        public CardInfo(String suit, String rank) { this.suit = suit; this.rank = rank; }
        @Override public String toString() { return rank + " of " + suit; }
    }
}
