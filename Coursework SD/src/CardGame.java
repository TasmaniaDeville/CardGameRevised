package src;
import java.io.*;

import java.io.IOException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CardGame {
    private final int numberOfPlayers;
    private final List<Card> pack;
    private final List<Deck> decks;
    private final List<Player> players;
    private final AtomicBoolean gameWon;
    private final AtomicInteger winner;
    private final ExecutorService threadPool;

    public CardGame(int numberOfPlayers, String packPath) throws IOException {
        this.numberOfPlayers = numberOfPlayers;
        this.pack = loadPack(packPath);
        this.players = new ArrayList<>();
        this.decks = new ArrayList<>();
        this.gameWon = new AtomicBoolean(false);
        this.winner = new AtomicInteger(-1);
        this.threadPool = Executors.newFixedThreadPool(numberOfPlayers);

        validatePack();
        initializeDecks();
        initializePlayers();
        dealCards();

    }

    private List<Card> loadPack(String packPath) throws IOException {
        // Create empty array of cards to write pack into
        List<Card> cards = new ArrayList<>();
        // Split each line into its own string
        BufferedReader reader = new BufferedReader(new FileReader(packPath));
        String line;
        // for each line take the number and add a new card for that value
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                int value = Integer.parseInt(line);
                cards.add(new Card(value));
            }
        }
        reader.close();
        return cards;
    }

    private void validatePack() {
        // if there is not 8 cards per player then throw error
        if (pack.size() != 8 * numberOfPlayers) {
            throw new IllegalArgumentException("Invalid pack size. Expected " + (8 * numberOfPlayers) + " but got " + pack.size());
        }
    }

    private void initializeDecks() {
        // creates a deck for each player (a discard deck for one player is the draw deck for another so for n players, n decks)
        for (int i = 1; i <= numberOfPlayers; i++) {
            decks.add(new Deck(i));
        }
    }
    private void dealCards(){
        // Player draws cards from pack in round-robin
        Iterator<Card> packIterator = pack.iterator();
        for (int i = 0; i < 4; i++) {
            for (Player player : players) {
                player.drawCard(packIterator.next());
            }
        }

        // Remaining cards added to decks in round-robin
        while (packIterator.hasNext()) {
            for (Deck deck : decks) {
                if (packIterator.hasNext()) {
                    // we need to specify to the top otherwise the cards will be dealt backwards from the deck
                    deck.addCardToTop(packIterator.next());
                }
            }
        }
    }

    private void initializePlayers() throws IOException {
        // Deck and Drawdeck created for each player and player initialised
        for (int i = 1; i <= numberOfPlayers; i++) {
            Deck drawDeck = decks.get(i - 1);
            Deck discardDeck = decks.get(i % numberOfPlayers);
            players.add(new Player(drawDeck, discardDeck, i, gameWon, winner));
        }
    }

    private void writeDecksToFile() {
        // for each deck each card value is read and added to file in following format: deck n contents: 1 2 3 4
        for (Deck deck : decks) {
            try (PrintWriter writer = new PrintWriter("deck" + deck.getDeckNumber() + "_output.txt")) {
                StringBuilder deckContents = new StringBuilder();
                for (Card card : deck.getCurrentDeck()) {
                    deckContents.append(card.getValue()).append(" ");
                }
                writer.println("deck" + deck.getDeckNumber() + " contents: " + deckContents.toString().trim());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void play() {
        // Start all player threads
        List<Future<?>> futures = new ArrayList<>();
        for (Player player : players) {
            futures.add(threadPool.submit(player));
        }
        // Wait for game to end
        try {
            while (!gameWon.get()) {
                Thread.sleep(100);
            }

            // Stop all players and wait for them to finish
            players.forEach(Player::stopPlaying);
            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            // Once game is finished cancel all tasks and write decks to file
            threadPool.shutdownNow();
            writeDecksToFile();
        }
    }
    public static void main(String[] args) {
        // make scanner to read console input
        Scanner scanner = new Scanner(System.in);
        // get number from player input must be > 0 else throw error
        int numberOfPlayers = 0;
        while (numberOfPlayers <= 0) {
            System.out.print("Please enter the number of players: ");
            try {
                numberOfPlayers = Integer.parseInt(scanner.nextLine().trim());
                if (numberOfPlayers <= 0) {
                    System.out.println("Number of players must be positive.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        // get deck path from player input
        String packFile = null;
        while (packFile == null) {
            System.out.print("Please enter the pack file location: ");
            packFile = scanner.nextLine().trim();
            try {
                CardGame game = new CardGame(numberOfPlayers, packFile);
                game.play();
            } catch (IOException e) {
                System.out.println("Error reading pack file: " + e.getMessage());
                packFile = null;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid pack file: " + e.getMessage());
                packFile = null;
            }
        }

    }



}
