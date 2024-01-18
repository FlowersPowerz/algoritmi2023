package connectx.BirbaBot;
import java.util.HashMap;

public class TranspositionTable {
    private HashMap<Long, TranspositionEntry> table;

    public TranspositionTable() {
        this.table = new HashMap<>();
    }

    public TranspositionEntry get(long hash) {
        return table.get(hash);
    }

    public void insert(long hash, TranspositionEntry entry) {
        table.put(hash, entry);
    }
}