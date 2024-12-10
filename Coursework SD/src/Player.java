package src;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Player implements Runnable{
    private final List<Card> hand;
    private final Deck drawDeck;
    private final Deck discardDeck;
    private final AtomicBoolean gameWon;
    private final AtomicInteger winner;
    private final PrintWriter output;
    private volatile boolean alive;
    private final int playerNumber;

    public Player(Deck drawDeck, Deck discardDeck, int playerNumber, AtomicBoolean gameWon, AtomicInteger winner) throws IOException {
        this.hand = new ArrayList<>();
        this.drawDeck = drawDeck;
        this.discardDeck = discardDeck;
        this.gameWon = gameWon;
        this.playerNumber = playerNumber;
        this.output = new PrintWriter(new FileWriter("player" + playerNumber + "_output.txt"));
        this.alive = true;
        this.winner = winner;
    }

    public void drawCard(Card card) {
        hand.add(card);
    }

    public void discardCard(Card card) {
        // remove from hand and add to bottom of discardDeck
        hand.remove(card);
        discardDeck.addCard(card);
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    private Card chooseDiscardCard() {
        // List to store cards that can be discarded
        List<Card> discardCards = new ArrayList<>();

        // Add cards that don't match playerNumber
        for (Card card : hand) {
            if (card.getValue() != playerNumber) {
                discardCards.add(card);
            }
        }
        // If all cards match playerNumber, the game will have finished but to prevent edge case errors, return the first card as default
        if (discardCards.isEmpty()) {
            return hand.getFirst();
        }

        // return a random card from the discard cards
        return discardCards.get(new Random().nextInt(discardCards.size()));
    }

    public boolean hasWinningHand() {
        // for every full hand, check if all values are equal, then set gameWon to true and the winner to the playerNumber
        if (hand.size() < 4) {
            return false;
        }
        int targetValue = hand.getFirst().getValue();
        for (Card card : hand) {
            if (card.getValue() != targetValue) {
                return false;
            }
        }

        winner.set(playerNumber);
        gameWon.set(true);

        return true;
    }

    private String handToString() {
        // convert hand of cards to a string with format 1 2 3 4
        if (hand.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Card card : hand) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(card.getValue());
        }

        return sb.toString();
    }

    @Override
    public void run() {
        while (alive && !gameWon.get()) {
            try{
                // check if hand is a winner
                if (hasWinningHand()) {
                    System.out.println("player " + playerNumber + " wins");
                    output.println("player " + playerNumber + " wins");
                    output.println("player " + playerNumber + " exits");
                    output.println("player " + playerNumber + " final hand: " + handToString());
                    output.flush();
                    alive = false;
                    break;
                }
                // Start by drawing from drawDeck
                Card drawCard = drawDeck.drawCard();
                output.println("player " + playerNumber + " draws a " + drawCard.getValue() + " from deck " + drawDeck.getDeckNumber());
                hand.add(drawCard);
                // choose discard card and discard to discard deck
                Card discardCard = chooseDiscardCard();
                discardCard(discardCard);
                output.println("player " + playerNumber + " discards a " + discardCard.getValue() + " to deck " + discardDeck.getDeckNumber());
                // write current hand to file
                output.println("player " + playerNumber + " current hand is " + handToString());
                output.flush();

                // repeat til either alive is false or someone sets gameWon to true
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!alive || gameWon.get()) {
            if (!hasWinningHand()) {
                // if player has not won then output this message
                output.println("player " + winner + " has informed player " + playerNumber + " that player " + winner + " has won");
                output.println("player " + playerNumber + " exits");
                output.println("player " + playerNumber + " final hand: " + handToString());
            }
            output.close();
        }
    }

    public void stopPlaying() {
        alive = false;
    }

}
