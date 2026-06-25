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
    private BorderPane root;
    private HBox handArea;
    private StackPane discardArea;
    private Label statusLabel;
    private Label directionLabel;
    private Label playerNameLabel;
    private Button drawBtn;
    private Button passBtn;
    private final List<Card> sevenExtras = new ArrayList<>();
    private Card pendingSevenCard = null;
    private HBox sevenBundleBar;

    @Override
    public void start(Stage stage) {
        game = new Game();
        game.addPlayers("Samuel");
        game.addPlayers("Abel");
        game.addPlayers("Dani");
        game.startGame();

        root = buildRoot();
        Scene scene = new Scene(root, 1100, 750);
        stage.setTitle("Crazy Card Game");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
        refresh();
    }

    private BorderPane buildRoot() {
        BorderPane bp = new BorderPane();
        bp.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, FELT_DARK), new Stop(0.5, FELT_MID), new Stop(1, FELT_LIGHT)),
                CornerRadii.EMPTY, Insets.EMPTY)));
        bp.setTop(buildTopBar());
        bp.setCenter(buildCentreArea());
        bp.setBottom(buildBottomArea());
        return bp;
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
        discardArea.setPrefSize(120, 170);

        // Opponent card counts across the top
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
        scroll.setPrefHeight(160);
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
        btn.setPrefWidth(140);
        btn.setPrefHeight(40);
        btn.setBackground(new Background(new BackgroundFill(Color.web(base), new CornerRadii(8), Insets.EMPTY)));
        btn.setEffect(new DropShadow(4, Color.BLACK));
        btn.setOnMouseEntered(e -> btn.setBackground(new Background(new BackgroundFill(Color.web(hover), new CornerRadii(8), Insets.EMPTY))));
        btn.setOnMouseExited(e  -> btn.setBackground(new Background(new BackgroundFill(Color.web(base),  new CornerRadii(8), Insets.EMPTY))));
        return btn;
    }

    private void refresh() {
        if (game.isGameOver()) { showWinner(); return; }

        Player current = game.getCurrentPlayer();

        // Show penalty prompt if active — player must defend or accept
        if (game.hasPendingPenalty()) {
            showPenaltyPrompt();
            return;
        }

        playerNameLabel.setText(current.getName() + "'s Turn");
        statusLabel.setText("");
        directionLabel.setText(game.isReversed() ? "<<  Direction: Counter-Clockwise" : ">>  Direction: Clockwise");

        // Update discard pile
        discardArea.getChildren().clear();
        discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));

        // Update opponent card count labels in centre
        updateOpponentLabels();

        handArea.getChildren().clear();
        pendingSevenCard = null;
        sevenExtras.clear();
        sevenBundleBar.setVisible(false);
        sevenBundleBar.setManaged(false);

        for (Card card : game.getCard()) {
            StackPane node = buildCard(card, true, false);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }

        drawBtn.setDisable(false);
        passBtn.setDisable(false);
    }

    // Centre area — opponent card counts
    private HBox opponentBar;

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
                    Color.color(0, 0, 0, 0.3), new CornerRadii(10), Insets.EMPTY)));

            Label name = new Label(p.getName());
            name.setTextFill(Color.WHITE);
            name.setFont(Font.font("Verdana", FontWeight.BOLD, 12));

            Label count = new Label(p.getHandSize() + " cards");
            count.setTextFill(GOLD);
            count.setFont(Font.font("Verdana", 11));

            // Show stacked face-down cards
            HBox faceDown = new HBox(-18);
            faceDown.setAlignment(Pos.CENTER);
            int show = Math.min(p.getHandSize(), 5);
            for (int j = 0; j < show; j++) {
                faceDown.getChildren().add(buildCardBack());
            }

            pBox.getChildren().addAll(name, faceDown, count);
            opponentBar.getChildren().add(pBox);
        }
    }

    private StackPane buildCardBack() {
        StackPane sp = new StackPane();
        sp.setPrefSize(35, 50);
        sp.setMinSize(35, 50);
        Rectangle r = new Rectangle(35, 50, Color.web("#1a237e"));
        r.setArcWidth(6); r.setArcHeight(6);
        r.setStroke(Color.WHITE); r.setStrokeWidth(1);
        // Crosshatch pattern
        Rectangle inner = new Rectangle(4, 4, 27, 42);
        inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.web("#3949ab")); inner.setStrokeWidth(1);
        sp.getChildren().addAll(r, inner);
        return sp;
    }

    /** Shows a penalty dialog — player must defend or accept drawing cards. */
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

        handArea.getChildren().clear();

        // Only show defence cards + show others greyed out
        for (Card card : game.getCard()) {
            boolean canDefend = isAce
                    ? (card.getOrder() == Order.TWO && card.getSuit() == Suit.Spades)
                    : (card.getOrder() == Order.TWO);

            StackPane node = buildCard(card, true, canDefend);
            if (canDefend) {
                node.setOnMouseClicked(e -> {
                    boolean defended = isAce
                            ? game.defendWithTwoOfSpades(card)
                            : game.defendWithTwo(card);
                    if (defended) {
                        if (isAce) {
                            statusLabel.setText(current.getName() + " defended! Penalty increases to " + game.getPendingPenalty());
                        } else {
                            statusLabel.setText(current.getName() + " stacked a 2! Penalty is now " + game.getPendingPenalty());
                        }
                        game.moveToNext();
                        refresh();
                    }
                });
            } else {
                node.setOpacity(0.5);
            }
            handArea.getChildren().add(node);
        }

        // Accept penalty button
        drawBtn.setText("Accept (" + amount + " cards)");
        drawBtn.setDisable(false);
        drawBtn.setOnAction(e -> {
            game.acceptPenalty();
            drawBtn.setText("Draw Card");
            drawBtn.setOnAction(ev -> onDraw());
            if (game.isGameOver()) { showWinner(); return; }
            refresh();
        });
        passBtn.setDisable(true);
    }

    private void onCardClicked(Card card) {
        if (game.isGameOver()) return;
        if (pendingSevenCard != null) {
            if (card == pendingSevenCard) { commitSevenPlay(); return; }
            if (card.getSuit() == pendingSevenCard.getSuit()) { toggleSevenExtra(card); }
            else { statusLabel.setText("Only " + pendingSevenCard.getSuit() + " cards can bundle with the 7."); }
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
        hint.setTextFill(GOLD);
        hint.setFont(Font.font("Verdana", 12));

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;");
        cancel.setOnAction(e -> {
            pendingSevenCard = null; sevenExtras.clear();
            sevenBundleBar.setVisible(false); sevenBundleBar.setManaged(false);
            refreshHandHighlights(); statusLabel.setText("");
        });

        sevenBundleBar.getChildren().addAll(hint, cancel);
        sevenBundleBar.setVisible(true);
        sevenBundleBar.setManaged(true);
        statusLabel.setText("Bundle mode: select extras then click 7 to play.");
    }

    private void toggleSevenExtra(Card card) {
        if (sevenExtras.contains(card)) { sevenExtras.remove(card); }
        else {
            if (sevenExtras.size() >= 4) { statusLabel.setText("Max 4 extra cards with a 7."); return; }
            sevenExtras.add(card);
        }
        refreshHandHighlights();
        statusLabel.setText("Extras selected: " + sevenExtras.size() + " — click 7 to play.");
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
            StackPane node = buildCard(card, true, sel);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
    }

    private boolean lastPlayInvalid = false;

    private void attemptPlay(Card card) {
        if (game.playCard(card)) {
            lastPlayInvalid = false;
            afterSuccessfulPlay(card);
        } else {
            lastPlayInvalid = true;
            statusLabel.setText("Invalid move! Others can say CRAZY to penalise you, or you can draw.");
            TranslateTransition tt = new TranslateTransition(Duration.millis(60), statusLabel);
            tt.setFromX(-6); tt.setToX(6); tt.setCycleCount(4); tt.setAutoReverse(true); tt.play();
        }
    }

    private void onCrazy() {
        if (!lastPlayInvalid) {
            statusLabel.setText("No invalid play to challenge!");
            return;
        }
        // Current player draws 2 cards as penalty
        game.applyCrazyPenalty();
        lastPlayInvalid = false;
        statusLabel.setText("CRAZY called! " + game.getCurrentPlayer().getName() + " draws 2 cards!");
        refresh();
    }

    private void afterSuccessfulPlay(Card card) {
        if (card != null && (card.getOrder() == Order.EIGHT || card.getOrder() == Order.J)) {
            Suit chosen = chooseSuit();
            if (chosen != null) { game.setSuit(chosen); statusLabel.setText("Suit changed to " + chosen); }
        }
        if (game.isGameOver()) { showWinner(); return; }
        refresh();
    }

    private void onDraw() {
        if (game.isGameOver()) return;
        if (game.hasDrawn()) { statusLabel.setText("You already drew. Play a card or pass."); return; }
        lastPlayInvalid = false;
        Card drawn = game.drawForCurrentPlayer();
        if (drawn != null) { game.setHasDrawn(true); statusLabel.setText("Drew: " + drawn); }
        if (game.isGameOver()) { showWinner(); return; }
        // Don't auto-refresh so player can see drawn card and play it
        discardArea.getChildren().clear();
        discardArea.getChildren().add(buildCard(game.getTopCard(), false, false));
        handArea.getChildren().clear();
        for (Card card : game.getCard()) {
            StackPane node = buildCard(card, true, false);
            node.setOnMouseClicked(e -> onCardClicked(card));
            handArea.getChildren().add(node);
        }
    }

    private void onPass() {
        if (game.isGameOver()) return;
        lastPlayInvalid = false;
        game.setHasDrawn(false);
        game.moveToNext();
        refresh();
    }

    private Suit chooseSuit() {
        Dialog<Suit> dialog = new Dialog<>();
        dialog.setTitle("Choose Suit");
        dialog.setHeaderText("Pick the suit:");
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16); grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:#1a6b3c;");
        Suit[] suits = Suit.values();
        final Suit[] chosen = {null};
        for (int i = 0; i < suits.length; i++) {
            Suit s = suits[i];
            StackPane btn = buildSuitPickerCard(s);
            btn.setOnMouseClicked(e -> { chosen[0] = s; dialog.close(); });
            grid.add(btn, i % 2, i / 2);
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color:#1a6b3c;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.showAndWait();
        return chosen[0];
    }

    private StackPane buildSuitPickerCard(Suit suit) {
        StackPane sp = new StackPane();
        sp.setPrefSize(110, 90);
        sp.setAlignment(Pos.CENTER);
        sp.setBackground(new Background(new BackgroundFill(CARD_WHITE, new CornerRadii(10), Insets.EMPTY)));
        sp.setEffect(new DropShadow(6, Color.BLACK));
        VBox content = new VBox(6);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(buildSuitShape(suit, 36), buildSuitLabel(suit, 13));
        sp.getChildren().add(content);
        sp.setOnMouseEntered(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(100), sp); st.setToX(1.1); st.setToY(1.1); st.play(); });
        sp.setOnMouseExited( e -> { ScaleTransition st = new ScaleTransition(Duration.millis(100), sp); st.setToX(1.0); st.setToY(1.0); st.play(); });
        return sp;
    }

    private void showWinner() {
        root.setCenter(buildWinnerScreen(game.getWinner().getName()));
        handArea.getChildren().clear();
        drawBtn.setDisable(true); passBtn.setDisable(true);
        playerNameLabel.setText("Game Over!"); statusLabel.setText(""); directionLabel.setText("");
    }

    private StackPane buildWinnerScreen(String name) {
        StackPane sp = new StackPane(); sp.setAlignment(Pos.CENTER);
        VBox box = new VBox(20); box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.6), new CornerRadii(20), Insets.EMPTY)));
        box.setMaxWidth(440); box.setMaxHeight(280);

        Label win = new Label(name + " Wins!");
        win.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
        win.setTextFill(GOLD); win.setEffect(new DropShadow(10, GOLD_DARK));

        Label sub = new Label("Congratulations!");
        sub.setFont(Font.font("Verdana", 18)); sub.setTextFill(Color.WHITE);

        box.getChildren().addAll(win, sub);
        sp.getChildren().add(box);
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARD RENDERING
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Builds a realistic card pane.
     * Hand cards: 80x120.  Table (discard) card: 110x160.
     */
    private StackPane buildCard(Card card, boolean isHand, boolean selected) {
        double W = isHand ? 80 : 110;
        double H = isHand ? 120 : 160;
        double pip = isHand ? 11 : 15;
        double cornerFont = isHand ? 14 : 18;

        // Use a Pane (not StackPane) as cardRoot so children are not clipped
        Pane cardRoot = new Pane();
        cardRoot.setPrefSize(W, H);
        cardRoot.setMinSize(W, H);
        cardRoot.setMaxSize(W, H);

        // Card background
        Rectangle body = new Rectangle(0, 0, W, H);
        body.setArcWidth(12); body.setArcHeight(12);
        body.setFill(CARD_WHITE);
        DropShadow shadow = new DropShadow(selected ? 16 : 8, selected ? GOLD : Color.color(0, 0, 0, 0.55));
        body.setEffect(shadow);
        if (selected) { body.setStroke(GOLD); body.setStrokeWidth(3); }
        else          { body.setStroke(Color.web("#aaaaaa")); body.setStrokeWidth(0.8); }
        cardRoot.getChildren().add(body);

        // Top-left rank + suit
        VBox topLeft = buildCorner(card, cornerFont, false);
        topLeft.setLayoutX(0); topLeft.setLayoutY(0);
        cardRoot.getChildren().add(topLeft);

        // Bottom-right rank + suit (rotated 180)
        VBox botRight = buildCorner(card, cornerFont, true);
        botRight.setLayoutX(W - cornerFont * 1.4);
        botRight.setLayoutY(H - cornerFont * 2.6);
        cardRoot.getChildren().add(botRight);

        // Centre pips
        Pane cardCentre = buildCardCentre(card, W, H, pip);
        cardCentre.setLayoutX(0); cardCentre.setLayoutY(0);
        cardRoot.getChildren().add(cardCentre);

        // Wrap in StackPane for layout compatibility, disable clip
        StackPane sp = new StackPane(cardRoot);
        sp.setPrefSize(W, H);
        sp.setMinSize(W, H);
        sp.setMaxSize(W, H);
        sp.setClip(null);

        if (isHand) {
            sp.setStyle("-fx-cursor:hand;");
            sp.setOnMouseEntered(e -> { TranslateTransition tt = new TranslateTransition(Duration.millis(110), sp); tt.setToY(-10); tt.play(); });
            sp.setOnMouseExited( e -> { TranslateTransition tt = new TranslateTransition(Duration.millis(110), sp); tt.setToY(0);   tt.play(); });
        }
        return sp;
    }

    /** Top-left (or rotated bottom-right) corner: rank + suit shape. */
    private VBox buildCorner(Card card, double fontSize, boolean rotated) {
        VBox box = new VBox(0);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(3, 0, 0, 4));
        box.setPickOnBounds(false); // prevent clipping issues
        box.setMouseTransparent(true);

        Label rank = new Label(rankLabel(card.getOrder()));
        rank.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        rank.setTextFill(suitColor(card.getSuit()));

        javafx.scene.Node pip = buildSuitShape(card.getSuit(), fontSize - 3);
        box.getChildren().addAll(rank, pip);
        if (rotated) box.setRotate(180);
        return box;
    }

    /**
     * Builds the centre of the card:
     * - Number cards: pip grid matching real card layout
     * - Face cards (J/Q/K): big bold letter
     * - Ace: large single pip
     */
    private Pane buildCardCentre(Card card, double W, double H, double pip) {
        Pane pane = new Pane();
        pane.setPrefSize(W, H);

        Color c = suitColor(card.getSuit());
        Order order = card.getOrder();

        if (order == Order.J || order == Order.Q || order == Order.K) {
            // Face card — big italic letter
            Label face = new Label(rankLabel(order));
            face.setFont(Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, H * 0.42));
            face.setTextFill(c);
            face.setLayoutX(W * 0.5 - H * 0.12);
            face.setLayoutY(H * 0.22);
            pane.getChildren().add(face);
            return pane;
        }

        if (order == Order.ONE) {
            // Ace — one large pip centred
            javafx.scene.Node shape = buildSuitShape(card.getSuit(), pip * 3.2);
            double cx = W / 2 - pip * 1.6;
            double cy = H / 2 - pip * 1.6;
            shape.setLayoutX(cx); shape.setLayoutY(cy);
            pane.getChildren().add(shape);
            return pane;
        }

        // Number cards — place pips at standard positions
        int count = orderToInt(order);
        double[][] positions = pipPositions(count, W, H, pip);
        for (double[] pos : positions) {
            javafx.scene.Node shape = buildSuitShape(card.getSuit(), pip);
            // Pips in bottom half are rotated 180 on real cards
            if (pos[2] == 1) shape.setRotate(180);
            shape.setLayoutX(pos[0]); shape.setLayoutY(pos[1]);
            pane.getChildren().add(shape);
        }
        return pane;
    }

    /**
     * Returns pip positions for number cards.
     * Each entry: [x, y, flipped(0/1)]
     * Coordinates are offsets so the pip centre lands at (x + pip/2, y + pip/2).
     */
    private double[][] pipPositions(int count, double W, double H, double p) {
        // Columns: left, centre, right
        double lx = W * 0.20, cx = W * 0.50 - p / 2, rx = W * 0.73;
        // Rows top-to-bottom: r1..r5
        double r1 = H * 0.14, r2 = H * 0.30, r3 = H * 0.50 - p / 2, r4 = H * 0.68, r5 = H * 0.83;
        double rm = H * 0.40; // mid-upper  (used by 8, 9, 10)
        double rn = H * 0.57; // mid-lower

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
            case TWO   -> 2;  case THREE -> 3;  case FOUR  -> 4;
            case FIVE  -> 5;  case SIX   -> 6;  case SEVEN -> 7;
            case EIGHT -> 8;  case NINE  -> 9;  case TEN   -> 10;
            default    -> 1;
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SUIT SHAPES  (drawn with JavaFX Path — no Unicode, no font dependency)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Builds a filled suit shape scaled to `size` pixels.
     * All shapes are drawn in a bounding box of [0,0] to [size, size].
     */
    private javafx.scene.Node buildSuitShape(Suit suit, double size) {
        Color c = suitColor(suit);
        return switch (suit) {
            case Hearts   -> heartShape(size, c);
            case Diamonds -> diamondShape(size, c);
            case Spades   -> spadeShape(size, c);
            case Clubs    -> clubShape(size, c);
        };
    }

    /** Heart: two circles on top + inverted triangle bottom */
    private javafx.scene.Node heartShape(double s, Color c) {
        Path p = new Path();
        p.setFill(c); p.setStroke(Color.TRANSPARENT);
        double cx = s / 2, tip = s * 0.95;
        double lx = s * 0.05, topY = s * 0.20;
        double ly = s * 0.38;

        p.getElements().addAll(
            new MoveTo(cx, tip),
            new CubicCurveTo(cx - s*0.18, tip - s*0.25,  lx, ly + s*0.10,  lx, topY),
            new CubicCurveTo(lx, s*0.02, s*0.35, s*0.02, cx, ly),
            new CubicCurveTo(s*0.65, s*0.02, s*0.95, s*0.02, s*0.95, topY),
            new CubicCurveTo(s*0.95, ly + s*0.10, cx + s*0.18, tip - s*0.25, cx, tip),
            new ClosePath()
        );
        return p;
    }

    /** Diamond: simple rotated square */
    private javafx.scene.Node diamondShape(double s, Color c) {
        Polygon poly = new Polygon(
            s*0.50, s*0.02,
            s*0.97, s*0.50,
            s*0.50, s*0.98,
            s*0.03, s*0.50
        );
        poly.setFill(c); poly.setStroke(Color.TRANSPARENT);
        return poly;
    }

    /** Spade: inverted heart on top + small triangle stem */
    private javafx.scene.Node spadeShape(double s, Color c) {
        Group g = new Group();
        double cx = s / 2;

        // Main spade body = inverted heart
        Path body = new Path();
        body.setFill(c); body.setStroke(Color.TRANSPARENT);
        double tip = s * 0.05;
        double lx = s * 0.05, rx = s * 0.95;
        double ly = s * 0.62;

        body.getElements().addAll(
            new MoveTo(cx, tip),
            new CubicCurveTo(cx - s*0.18, tip + s*0.25,  lx, ly - s*0.10, lx, s*0.78),
            new CubicCurveTo(lx, s*0.96, s*0.35, s*0.96, cx, ly),
            new CubicCurveTo(s*0.65, s*0.96, rx, s*0.96, rx, s*0.78),
            new CubicCurveTo(rx, ly - s*0.10, cx + s*0.18, tip + s*0.25, cx, tip),
            new ClosePath()
        );

        // Stem triangle
        Polygon stem = new Polygon(
            cx - s*0.18, s*0.72,
            cx + s*0.18, s*0.72,
            cx + s*0.08, s*0.95,
            cx - s*0.08, s*0.95
        );
        stem.setFill(c); stem.setStroke(Color.TRANSPARENT);

        g.getChildren().addAll(body, stem);
        return g;
    }

    /** Club: three circles + stem */
    private javafx.scene.Node clubShape(double s, Color c) {
        Group g = new Group();
        double r = s * 0.22;   // circle radius

        Circle top   = new Circle(s*0.50, s*0.22, r, c);
        Circle left  = new Circle(s*0.25, s*0.52, r, c);
        Circle right = new Circle(s*0.75, s*0.52, r, c);

        // Stem
        Polygon stem = new Polygon(
            s*0.42, s*0.60,
            s*0.58, s*0.60,
            s*0.65, s*0.95,
            s*0.35, s*0.95
        );
        stem.setFill(c); stem.setStroke(Color.TRANSPARENT);

        g.getChildren().addAll(stem, left, right, top);
        return g;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private Label buildSuitLabel(Suit suit, double fontSize) {
        Label l = new Label(suit.name());
        l.setFont(Font.font("Verdana", FontWeight.BOLD, fontSize));
        l.setTextFill(suitColor(suit));
        return l;
    }

    private static Color suitColor(Suit suit) {
        return switch (suit) {
            case Hearts, Diamonds -> RED_SUIT;
            case Spades, Clubs    -> BLACK_SUIT;
        };
    }

    private static String rankLabel(Order order) {
        return switch (order) {
            case ONE -> "A"; case TWO -> "2"; case THREE -> "3";
            case FOUR -> "4"; case FIVE -> "5"; case SIX -> "6";
            case SEVEN -> "7"; case EIGHT -> "8"; case NINE -> "9";
            case TEN -> "10"; case J -> "J"; case Q -> "Q"; case K -> "K";
        };
    }

    public static void main(String[] args) { launch(args); }
}
