package myApp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.AiPlayer;
import game.Card;
import game.Game;
import game.GameClient;
import game.GameClient.CardInfo;
import game.GameClient.GameState;
import game.GameServer;
import game.Order;
import game.Player;
import game.Suit;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainUi extends Application {

    // ── palette ───────────────────────────────────────────────────────────────
    private static final Color FELT_DARK  = Color.web("#1a6b3c");
    private static final Color FELT_MID   = Color.web("#1e7d46");
    private static final Color FELT_LIGHT = Color.web("#25954f");
    private static final Color CARD_WHITE = Color.web("#fdfaf4");
    private static final Color RED_SUIT   = Color.web("#cc0000");
    private static final Color BLACK_SUIT = Color.web("#111111");
    private static final Color GOLD       = Color.web("#ffd700");
    private static final Color GOLD_DARK  = Color.web("#b8860b");

    // ── game mode enum ────────────────────────────────────────────────────────
    enum GameMode { VS_COMPUTER, LOCAL_MULTIPLAYER, ONLINE_HOST, ONLINE_JOIN }

    // ── shared state ──────────────────────────────────────────────────────────
    private Stage primaryStage;
    private GameMode mode;

    // ── local game state (VS_COMPUTER + LOCAL_MULTIPLAYER) ────────────────────
    private Game game;
    private boolean[] isAi;          // isAi[i] = true if player i is computer
    private BorderPane root;
    private HBox handArea;
    private StackPane discardArea;
    private HBox opponentBar;
    private Label statusLabel, directionLabel, playerNameLabel;
    private Button drawBtn, passBtn;
    private final List<Card> sevenExtras = new ArrayList<>();
    private Card pendingSevenCard = null;
    private HBox sevenBundleBar;
    private boolean lastPlayInvalid = false;

    // ── online state ──────────────────────────────────────────────────────────
    private GameClient client;
    private GameServer server;
    private String myName;
    private List<CardInfo> myHand = new ArrayList<>();
    private GameState lastState;
    private boolean myTurn = false;
    private boolean penaltyActive = false;
    private boolean penaltyIsAce = false;
    private int penaltyAmount = 0;
    private final List<CardInfo> sevenExtrasOnline = new ArrayList<>();
    private CardInfo pendingSevenOnline = null;

    // ═════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Crazy Card Game");
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        showModeSelect();
        stage.show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SCREEN 1 — MODE SELECTION
    // ═════════════════════════════════════════════════════════════════════════

    private void showModeSelect() {
        // Use a ScrollPane so nothing clips even at small sizes
        VBox page = new VBox(30);
        page.setAlignment(Pos.CENTER);
        page.setBackground(feltBackground());
        page.setPadding(new Insets(40));

        Label title = new Label("CRAZY CARD GAME");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 42));
        title.setTextFill(GOLD);
        title.setEffect(new DropShadow(8, GOLD_DARK));

        Label sub = new Label("Choose how you want to play");
        sub.setFont(Font.font("Verdana", 16));
        sub.setTextFill(Color.WHITE);

        HBox modeRow = new HBox(20);
        modeRow.setAlignment(Pos.CENTER);

        modeRow.getChildren().addAll(
            buildModeCard("vs Computer",
                    "Play against AI opponents\non your own device",
                    "#2196F3", "#1565C0",
                    e -> showVsComputerSetup()),
            buildModeCard("Local Pass & Play",
                    "Multiple players on\none device, pass it around",
                    "#27ae60", "#1e8449",
                    e -> showLocalSetup()),
            buildModeCard("Online - Host",
                    "Host a game and invite\nfriends by IP address",
                    "#e67e22", "#ca6f1e",
                    e -> showOnlineHostSetup()),
            buildModeCard("Online - Join",
                    "Join a game hosted\nby a friend",
                    "#8e44ad", "#6c3483",
                    e -> showOnlineJoinSetup())
        );

        page.getChildren().addAll(title, sub, modeRow);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(sp, 1100, 750);
        } else {
            scene.setRoot(sp);
        }
        primaryStage.setScene(scene);
    }

    private Pane buildModeCard(String heading, String desc, String base, String dark,
                                javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setPrefSize(210, 200);
        card.setBackground(new Background(new BackgroundFill(
                Color.web(base), new CornerRadii(16), Insets.EMPTY)));
        card.setEffect(new DropShadow(10, Color.BLACK));

        Label h = new Label(heading);
        h.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        h.setTextFill(Color.WHITE);
        h.setTextAlignment(TextAlignment.CENTER);
        h.setWrapText(true);

        Label d = new Label(desc);
        d.setFont(Font.font("Verdana", 12));
        d.setTextFill(Color.web("#e8e8e8"));
        d.setTextAlignment(TextAlignment.CENTER);
        d.setWrapText(true);

        Button btn = new Button("Select");
        btn.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        btn.setTextFill(Color.web("#1a1a1a"));   // dark text — readable on any card colour
        btn.setPrefWidth(130);
        btn.setBackground(new Background(new BackgroundFill(Color.web("#ffffff"), new CornerRadii(8), Insets.EMPTY)));
        btn.setEffect(new DropShadow(4, Color.color(0,0,0,0.35)));
        btn.setOnMouseEntered(e -> btn.setBackground(new Background(new BackgroundFill(Color.web("#e0e0e0"), new CornerRadii(8), Insets.EMPTY))));
        btn.setOnMouseExited( e -> btn.setBackground(new Background(new BackgroundFill(Color.web("#ffffff"), new CornerRadii(8), Insets.EMPTY))));
        btn.setOnAction(action);

        card.getChildren().addAll(h, d, btn);

        card.setOnMouseEntered(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(120), card); st.setToX(1.05); st.setToY(1.05); st.play(); });
        card.setOnMouseExited( e -> { ScaleTransition st = new ScaleTransition(Duration.millis(120), card); st.setToX(1.0);  st.setToY(1.0);  st.play(); });
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SCREEN 2a — VS COMPUTER SETUP
    // ═════════════════════════════════════════════════════════════════════════

    private void showVsComputerSetup() {
        mode = GameMode.VS_COMPUTER;
        VBox page = setupPage("Play vs Computer");

        Label humanLbl = new Label("Your name:");
        humanLbl.setTextFill(Color.WHITE);
        humanLbl.setFont(Font.font("Verdana", 13));
        TextField humanName = styledField("You");
        HBox humanRow = centreRow(humanLbl, humanName);

        Label aiLbl = new Label("Number of AI opponents (1-5):");
        aiLbl.setTextFill(Color.WHITE);
        aiLbl.setFont(Font.font("Verdana", 13));
        Spinner<Integer> aiCount = new Spinner<>(1, 5, 2);
        aiCount.setPrefWidth(80);
        HBox aiRow = centreRow(aiLbl, aiCount);

        Button start = buildSetupButton("Start Game", "#1565C0");
        start.setOnAction(e -> {
            String name = humanName.getText().trim();
            if (name.isEmpty()) name = "You";
            int n = aiCount.getValue();
            startVsComputer(name, n);
        });

        Button back = buildSetupButton("Back", "#555555");
        back.setOnAction(e -> showModeSelect());

        page.getChildren().addAll(humanRow, aiRow, start, back);
        setScrollScene(page);
    }

    private void startVsComputer(String humanName, int numAi) {
        game = new Game();
        isAi = new boolean[1 + numAi];
        game.addPlayers(humanName);
        isAi[0] = false;
        for (int i = 1; i <= numAi; i++) {
            game.addPlayers("CPU " + i);
            isAi[i] = true;
        }
        game.startGame();
        root = buildGameRoot();
        // Reuse scene — just swap root
        primaryStage.getScene().setRoot(root);
        runAiTurnsIfNeeded();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SCREEN 2b — LOCAL MULTIPLAYER SETUP
    // ═════════════════════════════════════════════════════════════════════════

    private void showLocalSetup() {
        mode = GameMode.LOCAL_MULTIPLAYER;
        VBox page = setupPage("Pass & Play Setup");

        Label countLbl = new Label("Number of players:");
        countLbl.setTextFill(Color.WHITE);
        countLbl.setFont(Font.font("Verdana", 13));
        Spinner<Integer> countSpin = new Spinner<>(2, 6, 3);
        countSpin.setPrefWidth(80);

        VBox nameFields = new VBox(10);
        nameFields.setAlignment(Pos.CENTER);
        List<TextField> fields = new ArrayList<>();
        String[] defs = {"Samuel","Abel","Dani","Player 4","Player 5","Player 6"};

        Runnable rebuild = () -> {
            nameFields.getChildren().clear(); fields.clear();
            for (int i = 0; i < countSpin.getValue(); i++) {
                TextField tf = styledField(i < defs.length ? defs[i] : "Player "+(i+1));
                Label l = new Label("Player "+(i+1)+":"); l.setTextFill(Color.WHITE);
                l.setFont(Font.font("Verdana", 12)); l.setMinWidth(70);
                nameFields.getChildren().add(centreRow(l, tf)); fields.add(tf);
            }
        };
        rebuild.run();
        countSpin.valueProperty().addListener((o,v,n) -> rebuild.run());

        Button start = buildSetupButton("Start Game", "#1e8449");
        start.setOnAction(e -> {
            game = new Game();
            isAi = new boolean[fields.size()];
            for (TextField tf : fields) {
                String n = tf.getText().trim();
                game.addPlayers(n.isEmpty() ? "Player" : n);
            }
            game.startGame();
            root = buildGameRoot();
            primaryStage.getScene().setRoot(root);
            // Local multiplayer: show hand-off before first turn
            showHandOff(game.getCurrentPlayer().getName(), () -> refresh());
        });

        Button back = buildSetupButton("Back", "#555555");
        back.setOnAction(e -> showModeSelect());

        page.getChildren().addAll(centreRow(countLbl, countSpin), nameFields, start, back);
        setScrollScene(page);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SCREEN 2c — ONLINE HOST SETUP
    // ═════════════════════════════════════════════════════════════════════════

    private void showOnlineHostSetup() {
        mode = GameMode.ONLINE_HOST;
        VBox page = setupPage("Host Online Game");

        Label myNameLbl = new Label("Your name:");
        myNameLbl.setTextFill(Color.WHITE); myNameLbl.setFont(Font.font("Verdana", 13));
        TextField nameFld = styledField("Host");

        Label countLbl = new Label("Total players (including you):");
        countLbl.setTextFill(Color.WHITE); countLbl.setFont(Font.font("Verdana", 13));
        Spinner<Integer> countSpin = new Spinner<>(2, 6, 2);
        countSpin.setPrefWidth(80);

        Label ipNote = new Label();
        ipNote.setTextFill(Color.LIGHTCYAN);
        ipNote.setFont(Font.font("Verdana", 13));
        // Use the smart IP detector — works correctly over hotspot, WiFi, and LAN
        String detectedIp = GameServer.getLocalIp();
        ipNote.setText("Your IP: " + detectedIp + "   Port: " + GameServer.PORT);

        // Also list all available IPs in a smaller label so user can confirm
        Label allIpsNote = new Label(getAllLocalIps());
        allIpsNote.setTextFill(Color.web("#cccccc"));
        allIpsNote.setFont(Font.font("Verdana", 11));
        allIpsNote.setWrapText(true);
        allIpsNote.setMaxWidth(500);

        Label statusLbl = new Label("Press Start to begin hosting...");
        statusLbl.setTextFill(Color.LIGHTGRAY); statusLbl.setFont(Font.font("Verdana", 12));

        Button start = buildSetupButton("Start Hosting", "#ca6f1e");
        start.setOnAction(e -> {
            myName = nameFld.getText().trim().isEmpty() ? "Host" : nameFld.getText().trim();
            int total = countSpin.getValue();
            try {
                server = new GameServer(total);
                server.setLogCallback(msg -> Platform.runLater(() -> statusLbl.setText(msg)));
                server.startAccepting();
                statusLbl.setText("Hosting on port " + GameServer.PORT + " - waiting for " + (total-1) + " more player(s)...");
                start.setDisable(true);
                connectAsOnlinePlayer(myName, "localhost", statusLbl);
            } catch (IOException ex) {
                statusLbl.setText("Error: " + ex.getMessage());
            }
        });

        Button back = buildSetupButton("Back", "#555555");
        back.setOnAction(e -> showModeSelect());

        page.getChildren().addAll(centreRow(myNameLbl, nameFld),
                centreRow(countLbl, countSpin), ipNote, allIpsNote, statusLbl, start, back);
        setScrollScene(page);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SCREEN 2d — ONLINE JOIN SETUP
    // ═════════════════════════════════════════════════════════════════════════

    private void showOnlineJoinSetup() {
        mode = GameMode.ONLINE_JOIN;
        VBox page = setupPage("Join Online Game");

        Label nameLbl = new Label("Your name:");
        nameLbl.setTextFill(Color.WHITE); nameLbl.setFont(Font.font("Verdana", 13));
        TextField nameFld = styledField("Player");

        Label ipLbl = new Label("Host IP address:");
        ipLbl.setTextFill(Color.WHITE); ipLbl.setFont(Font.font("Verdana", 13));
        TextField ipFld = styledField("192.168.x.x");

        Label statusLbl = new Label("");
        statusLbl.setTextFill(Color.LIGHTCYAN); statusLbl.setFont(Font.font("Verdana", 12));

        Button join = buildSetupButton("Join Game", "#6c3483");
        join.setOnAction(e -> {
            myName = nameFld.getText().trim().isEmpty() ? "Player" : nameFld.getText().trim();
            String ip = ipFld.getText().trim();
            if (ip.isEmpty()) { statusLbl.setText("Enter the host's IP address."); return; }
            statusLbl.setText("Connecting to " + ip + "...");
            join.setDisable(true);
            connectAsOnlinePlayer(myName, ip, statusLbl);
        });

        Button back = buildSetupButton("Back", "#555555");
        back.setOnAction(e -> showModeSelect());

        page.getChildren().addAll(centreRow(nameLbl, nameFld),
                centreRow(ipLbl, ipFld), statusLbl, join, back);
        setScrollScene(page);
    }

    private void connectAsOnlinePlayer(String name, String host, Label statusLbl) {
        client = new GameClient();
        client.onMessage     = msg -> Platform.runLater(() -> { if (statusLabel != null) statusLabel.setText(msg); else statusLbl.setText(msg); });
        client.onStateUpdate = s   -> Platform.runLater(() -> handleOnlineState(s));
        client.onHandUpdate  = h   -> Platform.runLater(() -> { myHand = h; refreshOnlineHand(); });
        client.onYourTurn    = msg -> Platform.runLater(() -> handleYourTurn(msg));
        client.onWinner      = w   -> Platform.runLater(() -> showOnlineWinner(w));
        client.onChooseSuit  = ()  -> Platform.runLater(this::showOnlineChooseSuit);

        new Thread(() -> {
            try {
                client.connect(host, GameServer.PORT, name);
                Platform.runLater(() -> {
                    statusLbl.setText("Connected! Waiting for game to start...");
                    showOnlineWaitingRoom(statusLbl);
                });
            } catch (IOException ex) {
                Platform.runLater(() -> statusLbl.setText("Connection failed: " + ex.getMessage()));
            }
        }, "ConnectThread").start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ONLINE GAME UI
    // ═════════════════════════════════════════════════════════════════════════

    private void showOnlineWaitingRoom(Label statusLbl) {
        // Reuse statusLbl on a waiting screen until STATE arrives and we build the board
        VBox page = setupPage("Waiting for players...");
        page.getChildren().add(statusLbl);
        primaryStage.setScene(new Scene(page, 1100, 750));
    }

    private void handleOnlineState(GameState state) {
        lastState = state;
        if (root == null) {
            root = buildGameRoot();
            primaryStage.setScene(new Scene(root, 1100, 750));
        }
        // Update top card label
        String topLabel = state.topRank + " of " + state.topSuit;
        if (playerNameLabel != null) {
            playerNameLabel.setText(state.currentPlayer + "'s Turn");
        }
        if (directionLabel != null) {
            directionLabel.setText(state.reversed ? "<<  Counter-Clockwise" : ">>  Clockwise");
        }
        // Update discard
        if (discardArea != null) {
            discardArea.getChildren().clear();
            Card fakeTop = new Card(Suit.valueOf(state.topSuit), Order.valueOf(state.topRank));
            discardArea.getChildren().add(buildCard(fakeTop, false, false));
        }
        // Update opponent bars
        if (opponentBar != null) {
            opponentBar.getChildren().clear();
            for (var entry : state.playerCardCounts.entrySet()) {
                if (entry.getKey().equals(myName)) continue;
                VBox pBox = opponentInfoBox(entry.getKey(), entry.getValue());
                opponentBar.getChildren().add(pBox);
            }
        }
        if (state.gameOver && state.winner != null) showOnlineWinner(state.winner);
    }

    private void handleYourTurn(String msg) {
        myTurn = true;
        penaltyActive = msg.contains("PENALTY");
        if (penaltyActive) {
            String[] p = msg.split("\\|");
            penaltyAmount = Integer.parseInt(p[2]);
            penaltyIsAce  = p[3].equals("ACE");
            if (statusLabel != null) statusLabel.setText(penaltyIsAce
                    ? "Ace penalty! Defend with 2 of Spades or draw " + penaltyAmount + " cards!"
                    : "TWO penalty! Stack a 2 or draw " + penaltyAmount + " cards!");
        } else {
            if (statusLabel != null) statusLabel.setText("Your turn!");
        }
        refreshOnlineHand();
        if (drawBtn != null) {
            drawBtn.setText(penaltyActive ? "Accept (" + penaltyAmount + " cards)" : "Draw Card");
            drawBtn.setDisable(false);
            passBtn.setDisable(penaltyActive);
        }
    }

    private void refreshOnlineHand() {
        if (handArea == null) return;
        handArea.getChildren().clear();
        for (CardInfo ci : myHand) {
            Card fakeCard = new Card(Suit.valueOf(ci.suit), Order.valueOf(ci.rank));
            boolean sel = (pendingSevenOnline != null && pendingSevenOnline == ci)
                    || sevenExtrasOnline.contains(ci);
            boolean canDefend = penaltyActive && (penaltyIsAce
                    ? (ci.rank.equals("TWO") && ci.suit.equals("Spades"))
                    : ci.rank.equals("TWO"));
            Pane node = buildCard(fakeCard, true, sel || canDefend);

            if (!myTurn) { node.setOpacity(0.7); }
            else if (penaltyActive && !canDefend) { node.setOpacity(0.4); }
            else {
                final CardInfo ref = ci;
                node.setOnMouseClicked(e -> onOnlineCardClicked(ref));
            }
            handArea.getChildren().add(node);
        }
    }

    private void onOnlineCardClicked(CardInfo ci) {
        if (!myTurn) return;
        if (penaltyActive) {
            client.defend(ci);
            myTurn = false; penaltyActive = false;
            return;
        }
        // Seven bundle mode
        if (ci.rank.equals("SEVEN")) {
            if (pendingSevenOnline == null) {
                pendingSevenOnline = ci;
                sevenExtrasOnline.clear();
                if (statusLabel != null) statusLabel.setText("7 selected — click same-suit extras, then click 7 again to play.");
                refreshOnlineHand();
            } else if (pendingSevenOnline == ci) {
                client.playSevenWithExtras(pendingSevenOnline, new ArrayList<>(sevenExtrasOnline));
                pendingSevenOnline = null; sevenExtrasOnline.clear();
                myTurn = false;
            }
            return;
        }
        if (pendingSevenOnline != null) {
            if (ci.suit.equals(pendingSevenOnline.suit)) {
                if (sevenExtrasOnline.contains(ci)) sevenExtrasOnline.remove(ci);
                else if (sevenExtrasOnline.size() < 4) sevenExtrasOnline.add(ci);
                refreshOnlineHand();
            }
            return;
        }
        client.playCard(ci);
        myTurn = false;
    }

    private void showOnlineChooseSuit() {
        Suit chosen = chooseSuit();
        if (chosen != null) client.chooseSuit(chosen.name());
    }

    private void showOnlineWinner(String winner) {
        if (root != null) root.setCenter(buildWinnerScreen(winner));
    }

    private VBox opponentInfoBox(String name, int count) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 18, 10, 18));
        // Semi-transparent dark card with rounded corners
        box.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.45), new CornerRadii(12), Insets.EMPTY)));
        box.setEffect(new DropShadow(6, Color.BLACK));

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.WHITE);

        // Fanned face-down cards
        HBox faceDown = new HBox(-22);
        faceDown.setAlignment(Pos.CENTER);
        int show = Math.min(count, 6);
        for (int i = 0; i < show; i++) {
            StackPane back = buildCardBack();
            // Slight fan effect
            back.setRotate((i - show / 2.0) * 4);
            faceDown.getChildren().add(back);
        }

        // Card count badge
        Label countLabel = new Label(count + " cards");
        countLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 11));
        countLabel.setTextFill(GOLD);

        box.getChildren().addAll(nameLabel, faceDown, countLabel);
        return box;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LOCAL GAME FLOW (vs Computer + Pass&Play)
    // ═════════════════════════════════════════════════════════════════════════

    private void refresh() {
        if (game.isGameOver()) { showLocalWinner(); return; }
        Player current = game.getCurrentPlayer();
        if (game.hasPendingPenalty()) { showPenaltyPrompt(); return; }

        playerNameLabel.setText(current.getName() + "'s Turn");

        // Show forced suit prominently if J or 8 is on top
        Suit forced = game.getForcedSuit();
        Card top = game.getTopCard();
        if (forced != null) {
            statusLabel.setText("Top card: " + top + "   |   Active suit: " + suitBadge(forced));
            statusLabel.setTextFill(Color.web(forced == Suit.Hearts || forced == Suit.Diamonds ? "#ff6b6b" : "#a0d8ef"));
        } else if (top.getOrder() == Order.EIGHT || top.getOrder() == Order.J) {
            statusLabel.setText("Top card: " + top + "   |   Wild card played - suit may change!");
            statusLabel.setTextFill(Color.LIGHTYELLOW);
        } else {
            statusLabel.setText("Top card: " + top);
            statusLabel.setTextFill(Color.LIGHTYELLOW);
        }
        directionLabel.setText(game.isReversed() ? "<<  Counter-Clockwise" : ">>  Clockwise");

        discardArea.getChildren().clear();
        discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));

        // If suit is forced, overlay a suit indicator on the discard area
        if (forced != null) {
            Label suitIndicator = new Label(suitBadge(forced));
            suitIndicator.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
            suitIndicator.setTextFill(Color.WHITE);
            suitIndicator.setBackground(new Background(new BackgroundFill(
                    Color.color(0,0,0,0.65), new CornerRadii(6), Insets.EMPTY)));
            suitIndicator.setPadding(new Insets(3, 8, 3, 8));
            StackPane.setAlignment(suitIndicator, Pos.BOTTOM_CENTER);
            StackPane.setMargin(suitIndicator, new Insets(0, 0, -18, 0));
            discardArea.getChildren().add(suitIndicator);
        }

        updateOpponentLabels();

        handArea.getChildren().clear();
        pendingSevenCard = null; sevenExtras.clear();
        sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false);

        for (Card card : game.getCard()) {
            Pane node = buildCard(card, true, false);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
        drawBtn.setText("Draw Card"); drawBtn.setOnAction(e -> onDraw());
        drawBtn.setDisable(false); passBtn.setDisable(false);
    }

    /** Returns a short text badge showing the suit name e.g. "Hearts" */
    private static String suitBadge(Suit suit) {
        return switch (suit) {
            case Hearts   -> "Active suit: Hearts";
            case Diamonds -> "Active suit: Diamonds";
            case Spades   -> "Active suit: Spades";
            case Clubs    -> "Active suit: Clubs";
        };
    }

    /** After each human move, auto-run all consecutive AI turns. */
    private void runAiTurnsIfNeeded() {
        if (game.isGameOver()) { showLocalWinner(); return; }
        int idx = game.getCurrentPlayerIndex();
        if (isAi != null && idx < isAi.length && isAi[idx]) {
            String aiName = game.getCurrentPlayer().getName();
            if (playerNameLabel != null) playerNameLabel.setText(aiName + "'s Turn");
            if (statusLabel != null) statusLabel.setText(aiName + " is thinking...");

            // 1.5s delay so human can see who is playing, then 0.8s after to read the result
            javafx.animation.PauseTransition think = new javafx.animation.PauseTransition(Duration.millis(1500));
            think.setOnFinished(e -> {
                if (game.isGameOver()) { showLocalWinner(); return; }
                String result = AiPlayer.takeTurn(game);
                if (statusLabel != null) statusLabel.setText(result + "   |   Top card: " + game.getTopCard());
                if (directionLabel != null)
                    directionLabel.setText(game.isReversed() ? "<<  Counter-Clockwise" : ">>  Clockwise");
                if (discardArea != null) {
                    discardArea.getChildren().clear();
                    discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));
                }
                updateOpponentLabels();
                if (game.isGameOver()) { showLocalWinner(); return; }
                // Pause so human can read the result before the next AI takes over
                javafx.animation.PauseTransition read = new javafx.animation.PauseTransition(Duration.millis(900));
                read.setOnFinished(ev -> runAiTurnsIfNeeded());
                read.play();
            });
            think.play();
        } else {
            refresh();
        }
    }

    private void advanceTurn() {
        if (mode == GameMode.LOCAL_MULTIPLAYER) {
            // Only show hand-off screen in pass-and-play mode
            showHandOff(game.getCurrentPlayer().getName(), () -> refresh());
        } else {
            // VS_COMPUTER: just run AI turns / show human's turn directly
            runAiTurnsIfNeeded();
        }
    }

    private void showHandOff(String playerName, Runnable onReady) {
        StackPane screen = new StackPane();
        screen.setBackground(feltBackground());
        screen.setAlignment(Pos.CENTER);
        VBox box = new VBox(24); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(50));
        box.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.5), new CornerRadii(20), Insets.EMPTY)));
        box.setMaxWidth(480);
        Label msg = new Label("Pass the device to"); msg.setFont(Font.font("Verdana", 16)); msg.setTextFill(Color.WHITE);
        Label name = new Label(playerName); name.setFont(Font.font("Georgia", FontWeight.BOLD, 38));
        name.setTextFill(GOLD); name.setEffect(new DropShadow(8, GOLD_DARK));
        Label hint = new Label("Press the button when ready to see your cards.");
        hint.setFont(Font.font("Verdana", 13)); hint.setTextFill(Color.LIGHTGRAY);
        hint.setWrapText(true); hint.setTextAlignment(TextAlignment.CENTER);
        Button readyBtn = buildActionButton("I'm Ready - Show My Cards", "#27ae60", "#1e8449");
        readyBtn.setPrefWidth(310);
        readyBtn.setOnAction(e -> { primaryStage.getScene().setRoot(root); onReady.run(); });
        box.getChildren().addAll(msg, name, hint, readyBtn);
        screen.getChildren().add(box);
        primaryStage.getScene().setRoot(screen);
    }

    private void updateOpponentLabels() {
        if (opponentBar == null) return;
        opponentBar.getChildren().clear();
        List<Player> players = game.getPlayers();
        int curr = game.getCurrentPlayerIndex();
        for (int i = 0; i < players.size(); i++) {
            if (i == curr) continue;
            opponentBar.getChildren().add(opponentInfoBox(players.get(i).getName(), players.get(i).getHandSize()));
        }
    }

    private void showPenaltyPrompt() {
        Player current = game.getCurrentPlayer();
        int amount = game.getPendingPenalty();
        boolean isAce = game.isActiveAce();
        playerNameLabel.setText(current.getName() + "'s Turn  [PENALTY]");
        statusLabel.setText(isAce ? "Ace of Spades! Defend with 2 of Spades or draw " + amount + " cards!"
                                  : "TWO penalty! Stack a 2 or draw " + amount + " cards!");
        directionLabel.setText("");
        discardArea.getChildren().clear();
        discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));
        updateOpponentLabels();
        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            boolean canDefend = isAce ? (card.getOrder()==Order.TWO && card.getSuit()==Suit.Spades)
                                      : (card.getOrder()==Order.TWO);
            Pane node = buildCard(card, true, canDefend);
            if (canDefend) { node.setOnMouseClicked(e -> { boolean ok = isAce ? game.defendWithTwoOfSpades(card) : game.defendWithTwo(card); if (ok) { game.moveToNext(); advanceTurn(); } }); }
            else { node.setOpacity(0.45); }
            handArea.getChildren().add(node);
        }
        drawBtn.setText("Accept (" + amount + " cards)");
        drawBtn.setDisable(false);
        drawBtn.setOnAction(e -> {
            game.acceptPenalty();
            drawBtn.setText("Draw Card");
            drawBtn.setOnAction(ev -> onDraw());
            if (game.isGameOver()) { showLocalWinner(); return; }
            // Stay on this player's turn — they get to play after drawing penalty
            statusLabel.setText("Penalty accepted. Play a card, draw one more, or pass.");
            statusLabel.setTextFill(Color.LIGHTYELLOW);
            refresh();
        });
        passBtn.setDisable(true);
    }

    private void onCardClicked(Card card) {
        if (game.isGameOver()) return;
        if (pendingSevenCard != null) {
            if (card == pendingSevenCard) { commitSevenPlay(); return; }
            if (card.getSuit() == pendingSevenCard.getSuit()) toggleSevenExtra(card);
            else statusLabel.setText("Only " + pendingSevenCard.getSuit() + " cards can bundle with 7.");
            return;
        }
        if (card.getOrder() == Order.SEVEN && game.isValidMove(card)) { enterSevenBundleMode(card); return; }
        attemptPlay(card);
    }

    private void enterSevenBundleMode(Card seven) {
        pendingSevenCard = seven; sevenExtras.clear(); refreshHandHighlights();
        sevenBundleBar.getChildren().clear();
        Label hint = new Label("7 selected - click same-suit extras to bundle (max 4), then click 7 again to play");
        hint.setTextFill(GOLD); hint.setFont(Font.font("Verdana", 12));
        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;");
        cancel.setOnAction(e -> { pendingSevenCard = null; sevenExtras.clear(); sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false); refreshHandHighlights(); statusLabel.setText(""); });
        sevenBundleBar.getChildren().addAll(hint, cancel);
        sevenBundleBar.setVisible(true); sevenBundleBar.setManaged(true);
        statusLabel.setText("Bundle mode: select extras then click 7 to confirm.");
    }

    private void toggleSevenExtra(Card card) {
        if (sevenExtras.contains(card)) sevenExtras.remove(card);
        else { if (sevenExtras.size() >= 4) { statusLabel.setText("Max 4 extras."); return; } sevenExtras.add(card); }
        refreshHandHighlights();
        statusLabel.setText("Extras: " + sevenExtras.size() + " — click 7 to confirm.");
    }

    private void commitSevenPlay() {
        boolean ok = game.playSevenWithExtras(pendingSevenCard, new ArrayList<>(sevenExtras));
        if (ok) { pendingSevenCard = null; sevenExtras.clear(); sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false); afterSuccessfulPlay(null); }
        else statusLabel.setText("Invalid play.");
    }

    private void refreshHandHighlights() {
        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            Pane node = buildCard(card, true, card == pendingSevenCard || sevenExtras.contains(card));
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
    }

    private void attemptPlay(Card card) {
        if (game.playCard(card)) { lastPlayInvalid = false; afterSuccessfulPlay(card); }
        else { lastPlayInvalid = true; statusLabel.setText("Invalid move! Others can say CRAZY to penalise you, or draw."); TranslateTransition tt = new TranslateTransition(Duration.millis(60), statusLabel); tt.setFromX(-6); tt.setToX(6); tt.setCycleCount(4); tt.setAutoReverse(true); tt.play(); }
    }

    private void afterSuccessfulPlay(Card card) {
        if (card != null && (card.getOrder()==Order.EIGHT || card.getOrder()==Order.J)) {
            if (game.canChangeSuit()) {
                Suit chosen = chooseSuit();
                if (chosen != null) { game.setSuit(chosen); statusLabel.setText("Suit changed to " + chosen); }
            } else {
                statusLabel.setText("Cannot change suit twice in a row with 3+ players.");
            }
        }
        if (game.isGameOver()) { showLocalWinner(); return; }
        advanceTurn();
    }

    private void onDraw() {
        if (game.isGameOver()) return;
        if (game.hasDrawn()) { statusLabel.setText("Already drew. Play or pass."); return; }
        lastPlayInvalid = false;
        Card drawn = game.drawForCurrentPlayer();
        if (drawn != null) { game.setHasDrawn(true); statusLabel.setText("Drew: " + drawn); }
        handArea.getChildren().clear();
        for (Card card : game.getCard()) { Pane node = buildCard(card, true, false); node.setOnMouseClicked(e -> onCardClicked(card)); handArea.getChildren().add(node); }
        updateOpponentLabels();
    }

    private void onPass() {
        if (game.isGameOver()) return;
        lastPlayInvalid = false; game.setHasDrawn(false); game.moveToNext(); advanceTurn();
    }

    private void onCrazy() {
        if (!lastPlayInvalid) { statusLabel.setText("No invalid play to challenge!"); return; }
        game.applyCrazyPenalty(); lastPlayInvalid = false;
        statusLabel.setText("CRAZY! " + game.getCurrentPlayer().getName() + " draws 2 cards.");
        handArea.getChildren().clear();
        for (Card card : game.getCard()) { Pane node = buildCard(card, true, false); node.setOnMouseClicked(e -> onCardClicked(card)); handArea.getChildren().add(node); }
        updateOpponentLabels();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SHARED UI BUILDERS
    // ═════════════════════════════════════════════════════════════════════════

    private BorderPane buildGameRoot() {
        BorderPane bp = new BorderPane();
        bp.setBackground(feltBackground());
        bp.setTop(buildTopBar());

        // Centre: opponents top + discard in true centre
        BorderPane centre = new BorderPane();
        centre.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        // Opponents strip at the top of the centre area
        opponentBar = new HBox(24);
        opponentBar.setAlignment(Pos.CENTER);
        opponentBar.setPadding(new Insets(14, 20, 10, 20));
        centre.setTop(opponentBar);

        // Discard pile truly centred
        discardArea = new StackPane();
        discardArea.setAlignment(Pos.CENTER);
        discardArea.setPrefSize(130, 185);
        centre.setCenter(discardArea);

        bp.setCenter(centre);
        VBox.setVgrow(centre, Priority.ALWAYS);

        VBox bottom = buildBottomArea();
        bottom.setMinHeight(220);
        bp.setBottom(bottom);
        return bp;
    }

    private VBox buildTopBar() {
        VBox bar = new VBox(3); bar.setAlignment(Pos.CENTER); bar.setPadding(new Insets(10,20,6,20));
        // Top row: title + main menu button
        Label title = new Label("CRAZY CARD GAME"); title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setTextFill(GOLD); title.setEffect(new DropShadow(4, GOLD_DARK));
        Button menuBtn = new Button("Main Menu");
        menuBtn.setFont(Font.font("Verdana", FontWeight.BOLD, 11));
        menuBtn.setTextFill(Color.web("#1a1a1a"));
        menuBtn.setBackground(new Background(new BackgroundFill(Color.web("#f0f0f0"), new CornerRadii(6), Insets.EMPTY)));
        menuBtn.setPadding(new Insets(5, 12, 5, 12));
        menuBtn.setEffect(new DropShadow(3, Color.color(0,0,0,0.3)));
        menuBtn.setOnMouseEntered(e -> menuBtn.setBackground(new Background(new BackgroundFill(Color.web("#cccccc"), new CornerRadii(6), Insets.EMPTY))));
        menuBtn.setOnMouseExited( e -> menuBtn.setBackground(new Background(new BackgroundFill(Color.web("#f0f0f0"), new CornerRadii(6), Insets.EMPTY))));
        menuBtn.setOnAction(e -> { if(server!=null){server.stop();server=null;} if(client!=null){client.disconnect();client=null;} showModeSelect(); });
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);
        // Put menu button on right
        Region spacerL = new Region(); HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region(); HBox.setHgrow(spacerR, Priority.ALWAYS);
        HBox fullTitleRow = new HBox(10, menuBtn, spacerL, title, spacerR);
        fullTitleRow.setAlignment(Pos.CENTER_LEFT);

        playerNameLabel = new Label(); playerNameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 15)); playerNameLabel.setTextFill(Color.WHITE);
        statusLabel = new Label(); statusLabel.setFont(Font.font("Verdana", 12)); statusLabel.setTextFill(Color.LIGHTYELLOW);
        directionLabel = new Label(); directionLabel.setFont(Font.font("Verdana", 11)); directionLabel.setTextFill(Color.LIGHTCYAN);
        Region div = new Region(); div.setPrefHeight(2);
        div.setBackground(new Background(new BackgroundFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE, new Stop(0,Color.TRANSPARENT),new Stop(0.5,GOLD),new Stop(1,Color.TRANSPARENT)), CornerRadii.EMPTY, Insets.EMPTY)));
        bar.getChildren().addAll(fullTitleRow, playerNameLabel, statusLabel, directionLabel, div);
        return bar;
    }

    // buildCentreArea kept for reference — opponents and discard are now built inline in buildGameRoot()
    private StackPane buildCentreArea() {
        StackPane stack = new StackPane(); stack.setAlignment(Pos.CENTER);
        discardArea = new StackPane(); discardArea.setAlignment(Pos.CENTER); discardArea.setPrefSize(130,185);
        opponentBar = new HBox(20); opponentBar.setAlignment(Pos.CENTER);
        StackPane.setAlignment(opponentBar, Pos.TOP_CENTER); StackPane.setMargin(opponentBar, new Insets(10,0,0,0));
        stack.getChildren().addAll(opponentBar, discardArea);
        VBox.setVgrow(stack, Priority.ALWAYS);
        return stack;
    }

    private VBox buildBottomArea() {
        VBox box = new VBox(6); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(6,16,12,16));
        box.setMaxWidth(Double.MAX_VALUE);
        sevenBundleBar = new HBox(10); sevenBundleBar.setAlignment(Pos.CENTER); sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false);

        handArea = new HBox(8); handArea.setAlignment(Pos.CENTER); handArea.setPadding(new Insets(6));
        ScrollPane scroll = new ScrollPane(handArea);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        scroll.setPrefHeight(148);
        scroll.setMinHeight(148);
        scroll.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(scroll, Priority.ALWAYS);

        HBox handPanel = new HBox(scroll);
        handPanel.setAlignment(Pos.CENTER);
        handPanel.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.28), new CornerRadii(10), Insets.EMPTY)));
        handPanel.setPadding(new Insets(6));
        HBox.setHgrow(scroll, Priority.ALWAYS);
        handPanel.setMaxWidth(Double.MAX_VALUE);

        drawBtn = buildActionButton("Draw Card","#2196F3","#1565C0");
        passBtn = buildActionButton("Pass Turn","#FF9800","#E65100");
        Button crazyBtn = buildActionButton("CRAZY!","#8e24aa","#6a1b9a");

        drawBtn.setOnAction(e -> { if (mode==GameMode.ONLINE_HOST||mode==GameMode.ONLINE_JOIN) { if (penaltyActive) { client.acceptPenalty(); myTurn=false; penaltyActive=false; drawBtn.setText("Draw Card"); } else client.drawCard(); } else onDraw(); });
        passBtn.setOnAction(e -> { if (mode==GameMode.ONLINE_HOST||mode==GameMode.ONLINE_JOIN) client.pass(); else onPass(); });
        crazyBtn.setOnAction(e -> onCrazy());

        HBox btnRow = new HBox(16, drawBtn, passBtn, crazyBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setMinHeight(50);

        box.getChildren().addAll(sevenBundleBar, handPanel, btnRow);
        return box;
    }

    private Suit chooseSuit() {
        Dialog<Suit> dialog = new Dialog<>(); dialog.setTitle("Choose Suit"); dialog.setHeaderText("Pick the suit:");
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(16); grid.setAlignment(Pos.CENTER); grid.setPadding(new Insets(20)); grid.setStyle("-fx-background-color:#1a6b3c;");
        final Suit[] chosen = {null};
        Suit[] suits = Suit.values();
        for (int i = 0; i < suits.length; i++) { Suit s = suits[i]; Pane btn = buildSuitPickerCard(s); btn.setOnMouseClicked(e -> { chosen[0]=s; dialog.close(); }); grid.add(btn,i%2,i/2); }
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().setStyle("-fx-background-color:#1a6b3c;"); dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait(); return chosen[0];
    }

    private Pane buildSuitPickerCard(Suit suit) {
        Pane sp = new Pane(); sp.setPrefSize(110,90);
        sp.setBackground(new Background(new BackgroundFill(CARD_WHITE, new CornerRadii(10), Insets.EMPTY)));
        sp.setEffect(new DropShadow(6, Color.BLACK));
        javafx.scene.Node shape = buildSuitShape(suit, 36); shape.setLayoutX(37); shape.setLayoutY(8);
        Text name = new Text(suit.name()); name.setFont(Font.font("Verdana", FontWeight.BOLD, 13)); name.setFill(suitColor(suit)); name.setLayoutX(30); name.setLayoutY(70);
        sp.getChildren().addAll(shape, name);
        sp.setOnMouseEntered(e -> { ScaleTransition st=new ScaleTransition(Duration.millis(100),sp); st.setToX(1.08); st.setToY(1.08); st.play(); });
        sp.setOnMouseExited( e -> { ScaleTransition st=new ScaleTransition(Duration.millis(100),sp); st.setToX(1.0);  st.setToY(1.0);  st.play(); });
        sp.setStyle("-fx-cursor:hand;"); return sp;
    }

    private void showLocalWinner() {
        if (game.getWinner() == null) return;
        root.setCenter(buildWinnerScreen(game.getWinner().getName()));
        if (drawBtn!=null) drawBtn.setDisable(true); if(passBtn!=null) passBtn.setDisable(true);
        if (playerNameLabel!=null) playerNameLabel.setText("Game Over!"); if(statusLabel!=null) statusLabel.setText(""); if(directionLabel!=null) directionLabel.setText("");
        primaryStage.getScene().setRoot(root);
    }

    private StackPane buildWinnerScreen(String name) {
        StackPane sp = new StackPane(); sp.setAlignment(Pos.CENTER); sp.setBackground(feltBackground());
        VBox box = new VBox(20); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(50));
        box.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.6), new CornerRadii(20), Insets.EMPTY)));
        box.setMaxWidth(480); box.setMaxHeight(340);
        Label win = new Label(name + " Wins!"); win.setFont(Font.font("Georgia", FontWeight.BOLD, 38)); win.setTextFill(GOLD); win.setEffect(new DropShadow(10, GOLD_DARK));
        Label sub = new Label("Congratulations!"); sub.setFont(Font.font("Verdana", 18)); sub.setTextFill(Color.WHITE);
        Button again = buildSetupButton("Main Menu", "#1e8449");
        again.setPrefWidth(180);
        again.setOnAction(e -> { if(server!=null){server.stop();server=null;} if(client!=null){client.disconnect();client=null;} showModeSelect(); });
        box.getChildren().addAll(win, sub, again); sp.getChildren().add(box); return sp;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARD RENDERING
    // ═════════════════════════════════════════════════════════════════════════

    private Pane buildCard(Card card, boolean isHand, boolean selected) {
        double W=isHand?82:110, H=isHand?122:160, pip=isHand?11:15, rFont=isHand?15:20;
        Pane pane = new Pane(); pane.setPrefSize(W,H); pane.setMinSize(W,H); pane.setMaxSize(W,H);
        Rectangle body = new Rectangle(1,1,W-2,H-2); body.setArcWidth(10); body.setArcHeight(10); body.setFill(CARD_WHITE);
        body.setEffect(new DropShadow(selected?16:7, selected?GOLD:Color.color(0,0,0,0.5)));
        body.setStroke(selected?GOLD:Color.web("#999999")); body.setStrokeWidth(selected?2.5:0.8);
        pane.getChildren().add(body);
        Color sc = suitColor(card.getSuit()); String rank = rankLabel(card.getOrder());
        Text tl = new Text(rank); tl.setFont(Font.font("Arial",FontWeight.BOLD,rFont)); tl.setFill(sc); tl.setLayoutX(5); tl.setLayoutY(rFont+2); pane.getChildren().add(tl);
        double sSize=rFont*0.75; javafx.scene.Node ptl=buildSuitShape(card.getSuit(),sSize); ptl.setLayoutX(6); ptl.setLayoutY(rFont+5); pane.getChildren().add(ptl);
        Text br = new Text(rank); br.setFont(Font.font("Arial",FontWeight.BOLD,rFont)); br.setFill(sc); br.setRotate(180); br.setLayoutX(W-rFont-2); br.setLayoutY(H-6); pane.getChildren().add(br);
        javafx.scene.Node pbr=buildSuitShape(card.getSuit(),sSize); pbr.setRotate(180); pbr.setLayoutX(W-sSize-6); pbr.setLayoutY(H-rFont-sSize-4); pane.getChildren().add(pbr);
        buildCentreOnPane(pane, card, W, H, pip);
        if (isHand) { pane.setStyle("-fx-cursor:hand;");
            pane.setOnMouseEntered(e -> { TranslateTransition tt=new TranslateTransition(Duration.millis(110),pane); tt.setToY(-12); tt.play(); });
            pane.setOnMouseExited( e -> { TranslateTransition tt=new TranslateTransition(Duration.millis(110),pane); tt.setToY(0);   tt.play(); }); }
        return pane;
    }

    private void buildCentreOnPane(Pane pane, Card card, double W, double H, double pip) {
        Color c = suitColor(card.getSuit()); Order order = card.getOrder();
        if (order==Order.J||order==Order.Q||order==Order.K) { Text f=new Text(rankLabel(order)); double fs=H*0.42; f.setFont(Font.font("Georgia",FontWeight.BOLD,FontPosture.ITALIC,fs)); f.setFill(c); f.setLayoutX(W*0.5-fs*0.27); f.setLayoutY(H*0.5+fs*0.35); pane.getChildren().add(f); return; }
        if (order==Order.ONE) { double sz=pip*3.0; javafx.scene.Node sh=buildSuitShape(card.getSuit(),sz); sh.setLayoutX(W/2-sz/2); sh.setLayoutY(H/2-sz/2); pane.getChildren().add(sh); return; }
        int count=orderToInt(order); double[][]pos=pipPositions(count,W,H,pip);
        for (double[]p:pos) { javafx.scene.Node sh=buildSuitShape(card.getSuit(),pip); if(p[2]==1)sh.setRotate(180); sh.setLayoutX(p[0]); sh.setLayoutY(p[1]); pane.getChildren().add(sh); }
    }

    private double[][] pipPositions(int n, double W, double H, double p) {
        double lx=W*0.18,cx=W*0.50-p/2,rx=W*0.68,r1=H*0.12,r2=H*0.29,r3=H*0.48,r4=H*0.64,r5=H*0.80,rm=H*0.38,rn=H*0.55;
        return switch(n){case 2->new double[][]{{cx,r1,0},{cx,r5,1}};case 3->new double[][]{{cx,r1,0},{cx,r3,0},{cx,r5,1}};case 4->new double[][]{{lx,r1,0},{rx,r1,0},{lx,r5,1},{rx,r5,1}};case 5->new double[][]{{lx,r1,0},{rx,r1,0},{cx,r3,0},{lx,r5,1},{rx,r5,1}};case 6->new double[][]{{lx,r1,0},{rx,r1,0},{lx,r3,0},{rx,r3,0},{lx,r5,1},{rx,r5,1}};case 7->new double[][]{{lx,r1,0},{rx,r1,0},{cx,r2,0},{lx,r3,0},{rx,r3,0},{lx,r5,1},{rx,r5,1}};case 8->new double[][]{{lx,r1,0},{rx,r1,0},{cx,r2,0},{lx,r3,0},{rx,r3,0},{cx,r4,1},{lx,r5,1},{rx,r5,1}};case 9->new double[][]{{lx,r1,0},{rx,r1,0},{lx,rm,0},{rx,rm,0},{cx,r3,0},{lx,rn,1},{rx,rn,1},{lx,r5,1},{rx,r5,1}};case 10->new double[][]{{lx,r1,0},{rx,r1,0},{cx,r2,0},{lx,rm,0},{rx,rm,0},{lx,rn,1},{rx,rn,1},{cx,r4,1},{lx,r5,1},{rx,r5,1}};default->new double[][]{{cx,r3,0}};};
    }

    private int orderToInt(Order o) { return switch(o){case TWO->2;case THREE->3;case FOUR->4;case FIVE->5;case SIX->6;case SEVEN->7;case EIGHT->8;case NINE->9;case TEN->10;default->1;}; }

    private StackPane buildCardBack() {
        StackPane sp=new StackPane(); sp.setPrefSize(36,52); sp.setMinSize(36,52);
        Rectangle r=new Rectangle(36,52,Color.web("#1a237e")); r.setArcWidth(6); r.setArcHeight(6); r.setStroke(Color.WHITE); r.setStrokeWidth(1);
        Rectangle in=new Rectangle(4,4,28,44); in.setFill(Color.TRANSPARENT); in.setStroke(Color.web("#3949ab")); in.setStrokeWidth(1);
        sp.getChildren().addAll(r,in); return sp;
    }

    private javafx.scene.Node buildSuitShape(Suit suit, double size) { return switch(suit){case Hearts->heartShape(size,suitColor(suit));case Diamonds->diamondShape(size,suitColor(suit));case Spades->spadeShape(size,suitColor(suit));case Clubs->clubShape(size,suitColor(suit));}; }

    private javafx.scene.Node heartShape(double s,Color c){Path p=new Path();p.setFill(c);p.setStroke(Color.TRANSPARENT);double cx=s/2,tip=s*.95,lx=s*.05,topY=s*.22,ly=s*.40;p.getElements().addAll(new MoveTo(cx,tip),new CubicCurveTo(cx-s*.18,tip-s*.25,lx,ly+s*.10,lx,topY),new CubicCurveTo(lx,s*.02,s*.35,s*.02,cx,ly),new CubicCurveTo(s*.65,s*.02,s*.95,s*.02,s*.95,topY),new CubicCurveTo(s*.95,ly+s*.10,cx+s*.18,tip-s*.25,cx,tip),new ClosePath());return p;}
    private javafx.scene.Node diamondShape(double s,Color c){Polygon p=new Polygon(s*.50,s*.02,s*.97,s*.50,s*.50,s*.98,s*.03,s*.50);p.setFill(c);p.setStroke(Color.TRANSPARENT);return p;}
    private javafx.scene.Node spadeShape(double s,Color c){Group g=new Group();double cx=s/2,tip=s*.05,lx=s*.05,rx=s*.95,ly=s*.62;Path b=new Path();b.setFill(c);b.setStroke(Color.TRANSPARENT);b.getElements().addAll(new MoveTo(cx,tip),new CubicCurveTo(cx-s*.18,tip+s*.25,lx,ly-s*.10,lx,s*.78),new CubicCurveTo(lx,s*.96,s*.35,s*.96,cx,ly),new CubicCurveTo(s*.65,s*.96,rx,s*.96,rx,s*.78),new CubicCurveTo(rx,ly-s*.10,cx+s*.18,tip+s*.25,cx,tip),new ClosePath());Polygon st=new Polygon(cx-s*.18,s*.72,cx+s*.18,s*.72,cx+s*.08,s*.95,cx-s*.08,s*.95);st.setFill(c);st.setStroke(Color.TRANSPARENT);g.getChildren().addAll(b,st);return g;}
    private javafx.scene.Node clubShape(double s,Color c){Group g=new Group();double r=s*.22;Circle t=new Circle(s*.50,s*.22,r,c);Circle l=new Circle(s*.25,s*.52,r,c);Circle ri=new Circle(s*.75,s*.52,r,c);Polygon st=new Polygon(s*.42,s*.60,s*.58,s*.60,s*.65,s*.95,s*.35,s*.95);st.setFill(c);st.setStroke(Color.TRANSPARENT);g.getChildren().addAll(st,l,ri,t);return g;}

    private static Color suitColor(Suit s){return switch(s){case Hearts,Diamonds->RED_SUIT;case Spades,Clubs->BLACK_SUIT;};}
    private static String rankLabel(Order o){return switch(o){case ONE->"A";case TWO->"2";case THREE->"3";case FOUR->"4";case FIVE->"5";case SIX->"6";case SEVEN->"7";case EIGHT->"8";case NINE->"9";case TEN->"10";case J->"J";case Q->"Q";case K->"K";};}

    // ── utility builders ─────────────────────────────────────────────────────
    private VBox setupPage(String heading) {
        VBox page=new VBox(20);page.setAlignment(Pos.CENTER);page.setBackground(feltBackground());page.setPadding(new Insets(50));
        Label t=new Label(heading);t.setFont(Font.font("Georgia",FontWeight.BOLD,30));t.setTextFill(GOLD);t.setEffect(new DropShadow(6,GOLD_DARK));
        page.getChildren().add(t); return page;
    }
    private HBox centreRow(javafx.scene.Node... nodes){HBox r=new HBox(10);r.setAlignment(Pos.CENTER);r.getChildren().addAll(nodes);return r;}
    private TextField styledField(String def){TextField tf=new TextField(def);tf.setFont(Font.font("Verdana",14));tf.setMaxWidth(260);tf.setStyle("-fx-background-radius:8;-fx-padding:8;");return tf;}

    /** Buttons inside setup screens — white background, dark text, clear and readable on the felt. */
    private Button buildSetupButton(String text, String textColor) {
        Button b = new Button(text);
        b.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        b.setTextFill(Color.web(textColor));
        b.setPrefWidth(180);
        b.setPrefHeight(42);
        b.setBackground(new Background(new BackgroundFill(Color.web("#ffffff"), new CornerRadii(10), Insets.EMPTY)));
        b.setEffect(new DropShadow(5, Color.color(0,0,0,0.35)));
        b.setOnMouseEntered(e -> b.setBackground(new Background(new BackgroundFill(Color.web("#e0e0e0"), new CornerRadii(10), Insets.EMPTY))));
        b.setOnMouseExited( e -> b.setBackground(new Background(new BackgroundFill(Color.web("#ffffff"), new CornerRadii(10), Insets.EMPTY))));
        return b;
    }
    private Button buildActionButton(String text,String base,String hover){
        Button b=new Button(text);
        b.setFont(Font.font("Verdana",FontWeight.BOLD,13));
        b.setTextFill(Color.WHITE);
        b.setPrefWidth(145);
        b.setPrefHeight(40);
        b.setBackground(new Background(new BackgroundFill(Color.web(base),new CornerRadii(8),Insets.EMPTY)));
        b.setEffect(new DropShadow(4,Color.BLACK));
        b.setOnMouseEntered(e->b.setBackground(new Background(new BackgroundFill(Color.web(hover),new CornerRadii(8),Insets.EMPTY))));
        b.setOnMouseExited(e->b.setBackground(new Background(new BackgroundFill(Color.web(base),new CornerRadii(8),Insets.EMPTY))));
        return b;
    }
    private Background feltBackground(){return new Background(new BackgroundFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,FELT_DARK),new Stop(0.5,FELT_MID),new Stop(1,FELT_LIGHT)),CornerRadii.EMPTY,Insets.EMPTY));}

    /**
     * Returns a string listing all active non-loopback IPv4 addresses on this machine.
     * Shown on the host screen so the user can confirm which IP their friends should use.
     */
    private static String getAllLocalIps() {
        StringBuilder sb = new StringBuilder("All network IPs on this device: ");
        try {
            boolean found = false;
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    if (addr.getAddress().length != 4) continue; // IPv4 only
                    sb.append(ni.getDisplayName().split(" ")[0])
                      .append(": ").append(addr.getHostAddress()).append("   ");
                    found = true;
                }
            }
            if (!found) sb.append("(none found)");
        } catch (Exception e) {
            sb.append("(error reading interfaces)");
        }
        return sb.toString();
    }

    /** Wraps a VBox content page in a ScrollPane and sets it as the current scene root.
     *  This ensures setup screens never clip content when the window is small. */
    private void setScrollScene(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(sp, 1100, 750));
        } else {
            primaryStage.getScene().setRoot(sp);
        }
    }

    public static void main(String[] args){launch(args);}
}
