package src;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Deck {
    private final int deckNumber;
    private final Deque<Card> cards;
    private final ReentrantLock lock;

    public Deck(int deckNumber) {
        this.deckNumber = deckNumber;
        this.cards = new LinkedList<>();
        this.lock = new ReentrantLock();
    }

    public Card drawCard() {
        // lock other threads to make sure the correct cards are drawn
        lock.lock();
        try{
            return cards.removeFirst();
        }
        catch(Exception e){
            // helps with testing when player has no one filling decks up
            return new Card(69);
        }
        finally {
            lock.unlock();
        }

    }
    public void addCardToTop(Card card) {
        // adds a card to the bottom the deck
        lock.lock();
        try{
            cards.addFirst(card);
        }
        finally {
            lock.unlock();
        }
    }
    public void addCard(Card card) {
        // adds a card to the bottom the deck
        lock.lock();
        try{
            cards.addLast(card);
        }
        finally {
            lock.unlock();
        }
    }

    public List<Card> getCurrentDeck(){
        lock.lock();
        try{
            return new ArrayList<>(cards);
        }
        finally {
            lock.unlock();
        }

    }

    public int getDeckNumber() {
        return deckNumber;
    }
}