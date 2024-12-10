package src;

public class Card{
private final int value;
    public Card(int value){
        if(value < 0) throw new IllegalArgumentException();
        this.value = value;
    }
    public final int getValue(){return value;}
}