package connectx.ZobristBot;

import java.util.HashMap;

public class TranspositionTable {
    private HashMap<Long, TranspositionEntry> table;
    private final int size = 1000000;

    public TranspositionTable() {
        this.table = new HashMap<>(size);
    }

    public TranspositionEntry search(long key) {
        return table.get(key);
    }

    public void insert(long key, TranspositionEntry entry) {
        table.put(key, entry);
    }

    public void delete(long key) {
        table.remove(key);
    }

    public int getSize() {
        return table.size();
    }

    public void clear() {
        table.clear();
    }
}