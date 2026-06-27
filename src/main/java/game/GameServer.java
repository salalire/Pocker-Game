package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * TCP game server for online multiplayer.
 *
 * Protocol (plain text, newline-delimited):
 *   Server → Client:
 *     STATE|<json-like state string>        — full game state update
 *     WAIT|<playerName>                     — it's this player's turn
 *     YOUR_TURN                             — sent only to the current player
 *     WINNER|<playerName>                   — game over
 *     MSG|<text>                            — status message broadcast
 *
 *   Client → Server:
 *     JOIN|<playerName>                     — register player
 *     PLAY|<suit>|<rank>                    — play a card
 *     PLAY_SEVEN|<suit>|<rank>|<e1suit>|<e1rank>|...  — 7 with extras
 *     DRAW                                  — draw a card
 *     PASS                                  — pass turn
 *     DEFEND|<suit>|<rank>                  — defend against penalty
 *     ACCEPT_PENALTY                        — accept penalty cards
 *     CHOOSE_SUIT|<suit>                    — after J/8
 */
public class GameServer {

    public static final int PORT = 45678;

    private final int expectedPlayers;
    private final ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private Game game;
    private volatile boolean started = false;
    private Consumer<String> logCallback; // optional UI log

    public GameServer(int expectedPlayers) throws IOException {
        this.expectedPlayers = expectedPlayers;
        this.serverSocket = new ServerSocket(PORT);
    }

    public void setLogCallback(Consumer<String> cb) { this.logCallback = cb; }

    private void log(String msg) {
        System.out.println("[SERVER] " + msg);
        if (logCallback != null) logCallback.accept(msg);
    }

    /** Starts accepting connections on a background thread. */
    public void startAccepting() {
        Thread t = new Thread(() -> {
            log("Waiting for " + expectedPlayers + " players on port " + PORT + "...");
            try {
                while (clients.size() < expectedPlayers) {
                    Socket sock = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(sock);
                    synchronized (clients) { clients.add(handler); }
                    new Thread(handler).start();
                    log("Player connected (" + clients.size() + "/" + expectedPlayers + ")");
                }
                // All connected — wait for JOIN messages then start
            } catch (IOException e) {
                if (!serverSocket.isClosed()) log("Accept error: " + e.getMessage());
            }
        }, "ServerAccept");
        t.setDaemon(true);
        t.start();
    }

    /** Called once all players have sent JOIN. Starts the game. */
    private synchronized void tryStartGame() {
        if (started) return;
        long joined = clients.stream().filter(c -> c.playerName != null).count();
        if (joined < expectedPlayers) return;
        started = true;

        game = new Game();
        for (ClientHandler ch : clients) game.addPlayers(ch.playerName);
        game.startGame();

        broadcast("MSG|Game started! Top card: " + game.getTopCard());
        broadcastState();
        notifyCurrentPlayer();
    }

    private void broadcastState() {
        String state = buildStateString();
        broadcast("STATE|" + state);
    }

    private void notifyCurrentPlayer() {
        Player cur = game.getCurrentPlayer();
        broadcast("WAIT|" + cur.getName());
        for (ClientHandler ch : clients) {
            if (ch.playerName != null && ch.playerName.equals(cur.getName())) {
                if (game.hasPendingPenalty()) {
                    ch.send("YOUR_TURN|PENALTY|" + game.getPendingPenalty() + "|" + (game.isActiveAce() ? "ACE" : "TWO"));
                } else {
                    ch.send("YOUR_TURN");
                }
            }
        }
    }

    private void broadcast(String msg) {
        synchronized (clients) {
            for (ClientHandler ch : clients) ch.send(msg);
        }
    }

    /** Serialise game state to a simple pipe-delimited string. */
    private String buildStateString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TOP:").append(game.getTopCard().getSuit()).append(":").append(game.getTopCard().getOrder());
        sb.append(";DIR:").append(game.isReversed() ? "CCW" : "CW");
        sb.append(";CUR:").append(game.getCurrentPlayer().getName());
        sb.append(";OVER:").append(game.isGameOver());
        if (game.isGameOver()) sb.append(";WINNER:").append(game.getWinner().getName());

        // Each player's hand size + hand (only sent to their own client via YOUR_HAND)
        for (Player p : game.getPlayers()) {
            sb.append(";P:").append(p.getName()).append(":").append(p.getHandSize());
        }
        return sb.toString();
    }

    /** Send a player their own hand. */
    private void sendHand(ClientHandler ch) {
        if (ch.playerName == null) return;
        Player p = findPlayer(ch.playerName);
        if (p == null) return;
        StringBuilder sb = new StringBuilder("HAND");
        for (Card c : p.getHand()) {
            sb.append("|").append(c.getSuit()).append(":").append(c.getOrder());
        }
        ch.send(sb.toString());
    }

    private Player findPlayer(String name) {
        for (Player p : game.getPlayers()) if (p.getName().equals(name)) return p;
        return null;
    }

    private Card findCard(Player p, String suit, String rank) {
        Suit s = Suit.valueOf(suit);
        Order o = Order.valueOf(rank);
        for (Card c : p.getHand()) if (c.getSuit() == s && c.getOrder() == o) return c;
        return null;
    }

    // ── per-client handler ────────────────────────────────────────────────────

    private class ClientHandler implements Runnable {
        final Socket socket;
        String playerName;
        PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
            try { this.out = new PrintWriter(socket.getOutputStream(), true); }
            catch (IOException e) { log("Output stream error: " + e.getMessage()); }
        }

        void send(String msg) {
            if (out != null) out.println(msg);
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line.trim());
                }
            } catch (IOException e) {
                log("Client disconnected: " + (playerName != null ? playerName : "unknown"));
            }
        }

        private void handleMessage(String msg) {
            String[] parts = msg.split("\\|");
            String cmd = parts[0];

            switch (cmd) {
                case "JOIN" -> {
                    playerName = parts.length > 1 ? parts[1] : "Player";
                    log(playerName + " joined.");
                    tryStartGame();
                    if (started) sendHand(this);
                }
                case "PLAY" -> {
                    if (!isMyTurn()) { send("MSG|Not your turn."); return; }
                    Card card = findCard(findPlayer(playerName), parts[1], parts[2]);
                    if (card == null) { send("MSG|Card not found."); return; }
                    boolean ok = game.playCard(card);
                    if (!ok) { send("MSG|Invalid move."); return; }
                    afterMove(card);
                }
                case "PLAY_SEVEN" -> {
                    if (!isMyTurn()) { send("MSG|Not your turn."); return; }
                    Player p = findPlayer(playerName);
                    Card seven = findCard(p, parts[1], parts[2]);
                    List<Card> extras = new ArrayList<>();
                    for (int i = 3; i + 1 < parts.length; i += 2) {
                        Card e = findCard(p, parts[i], parts[i+1]);
                        if (e != null) extras.add(e);
                    }
                    if (!game.playSevenWithExtras(seven, extras)) { send("MSG|Invalid 7 play."); return; }
                    afterMove(null);
                }
                case "DRAW" -> {
                    if (!isMyTurn()) { send("MSG|Not your turn."); return; }
                    game.drawForCurrentPlayer();
                    game.setHasDrawn(true);
                    sendHand(this);
                    broadcastState();
                    send("MSG|You drew a card.");
                }
                case "PASS" -> {
                    if (!isMyTurn()) { send("MSG|Not your turn."); return; }
                    game.setHasDrawn(false);
                    game.moveToNext();
                    broadcastState();
                    sendHandToAll();
                    notifyCurrentPlayer();
                }
                case "DEFEND" -> {
                    if (!isMyTurn()) { send("MSG|Not your turn."); return; }
                    Player p = findPlayer(playerName);
                    Card defence = findCard(p, parts[1], parts[2]);
                    boolean ok = game.isActiveAce()
                            ? game.defendWithTwoOfSpades(defence)
                            : game.defendWithTwo(defence);
                    if (ok) {
                        game.moveToNext();
                        broadcastState();
                        sendHandToAll();
                        notifyCurrentPlayer();
                    } else { send("MSG|Invalid defence."); }
                }
                case "ACCEPT_PENALTY" -> {
                    if (!isMyTurn()) { send("MSG|Not your turn."); return; }
                    game.acceptPenalty();
                    // Player stays in their turn after drawing penalty cards
                    broadcastState();
                    sendHandToAll();
                    // Tell this player it's still their turn (penalty cleared, now play normally)
                    send("YOUR_TURN");
                    send("MSG|Penalty accepted. Play a card, draw one more, or pass.");
                }
                case "CHOOSE_SUIT" -> {
                    game.setSuit(Suit.valueOf(parts[1]));
                    broadcast("MSG|Suit changed to " + parts[1]);
                    broadcastState();
                    notifyCurrentPlayer();
                }
                default -> send("MSG|Unknown command: " + cmd);
            }
        }

        private boolean isMyTurn() {
            return started && playerName != null
                    && playerName.equals(game.getCurrentPlayer().getName());
        }

        private void afterMove(Card card) {
            if (game.isGameOver()) {
                broadcast("WINNER|" + game.getWinner().getName());
                broadcastState();
                return;
            }
            if (card != null && (card.getOrder() == Order.EIGHT || card.getOrder() == Order.J)) {
                send("CHOOSE_SUIT");
            }
            broadcastState();
            sendHandToAll();
            notifyCurrentPlayer();
        }
    }

    private void sendHandToAll() {
        synchronized (clients) {
            for (ClientHandler ch : clients) sendHand(ch);
        }
    }

    public void stop() {
        try { serverSocket.close(); } catch (IOException ignored) {}
    }

    /**
     * Returns the best local IP address for LAN/hotspot play.
     *
     * Strategy:
     *   1. Scan all network interfaces.
     *   2. Skip loopback (127.x), link-local (169.254.x), and virtual/inactive interfaces.
     *   3. Prefer addresses in private ranges: 192.168.x.x, 10.x.x.x, 172.16-31.x.x
     *   4. Prefer WiFi/WLAN interfaces over others (common on hotspot connections).
     *   5. Fall back to InetAddress.getLocalHost() if nothing better is found.
     *   6. Last resort: return "127.0.0.1".
     */
    public static String getLocalIp() {
        String best = null;
        int bestScore = -1;

        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                // Skip loopback, down, virtual, and point-to-point interfaces
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual() || ni.isPointToPoint()) continue;

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                // Skip virtual adapters (VPN, Hyper-V, Docker, etc.)
                if (displayName.contains("virtual") || displayName.contains("hyper-v")
                        || displayName.contains("vmware") || displayName.contains("docker")
                        || displayName.contains("loopback") || displayName.contains("bluetooth")) continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    // IPv4 only
                    if (addr.getAddress().length != 4) continue;

                    String ip = addr.getHostAddress();
                    int score = scoreAddress(ip, name, displayName);
                    if (score > bestScore) {
                        bestScore = score;
                        best = ip;
                    }
                }
            }
        } catch (Exception ignored) {}

        if (best != null) return best;

        // Fallback: getLocalHost (may return 127.0.0.1 on some systems)
        try { return InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    /**
     * Scores an IP/interface candidate. Higher = better.
     * Hotspot/WiFi private addresses get the highest score.
     */
    private static int scoreAddress(String ip, String ifName, String displayName) {
        int score = 0;

        // Private range bonus
        if (ip.startsWith("192.168.")) score += 30;
        else if (ip.startsWith("10."))   score += 25;
        else if (ip.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*")) score += 20;

        // Hotspot / WiFi interface bonus (common names on Windows and Linux)
        if (ifName.contains("wlan") || ifName.contains("wifi") || ifName.contains("wi-fi")
                || displayName.contains("wi-fi") || displayName.contains("wireless")
                || displayName.contains("wlan")) score += 20;

        // Ethernet is good too
        if (ifName.startsWith("eth") || displayName.contains("ethernet")) score += 10;

        // 192.168.43.x is the Android hotspot default subnet — bonus
        if (ip.startsWith("192.168.43.")) score += 5;

        return score;
    }
}
