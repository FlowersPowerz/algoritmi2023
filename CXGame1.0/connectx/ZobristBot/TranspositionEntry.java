package connectx.ZobristBot;

public class TranspositionEntry {

    private int eval;
    private int depth;
    private EntryType type;

    public TranspositionEntry(int evaluation, int depth, EntryType type) {
        this.eval = evaluation;
        this.depth = depth;
        this.type = type;
    }

    public int getEval() {
        return eval;
    }

    public int getDepth() {
        return depth;
    }

    public EntryType getType() {
        return type;
    }
}
