package connectx.BirbaBot;

import connectx.CXCell;

public class LabeledMove implements Comparable<LabeledMove> {
    private int value;
    private CXCell move;

    public LabeledMove(int label, CXCell cell) {
        this.value = label;
        this.move = cell;
    }

    public int getMove() {
        return this.move.j;
    }

    public CXCell getCell() {
        return move;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public int compareTo(LabeledMove other) {
        // Compare by value in ascending order (change the order if needed)
        return Integer.compare(other.getValue(), this.getValue());
    }
}