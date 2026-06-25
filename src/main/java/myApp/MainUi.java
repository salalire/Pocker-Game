package myApp;

import java.util.ArrayList;
import java.util.List;

import game.Card;
import game.Game;
import game.Order;
import game.Player;
import game.Suit;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
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

    // ── state ─────────────────────────────────────────────────────────────────
    private Game game;
    private Stage primaryStage;
    private BorderPane root;
    private HBox handArea;
    private StackPane discardArea;
    private HBox opponentBar;
    private Label statusLabel;
    private Label directionLabel;
    private Label playerNameLabel;
    private Button drawBtn;
    private Button passBtn;
    private final List<Card> sevenExtras = new ArrayList<>();
    private Card pendingSevenCard = null;
    private HBox sevenBundleBar;
    private boolean lastPlayInvalid = false;

    // ═════════════════════════════════════════════════════════════════════════
    //  STARTUP: name entry screen → game
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Crazy Card Game");
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        showNameEntry();
        stage.show();
    }

    /** Full-screen name entry: player count selector + name fields. */
    private void showNameEntry() {
        VBox page = new VBox(24);
        page.setAlignment(Pos.CENTER);
        page.setBackground(feltBackground());
        page.setPadding(new Insets(50));

        Label title = new Label("CRAZY CARD GAME");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        title.setTextFill(GOLD);
        title.setEffect(new DropShadow(6, GOLD_DARK));

        Label sub = new Label("Enter player names to begin");
        sub.setFont(Font.font("Verdana", 15));
        sub.setTextFill(Color.WHITE);

        // Player count chooser
        Label countLbl = new Label("Number of players:");
        countLbl.setTextFill(Color.WHITE);
        countLbl.setFont(Font.font("Verdana", FontWeight.BOLD, 13));

        Spinner<Integer> countSpinner = new Spinner<>(2, 6, 3);
        countSpinner.setPrefWidth(80);
        countSpinner.setStyle("-fx-font-size:14;");

        HBox countRow = new HBox(12, countLbl, countSpinner);
        countRow.setAlignment(Pos.CENTER);

        // Dynamic name fields
        VBox nameFields = new VBox(10);
        nameFields.setAlignment(Pos.CENTER);
        nameFields.setMaxWidth(320);

        List<TextField> fields = new ArrayList<>();
        String[] defaults = {"Samuel", "Abel", "Dani", "Player 4", "Player 5", "Player 6"};

        Runnable rebuildFields = () -> {
            nameFields.getChildren().clear();
            fields.clear();
            int n = countSpinner.getValue();
            for (int i = 0; i < n; i++) {
                TextField tf = new TextField(i < defaults.length ? defaults[i] : "Player " + (i+1));
                tf.setFont(Font.font("Verdana", 14));
                tf.setMaxWidth(280);
                tf.setStyle("-fx-background-radius:8;-fx-padding:8;");
                Label lbl = new Label("Player " + (i + 1) + ":");
                lbl.setTextFill(Color.WHITE);
                lbl.setFont(Font.font("Verdana", 12));
                lbl.setMinWidth(70);
                HBox row = new HBox(10, lbl, tf);
                row.setAlignment(Pos.CENTER);
                nameFields.getChildren().add(row);
                fields.add(tf);
            }
        };

        rebuildFields.run();
        countSpinner.valueProperty().addListener((obs, o, n) -> rebuildFields.run());

        Button startBtn = buildActionButton("Start Game", "#27ae60", "#1e8449");
        startBtn.setPrefWidth(180);
        startBtn.setOnAction(e -> {
            game = new Game();
            for (TextField tf : fields) {
                String name = tf.getText().trim();
                game.addPlayers(name.isEmpty() ? "Player" : name);
            }
            game.startGame();
            root = buildGameRoot();
            Scene scene = new Scene(root, 1100, 750);
            primaryStage.setScene(scene);
            // Show hand-off screen for first player
            showHandOff(game.getCurrentPlayer().getName(), () -> refresh());
        });

        page.getChildren().addAll(title, sub, countRow, nameFields, startBtn);
        primaryStage.setScene(new Scene(page, 1100, 750));
    }

    /**
     * "Pass the device" screen shown between turns.
     * Cards are hidden until the correct player confirms they are ready.
     */
    private void showHandOff(String playerName, Runnable onReady) {
        StackPane screen = new StackPane();
        screen.setBackground(feltBackground());
        screen.setAlignment(Pos.CENTER);

        VBox box = new VBox(24);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        box.setBackground(new Background(new BackgroundFill(
                Color.color(0,0,0,0.5), new CornerRadii(20), Insets.EMPTY)));
        box.setMaxWidth(480);

        Label msg = new Label("Pass the device to");
        msg.setFont(Font.font("Verdana", 16));
        msg.setTextFill(Color.WHITE);

        Label name = new Label(playerName);
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 38));
        name.setTextFill(GOLD);
        name.setEffect(new DropShadow(8, GOLD_DARK));

        Label hint = new Label("Press the button when you are ready to see your cards.");
        hint.setFont(Font.font("Verdana", 13));
        hint.setTextFill(Color.LIGHTGRAY);
        hint.setWrapText(true);
        hint.setTextAlignment(TextAlignment.CENTER);

        Button readyBtn = buildActionButton("I'm Ready — Show My Cards", "#27ae60", "#1e8449");
        readyBtn.setPrefWidth(310);
        readyBtn.setOnAction(e -> {
            primaryStage.getScene().setRoot(root);
            onReady.run();
        });

        box.getChildren().addAll(msg, name, hint, readyBtn);
        screen.getChildren().add(box);

        primaryStage.getScene().setRoot(screen);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GAME ROOT LAYOUT
    // ═════════════════════════════════════════════════════════════════════════

    private BorderPane buildGameRoot() {
        BorderPane bp = new BorderPane();
        bp.setBackground(feltBackground());
        bp.setTop(buildTopBar());
        bp.setCenter(buildCentreArea());
        bp.setBottom(buildBottomArea());
        return bp;
    }

    private Background feltBackground() {
        return new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, FELT_DARK), new Stop(0.5, FELT_MID), new Stop(1, FELT_LIGHT)),
                CornerRadii.EMPTY, Insets.EMPTY));
    }

    private VBox buildTopBar() {
        VBox bar = new VBox(4);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(14, 20, 8, 20));

        Label title = new Label("CRAZY CARD GAME");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setTextFill(GOLD);
        title.setEffect(new DropShadow(4, GOLD_DARK));

        playerNameLabel = new Label();
        playerNameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        playerNameLabel.setTextFill(Color.WHITE);

        statusLabel = new Label();
        statusLabel.setFont(Font.font("Verdana", 13));
        statusLabel.setTextFill(Color.LIGHTYELLOW);

        directionLabel = new Label();
        directionLabel.setFont(Font.font("Verdana", 12));
        directionLabel.setTextFill(Color.LIGHTCYAN);

        Region divider = new Region();
        divider.setPrefHeight(2);
        divider.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.TRANSPARENT), new Stop(0.5, GOLD), new Stop(1, Color.TRANSPARENT)),
                CornerRadii.EMPTY, Insets.EMPTY)));

        bar.getChildren().addAll(title, playerNameLabel, statusLabel, directionLabel, divider);
        return bar;
    }

    private StackPane buildCentreArea() {
        StackPane stack = new StackPane();
        stack.setAlignment(Pos.CENTER);

        discardArea = new StackPane();
        discardArea.setAlignment(Pos.CENTER);
        discardArea.setPrefSize(130, 185);

        opponentBar = new HBox(20);
        opponentBar.setAlignment(Pos.CENTER);
        StackPane.setAlignment(opponentBar, Pos.TOP_CENTER);
        StackPane.setMargin(opponentBar, new Insets(10, 0, 0, 0));

        stack.getChildren().addAll(opponentBar, discardArea);
        VBox.setVgrow(stack, Priority.ALWAYS);
        return stack;
    }

    private VBox buildBottomArea() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 20, 16, 20));

        sevenBundleBar = new HBox(10);
        sevenBundleBar.setAlignment(Pos.CENTER);
        sevenBundleBar.setVisible(false);
        sevenBundleBar.setManaged(false);

        handArea = new HBox(10);
        handArea.setAlignment(Pos.CENTER);
        handArea.setPadding(new Insets(8));

        ScrollPane scroll = new ScrollPane(handArea);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        scroll.setPrefHeight(165);
        scroll.setMaxWidth(Double.MAX_VALUE);

        HBox handPanel = new HBox(scroll);
        handPanel.setAlignment(Pos.CENTER);
        handPanel.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.3), new CornerRadii(12), Insets.EMPTY)));
        handPanel.setPadding(new Insets(8));
        HBox.setHgrow(scroll, Priority.ALWAYS);
        handPanel.setMaxWidth(Double.MAX_VALUE);

        drawBtn = buildActionButton("Draw Card", "#2196F3", "#1565C0");
        passBtn = buildActionButton("Pass Turn", "#FF9800", "#E65100");
        Button crazyBtn = buildActionButton("CRAZY!", "#8e24aa", "#6a1b9a");
        drawBtn.setOnAction(e -> onDraw());
        passBtn.setOnAction(e -> onPass());
        crazyBtn.setOnAction(e -> onCrazy());

        HBox btnRow = new HBox(20, drawBtn, passBtn, crazyBtn);
        btnRow.setAlignment(Pos.CENTER);

        box.getChildren().addAll(sevenBundleBar, handPanel, btnRow);
        return box;
    }

    private Button buildActionButton(String text, String base, String hover) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setPrefWidth(145);
        btn.setPrefHeight(40);
        btn.setBackground(new Background(new BackgroundFill(Color.web(base), new CornerRadii(8), Insets.EMPTY)));
        btn.setEffect(new DropShadow(4, Color.BLACK));
        btn.setOnMouseEntered(e -> btn.setBackground(new Background(new BackgroundFill(Color.web(hover), new CornerRadii(8), Insets.EMPTY))));
        btn.setOnMouseExited(e  -> btn.setBackground(new Background(new BackgroundFill(Color.web(base),  new CornerRadii(8), Insets.EMPTY))));
        return btn;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GAME FLOW
    // ═════════════════════════════════════════════════════════════════════════

    private void refresh() {
        if (game.isGameOver()) { showWinner(); return; }

        Player current = game.getCurrentPlayer();

        if (game.hasPendingPenalty()) { showPenaltyPrompt(); return; }

        playerNameLabel.setText(current.getName() + "'s Turn");
        statusLabel.setText("");
        directionLabel.setText(game.isReversed()
                ? "<<  Direction: Counter-Clockwise" : ">>  Direction: Clockwise");

        discardArea.getChildren().clear();
        discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));

        updateOpponentLabels();

        handArea.getChildren().clear();
        pendingSevenCard = null;
        sevenExtras.clear();
        sevenBundleBar.setVisible(false);
        sevenBundleBar.setManaged(false);

        for (Card card : game.getCard()) {
            Pane node = buildCard(card, true, false);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }

        drawBtn.setText("Draw Card");
        drawBtn.setOnAction(e -> onDraw());
        drawBtn.setDisable(false);
        passBtn.setDisable(false);
    }

    private void advanceTurn() {
        // Show hand-off screen so next player can't see current player's cards
        String nextName = game.getCurrentPlayer().getName();
        showHandOff(nextName, () -> refresh());
    }

    private void updateOpponentLabels() {
        if (opponentBar == null) return;
        opponentBar.getChildren().clear();
        List<Player> players = game.getPlayers();
        int curr = game.getCurrentPlayerIndex();
        for (int i = 0; i < players.size(); i++) {
            if (i == curr) continue;
            Player p = players.get(i);
            VBox pBox = new VBox(4);
            pBox.setAlignment(Pos.CENTER);
            pBox.setPadding(new Insets(8, 14, 8, 14));
            pBox.setBackground(new Background(new BackgroundFill(
                    Color.color(0,0,0,0.3), new CornerRadii(10), Insets.EMPTY)));

            Label name = new Label(p.getName());
            name.setTextFill(Color.WHITE);
            name.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

            HBox faceDown = new HBox(-18);
            faceDown.setAlignment(Pos.CENTER);
            int show = Math.min(p.getHandSize(), 5);
            for (int j = 0; j < show; j++) faceDown.getChildren().add(buildCardBack());

            Label count = new Label(p.getHandSize() + " cards");
            count.setTextFill(GOLD);
            count.setFont(Font.font("Verdana", 11));

            pBox.getChildren().addAll(name, faceDown, count);
            opponentBar.getChildren().add(pBox);
        }
    }

    private void showPenaltyPrompt() {
        Player current = game.getCurrentPlayer();
        int amount = game.getPendingPenalty();
        boolean isAce = game.isActiveAce();

        playerNameLabel.setText(current.getName() + "'s Turn  [PENALTY]");
        statusLabel.setText(isAce
                ? "Ace of Spades! Defend with 2 of Spades or draw " + amount + " cards!"
                : "TWO penalty! Stack a 2 or draw " + amount + " cards!");
        directionLabel.setText("");

        discardArea.getChildren().clear();
        discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));
        updateOpponentLabels();

        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            boolean canDefend = isAce
                    ? (card.getOrder() == Order.TWO && card.getSuit() == Suit.Spades)
                    : (card.getOrder() == Order.TWO);
            Pane node = buildCard(card, true, canDefend);
            if (canDefend) {
                node.setOnMouseClicked(e -> {
                    boolean ok = isAce ? game.defendWithTwoOfSpades(card) : game.defendWithTwo(card);
                    if (ok) {
                        statusLabel.setText(current.getName() + " defended! Penalty is now " + game.getPendingPenalty());
                        game.moveToNext();
                        advanceTurn();
                    }
                });
            } else {
                node.setOpacity(0.45);
            }
            handArea.getChildren().add(node);
        }

        drawBtn.setText("Accept (" + amount + " cards)");
        drawBtn.setDisable(false);
        drawBtn.setOnAction(e -> {
            game.acceptPenalty();
            drawBtn.setText("Draw Card");
            drawBtn.setOnAction(ev -> onDraw());
            if (game.isGameOver()) { showWinner(); return; }
            advanceTurn();
        });
        passBtn.setDisable(true);
    }

    private void onCardClicked(Card card) {
        if (game.isGameOver()) return;
        if (pendingSevenCard != null) {
            if (card == pendingSevenCard) { commitSevenPlay(); return; }
            if (card.getSuit() == pendingSevenCard.getSuit()) { toggleSevenExtra(card); }
            else { statusLabel.setText("Only " + pendingSevenCard.getSuit() + " cards can bundle with 7."); }
            return;
        }
        if (card.getOrder() == Order.SEVEN && game.isValidMove(card)) { enterSevenBundleMode(card); return; }
        attemptPlay(card);
    }

    private void enterSevenBundleMode(Card seven) {
        pendingSevenCard = seven;
        sevenExtras.clear();
        refreshHandHighlights();
        sevenBundleBar.getChildren().clear();
        Label hint = new Label("7 selected — click same-suit cards to bundle (max 4), then click 7 again to play");
        hint.setTextFill(GOLD); hint.setFont(Font.font("Verdana", 12));
        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;");
        cancel.setOnAction(e -> {
            pendingSevenCard = null; sevenExtras.clear();
            sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false);
            refreshHandHighlights(); statusLabel.setText("");
        });
        sevenBundleBar.getChildren().addAll(hint, cancel);
        sevenBundleBar.setVisible(true); sevenBundleBar.setManaged(true);
        statusLabel.setText("Bundle mode: select extras then click 7 to play.");
    }

    private void toggleSevenExtra(Card card) {
        if (sevenExtras.contains(card)) { sevenExtras.remove(card); }
        else {
            if (sevenExtras.size() >= 4) { statusLabel.setText("Max 4 extras with a 7."); return; }
            sevenExtras.add(card);
        }
        refreshHandHighlights();
        statusLabel.setText("Extras: " + sevenExtras.size() + " — click 7 to confirm.");
    }

    private void commitSevenPlay() {
        boolean ok = game.playSevenWithExtras(pendingSevenCard, new ArrayList<>(sevenExtras));
        if (ok) {
            pendingSevenCard = null; sevenExtras.clear();
            sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false);
            afterSuccessfulPlay(null);
        } else { statusLabel.setText("Invalid play."); }
    }

    private void refreshHandHighlights() {
        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            boolean sel = card == pendingSevenCard || sevenExtras.contains(card);
            Pane node = buildCard(card, true, sel);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
    }

    private void attemptPlay(Card card) {
        if (game.playCard(card)) {
            lastPlayInvalid = false;
            afterSuccessfulPlay(card);
        } else {
            lastPlayInvalid = true;
            statusLabel.setText("Invalid move! Others can say CRAZY to penalise you, or draw.");
            TranslateTransition tt = new TranslateTransition(Duration.millis(60), statusLabel);
            tt.setFromX(-6); tt.setToX(6); tt.setCycleCount(4); tt.setAutoReverse(true); tt.play();
        }
    }

    private void onCrazy() {
        if (!lastPlayInvalid) { statusLabel.setText("No invalid play to challenge!"); return; }
        game.applyCrazyPenalty();
        lastPlayInvalid = false;
        statusLabel.setText("CRAZY! " + game.getCurrentPlayer().getName() + " draws 2 cards.");
        // Refresh hand in place (don't advance turn — the penalised player still plays)
        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            Pane node = buildCard(card, true, false);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
        updateOpponentLabels();
    }

    private void afterSuccessfulPlay(Card card) {
        if (card != null && (card.getOrder() == Order.EIGHT || card.getOrder() == Order.J)) {
            Suit chosen = chooseSuit();
            if (chosen != null) { game.setSuit(chosen); statusLabel.setText("Suit changed to " + chosen); }
        }
        if (game.isGameOver()) { showWinner(); return; }
        advanceTurn();
    }

    private void onDraw() {
        if (game.isGameOver()) return;
        if (game.hasDrawn()) { statusLabel.setText("You already drew. Play or pass."); return; }
        lastPlayInvalid = false;
        Card drawn = game.drawForCurrentPlayer();
        if (drawn != null) { game.setHasDrawn(true); statusLabel.setText("Drew: " + drawn); }
        if (game.isGameOver()) { showWinner(); return; }
        // Refresh hand in-place so player can immediately play the drawn card
        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            Pane node = buildCard(card, true, false);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
        updateOpponentLabels();
    }

    private void onPass() {
        if (game.isGameOver()) return;
        lastPlayInvalid = false;
        game.setHasDrawn(false);
        game.moveToNext();
        advanceTurn();
    }

    private Suit chooseSuit() {
        Dialog<Suit> dialog = new Dialog<>();
        dialog.setTitle("Choose Suit");
        dialog.setHeaderText("Pick the suit to play:");
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16); grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:#1a6b3c;");
        Suit[] suits = Suit.values();
        final Suit[] chosen = {null};
        for (int i = 0; i < suits.length; i++) {
            Suit s = suits[i];
            Pane btn = buildSuitPickerCard(s);
            btn.setOnMouseClicked(e -> { chosen[0] = s; dialog.close(); });
            grid.add(btn, i % 2, i / 2);
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color:#1a6b3c;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
        return chosen[0];
    }

    private Pane buildSuitPickerCard(Suit suit) {
        Pane sp = new Pane();
        sp.setPrefSize(110, 90);
        sp.setBackground(new Background(new BackgroundFill(CARD_WHITE, new CornerRadii(10), Insets.EMPTY)));
        sp.setEffect(new DropShadow(6, Color.BLACK));

        javafx.scene.Node shape = buildSuitShape(suit, 36);
        shape.setLayoutX(37); shape.setLayoutY(8);

        Text name = new Text(suit.name());
        name.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        name.setFill(suitColor(suit));
        name.setLayoutX(110 / 2.0 - name.getLayoutBounds().getWidth() / 2 - 5);
        name.setLayoutY(70);
        sp.getChildren().addAll(shape, name);

        sp.setOnMouseEntered(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(100), sp); st.setToX(1.08); st.setToY(1.08); st.play(); });
        sp.setOnMouseExited( e -> { ScaleTransition st = new ScaleTransition(Duration.millis(100), sp); st.setToX(1.0);  st.setToY(1.0);  st.play(); });
        sp.setStyle("-fx-cursor:hand;");
        return sp;
    }

    private void showWinner() {
        root.setCenter(buildWinnerScreen(game.getWinner().getName()));
        if (handArea != null) handArea.getChildren().clear();
        if (drawBtn != null) drawBtn.setDisable(true);
        if (passBtn != null) passBtn.setDisable(true);
        if (playerNameLabel != null) playerNameLabel.setText("Game Over!");
        if (statusLabel != null) statusLabel.setText("");
        if (directionLabel != null) directionLabel.setText("");
        // Show handoff screen first
        primaryStage.getScene().setRoot(root);
    }

    private StackPane buildWinnerScreen(String name) {
        StackPane sp = new StackPane(); sp.setAlignment(Pos.CENTER);
        sp.setBackground(feltBackground());

        VBox box = new VBox(20); box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        box.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.6), new CornerRadii(20), Insets.EMPTY)));
        box.setMaxWidth(480); box.setMaxHeight(320);

        Label win = new Label(name + " Wins!");
        win.setFont(Font.font("Georgia", FontWeight.BOLD, 38));
        win.setTextFill(GOLD); win.setEffect(new DropShadow(10, GOLD_DARK));

        Label sub = new Label("Congratulations!");
        sub.setFont(Font.font("Verdana", 18)); sub.setTextFill(Color.WHITE);

        Button again = buildActionButton("Play Again", "#27ae60", "#1e8449");
        again.setPrefWidth(180);
        again.setOnAction(e -> showNameEntry());

        box.getChildren().addAll(win, sub, again);
        sp.getChildren().add(box);
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARD RENDERING — uses Pane with absolute coords, no clip issues
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns a Pane (NOT StackPane) containing a fully drawn card.
     * Hand: 82x122  |  Discard/table: 110x160
     */
    private Pane buildCard(Card card, boolean isHand, boolean selected) {
        double W = isHand ? 82 : 110;
        double H = isHand ? 122 : 160;
        double pip   = isHand ? 11 : 15;
        double rFont = isHand ? 15 : 20;   // rank corner font size

        Pane pane = new Pane();
        pane.setPrefSize(W, H);
        pane.setMinSize(W, H);
        pane.setMaxSize(W, H);

        // ── card body ────────────────────────────────────────────────────────
        Rectangle body = new Rectangle(1, 1, W - 2, H - 2);
        body.setArcWidth(10); body.setArcHeight(10);
        body.setFill(CARD_WHITE);
        DropShadow shadow = new DropShadow(selected ? 16 : 7,
                selected ? GOLD : Color.color(0,0,0,0.5));
        body.setEffect(shadow);
        body.setStroke(selected ? GOLD : Color.web("#999999"));
        body.setStrokeWidth(selected ? 2.5 : 0.8);
        pane.getChildren().add(body);

        Color sc = suitColor(card.getSuit());
        String rank = rankLabel(card.getOrder());

        // ── top-left corner: rank text ────────────────────────────────────────
        Text rankTL = new Text(rank);
        rankTL.setFont(Font.font("Arial", FontWeight.BOLD, rFont));
        rankTL.setFill(sc);
        rankTL.setLayoutX(5);
        rankTL.setLayoutY(rFont + 2);
        pane.getChildren().add(rankTL);

        // ── top-left corner: small suit pip below rank ────────────────────────
        double sSize = rFont * 0.75;
        javafx.scene.Node pipTL = buildSuitShape(card.getSuit(), sSize);
        pipTL.setLayoutX(6);
        pipTL.setLayoutY(rFont + 5);
        pane.getChildren().add(pipTL);

        // ── bottom-right corner (rotated 180) ────────────────────────────────
        Text rankBR = new Text(rank);
        rankBR.setFont(Font.font("Arial", FontWeight.BOLD, rFont));
        rankBR.setFill(sc);
        rankBR.setRotate(180);
        rankBR.setLayoutX(W - rFont - 2);
        rankBR.setLayoutY(H - 6);
        pane.getChildren().add(rankBR);

        javafx.scene.Node pipBR = buildSuitShape(card.getSuit(), sSize);
        pipBR.setRotate(180);
        pipBR.setLayoutX(W - sSize - 6);
        pipBR.setLayoutY(H - rFont - sSize - 4);
        pane.getChildren().add(pipBR);

        // ── centre content ────────────────────────────────────────────────────
        buildCentreOnPane(pane, card, W, H, pip);

        // ── hover lift for hand cards ─────────────────────────────────────────
        if (isHand) {
            pane.setStyle("-fx-cursor:hand;");
            pane.setOnMouseEntered(e -> {
                TranslateTransition tt = new TranslateTransition(Duration.millis(110), pane);
                tt.setToY(-12); tt.play();
            });
            pane.setOnMouseExited(e -> {
                TranslateTransition tt = new TranslateTransition(Duration.millis(110), pane);
                tt.setToY(0); tt.play();
            });
        }
        return pane;
    }

    private void buildCentreOnPane(Pane pane, Card card, double W, double H, double pip) {
        Color c = suitColor(card.getSuit());
        Order order = card.getOrder();

        // Face cards — large italic letter
        if (order == Order.J || order == Order.Q || order == Order.K) {
            Text face = new Text(rankLabel(order));
            double fSize = H * 0.42;
            face.setFont(Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, fSize));
            face.setFill(c);
            face.setLayoutX(W * 0.5 - fSize * 0.27);
            face.setLayoutY(H * 0.5 + fSize * 0.35);
            pane.getChildren().add(face);
            return;
        }

        // Ace — one large pip
        if (order == Order.ONE) {
            double sz = pip * 3.0;
            javafx.scene.Node shape = buildSuitShape(card.getSuit(), sz);
            shape.setLayoutX(W / 2 - sz / 2);
            shape.setLayoutY(H / 2 - sz / 2);
            pane.getChildren().add(shape);
            return;
        }

        // Number cards — pip grid
        int count = orderToInt(order);
        double[][] positions = pipPositions(count, W, H, pip);
        for (double[] pos : positions) {
            javafx.scene.Node shape = buildSuitShape(card.getSuit(), pip);
            if (pos[2] == 1) shape.setRotate(180);
            shape.setLayoutX(pos[0]);
            shape.setLayoutY(pos[1]);
            pane.getChildren().add(shape);
        }
    }

    private double[][] pipPositions(int count, double W, double H, double p) {
        double lx = W * 0.18, cx = W * 0.50 - p / 2, rx = W * 0.68;
        double r1 = H * 0.12, r2 = H * 0.29, r3 = H * 0.48, r4 = H * 0.64, r5 = H * 0.80;
        double rm = H * 0.38, rn = H * 0.55;
        return switch (count) {
            case 2  -> new double[][]{{cx,r1,0},{cx,r5,1}};
            case 3  -> new double[][]{{cx,r1,0},{cx,r3,0},{cx,r5,1}};
            case 4  -> new double[][]{{lx,r1,0},{rx,r1,0},{lx,r5,1},{rx,r5,1}};
            case 5  -> new double[][]{{lx,r1,0},{rx,r1,0},{cx,r3,0},{lx,r5,1},{rx,r5,1}};
            case 6  -> new double[][]{{lx,r1,0},{rx,r1,0},{lx,r3,0},{rx,r3,0},{lx,r5,1},{rx,r5,1}};
            case 7  -> new double[][]{{lx,r1,0},{rx,r1,0},{cx,r2,0},{lx,r3,0},{rx,r3,0},{lx,r5,1},{rx,r5,1}};
            case 8  -> new double[][]{{lx,r1,0},{rx,r1,0},{cx,r2,0},{lx,r3,0},{rx,r3,0},{cx,r4,1},{lx,r5,1},{rx,r5,1}};
            case 9  -> new double[][]{{lx,r1,0},{rx,r1,0},{lx,rm,0},{rx,rm,0},{cx,r3,0},{lx,rn,1},{rx,rn,1},{lx,r5,1},{rx,r5,1}};
            case 10 -> new double[][]{{lx,r1,0},{rx,r1,0},{cx,r2,0},{lx,rm,0},{rx,rm,0},{lx,rn,1},{rx,rn,1},{cx,r4,1},{lx,r5,1},{rx,r5,1}};
            default -> new double[][]{{cx,r3,0}};
        };
    }

    private int orderToInt(Order order) {
        return switch (order) {
            case TWO->2; case THREE->3; case FOUR->4; case FIVE->5;
            case SIX->6; case SEVEN->7; case EIGHT->8; case NINE->9; case TEN->10;
            default->1;
        };
    }

    private StackPane buildCardBack() {
        StackPane sp = new StackPane();
        sp.setPrefSize(36, 52);
        sp.setMinSize(36, 52);
        Rectangle r = new Rectangle(36, 52, Color.web("#1a237e"));
        r.setArcWidth(6); r.setArcHeight(6);
        r.setStroke(Color.WHITE); r.setStrokeWidth(1);
        Rectangle inner = new Rectangle(4, 4, 28, 44);
        inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.web("#3949ab")); inner.setStrokeWidth(1);
        sp.getChildren().addAll(r, inner);
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SUIT SHAPES  (JavaFX Path — no Unicode fonts)
    // ═════════════════════════════════════════════════════════════════════════

    private javafx.scene.Node buildSuitShape(Suit suit, double size) {
        return switch (suit) {
            case Hearts   -> heartShape(size, suitColor(suit));
            case Diamonds -> diamondShape(size, suitColor(suit));
            case Spades   -> spadeShape(size, suitColor(suit));
            case Clubs    -> clubShape(size, suitColor(suit));
        };
    }

    private javafx.scene.Node heartShape(double s, Color c) {
        Path p = new Path();
        p.setFill(c); p.setStroke(Color.TRANSPARENT);
        double cx = s/2, tip = s*0.95, lx = s*0.05, topY = s*0.22, ly = s*0.40;
        p.getElements().addAll(
            new MoveTo(cx, tip),
            new CubicCurveTo(cx-s*0.18, tip-s*0.25, lx, ly+s*0.10, lx, topY),
            new CubicCurveTo(lx, s*0.02, s*0.35, s*0.02, cx, ly),
            new CubicCurveTo(s*0.65, s*0.02, s*0.95, s*0.02, s*0.95, topY),
            new CubicCurveTo(s*0.95, ly+s*0.10, cx+s*0.18, tip-s*0.25, cx, tip),
            new ClosePath());
        return p;
    }

    private javafx.scene.Node diamondShape(double s, Color c) {
        Polygon poly = new Polygon(s*0.50,s*0.02, s*0.97,s*0.50, s*0.50,s*0.98, s*0.03,s*0.50);
        poly.setFill(c); poly.setStroke(Color.TRANSPARENT);
        return poly;
    }

    private javafx.scene.Node spadeShape(double s, Color c) {
        Group g = new Group();
        double cx = s/2, tip = s*0.05, lx = s*0.05, rx = s*0.95, ly = s*0.62;
        Path body = new Path();
        body.setFill(c); body.setStroke(Color.TRANSPARENT);
        body.getElements().addAll(
            new MoveTo(cx, tip),
            new CubicCurveTo(cx-s*0.18, tip+s*0.25, lx, ly-s*0.10, lx, s*0.78),
            new CubicCurveTo(lx, s*0.96, s*0.35, s*0.96, cx, ly),
            new CubicCurveTo(s*0.65, s*0.96, rx, s*0.96, rx, s*0.78),
            new CubicCurveTo(rx, ly-s*0.10, cx+s*0.18, tip+s*0.25, cx, tip),
            new ClosePath());
        Polygon stem = new Polygon(cx-s*0.18,s*0.72, cx+s*0.18,s*0.72, cx+s*0.08,s*0.95, cx-s*0.08,s*0.95);
        stem.setFill(c); stem.setStroke(Color.TRANSPARENT);
        g.getChildren().addAll(body, stem);
        return g;
    }

    private javafx.scene.Node clubShape(double s, Color c) {
        Group g = new Group();
        double r = s*0.22;
        Circle top   = new Circle(s*0.50, s*0.22, r, c);
        Circle left  = new Circle(s*0.25, s*0.52, r, c);
        Circle right = new Circle(s*0.75, s*0.52, r, c);
        Polygon stem = new Polygon(s*0.42,s*0.60, s*0.58,s*0.60, s*0.65,s*0.95, s*0.35,s*0.95);
        stem.setFill(c); stem.setStroke(Color.TRANSPARENT);
        g.getChildren().addAll(stem, left, right, top);
        return g;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Color suitColor(Suit suit) {
        return switch (suit) {
            case Hearts, Diamonds -> RED_SUIT;
            case Spades, Clubs    -> BLACK_SUIT;
        };
    }

    private static String rankLabel(Order order) {
        return switch (order) {
            case ONE->"A"; case TWO->"2"; case THREE->"3"; case FOUR->"4";
            case FIVE->"5"; case SIX->"6"; case SEVEN->"7"; case EIGHT->"8";
            case NINE->"9"; case TEN->"10"; case J->"J"; case Q->"Q"; case K->"K";
        };
    }

    public static void main(String[] args) { launch(args); }
}
