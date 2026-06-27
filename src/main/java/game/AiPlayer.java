package game;

import java.util.ArrayList;
import java.util.List;

/**
 * AI logic for a computer-controlled player.
 * Decides what to play based on the current game state.
 */
public class AiPlayer {

    /**
     * Decides and executes the AI's turn on the given game.
     * Handles penalties automatically, then picks the best card to play.
     * Returns a description of what the AI did (for the UI status label).
     */
    public static String takeTurn(Game game) {
        Player ai = game.getCurrentPlayer();

        // Handle any active penalty first
        if (game.hasPendingPenalty()) {
            boolean isAce = game.isActiveAce();
            if (isAce) {
                Card defence = ai.getDefence(Order.TWO, Suit.Spades);
                if (defence != null) {
                    game.defendWithTwoOfSpades(defence);
                    game.moveToNext();
                    return ai.getName() + " defended Ace with 2 of Spades!";
                }
            } else {
                Card defence = ai.getDefence(Order.TWO);
                if (defence != null) {
                    game.defendWithTwo(defence);
                    game.moveToNext();
                    return ai.getName() + " stacked a 2 (penalty now " + game.getPendingPenalty() + ")!";
                }
            }
            // No defence — accept penalty (draw cards), then continue turn
            int amount = game.getPendingPenalty();
            game.acceptPenalty();
            // Now try to play a card with the newly drawn hand
            Card best = pickBestCard(game, ai);
            if (best != null) {
                game.playCard(best);
                return ai.getName() + " drew " + amount + " penalty cards, then played " + best + ".";
            }
            // Nothing to play — draw one and pass
            game.drawForCurrentPlayer();
            game.setHasDrawn(false);
            game.moveToNext();
            return ai.getName() + " drew " + amount + " penalty cards and passed.";
        }

        // Pick the best card to play
        Card best = pickBestCard(game, ai);

        if (best != null) {
            // Handle 7 bundle: collect same-suit extras
            if (best.getOrder() == Order.SEVEN) {
                List<Card> extras = pickSevenExtras(ai, best, game);
                game.playSevenWithExtras(best, extras);
                if (extras.isEmpty()) {
                    return ai.getName() + " played 7 of " + best.getSuit() + " (reversed direction).";
                } else {
                    return ai.getName() + " played 7 of " + best.getSuit() + " + " + extras.size() + " extras.";
                }
            }

            game.playCard(best);

            // If wild card, pick suit that hurts opponents most (only if allowed)
            if (best.getOrder() == Order.EIGHT || best.getOrder() == Order.J) {
                if (game.canChangeSuit()) {
                    Suit chosen = pickBestSuit(ai, game);
                    game.setSuit(chosen);
                    return ai.getName() + " played " + best + " - chose " + chosen + ".";
                } else {
                    return ai.getName() + " played " + best + " (suit change blocked - 3+ players rule).";
                }
            }

            return ai.getName() + " played " + best + ".";
        }

        // No valid card — draw one
        Card drawn = game.drawForCurrentPlayer();
        if (drawn != null && game.isValidMove(drawn)) {
            game.playCard(drawn);
            return ai.getName() + " drew and played " + drawn + ".";
        }
        // Pass
        game.setHasDrawn(false);
        game.moveToNext();
        return ai.getName() + " drew and passed.";
    }

    /** Pick the best card: special cards first, then match, else null. */
    private static Card pickBestCard(Game game, Player ai) {
        List<Card> hand = ai.getHand();

        // Priority 1: Ace of Spades
        for (Card c : hand)
            if (c.getOrder() == Order.ONE && c.getSuit() == Suit.Spades && game.isValidMove(c)) return c;

        // Priority 2: TWO (stack penalty)
        for (Card c : hand)
            if (c.getOrder() == Order.TWO && game.isValidMove(c)) return c;

        // Priority 3: FIVE (skip)
        for (Card c : hand)
            if (c.getOrder() == Order.FIVE && game.isValidMove(c)) return c;

        // Priority 4: SEVEN (reverse, possibly bundle)
        for (Card c : hand)
            if (c.getOrder() == Order.SEVEN && game.isValidMove(c)) return c;

        // Priority 5: Wild (J or 8)
        for (Card c : hand)
            if ((c.getOrder() == Order.J || c.getOrder() == Order.EIGHT) && game.isValidMove(c)) return c;

        // Priority 6: Any valid card (prefer same suit over same rank)
        Card topCard = game.getTopCard();
        for (Card c : hand)
            if (game.isValidMove(c) && c.getSuit() == topCard.getSuit()) return c;

        for (Card c : hand)
            if (game.isValidMove(c)) return c;

        return null;
    }

    /** For a 7, collect up to 4 same-suit extras from hand. */
    private static List<Card> pickSevenExtras(Player ai, Card seven, Game game) {
        List<Card> extras = new ArrayList<>();
        for (Card c : ai.getHand()) {
            if (c != seven && c.getSuit() == seven.getSuit()) {
                extras.add(c);
                if (extras.size() == 4) break;
            }
        }
        return extras;
    }

    /** Pick the suit that opponents have the LEAST of — maximises their difficulty. */
    private static Suit pickBestSuit(Player ai, Game game) {
        List<Player> players = game.getPlayers();
        int[] opponentCounts = new int[Suit.values().length];

        // Count how many cards of each suit all opponents have combined
        for (Player p : players) {
            if (p == ai) continue;
            for (Card c : p.getHand()) {
                opponentCounts[c.getSuit().ordinal()]++;
            }
        }

        // Also factor in what the AI holds most of (want suits AI can play later)
        int[] aiCounts = new int[Suit.values().length];
        for (Card c : ai.getHand()) {
            aiCounts[c.getSuit().ordinal()]++;
        }

        // Score = AI count - opponent count (higher = better for AI)
        int best = 0;
        for (int i = 1; i < Suit.values().length; i++) {
            if ((aiCounts[i] - opponentCounts[i]) > (aiCounts[best] - opponentCounts[best])) {
                best = i;
            }
        }
        return Suit.values()[best];
    }
}
