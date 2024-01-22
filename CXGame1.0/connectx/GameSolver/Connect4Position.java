package connectx.GameSolver;

public class Connect4Position {
    public static final int WIDTH = 7; // larghezza della scacchiera
    public static final int HEIGHT = 6; // altezza della scacchiera
    // private static final int ALIGNMENTS = 4;

    private long currentPosition;
    private long mask;
    private int moves;

    public Connect4Position() {
        currentPosition = 0L;
        mask = 0L;
        moves = 0;
    }

    public boolean canPlay(int col) {
        return (mask & topMask(col)) == 0;
    }

    public void play(int col) {
        currentPosition ^= mask;
        mask |= mask + bottomMask(col);
        moves++;
    }

    public int play(String seq) {
        for (int i = 0; i < seq.length(); i++) {
            int col = Character.getNumericValue(seq.charAt(i)) - 1;
            if (col < 0 || col >= WIDTH || !canPlay(col) || isWinningMove(col)) {
                return i; // mossa non valida
            }
            play(col);
        }
        return seq.length();
    }

    public boolean isWinningMove(int col) {
        long pos = currentPosition;
        pos |= (mask + bottomMask(col)) & columnMask(col);
        return alignment(pos);
    }

    public int getMoves() {
        return moves;
    }

    public long key() {
        return currentPosition + mask;
    }

    private static boolean alignment(long pos) {
        // orizzontale
        long m = pos & (pos >> (HEIGHT + 1));
        if ((m & (m >> (2 * (HEIGHT + 1)))) != 0)
            return true;

        // diagonale 1
        m = pos & (pos >> HEIGHT);
        if ((m & (m >> (2 * HEIGHT))) != 0)
            return true;

        // diagonale 2
        m = pos & (pos >> (HEIGHT + 2));
        if ((m & (m >> (2 * (HEIGHT + 2)))) != 0)
            return true;

        // verticale
        m = pos & (pos >> 1);
        if ((m & (m >> 2)) != 0)
            return true;

        return false;
    }

    private static long topMask(int col) {
        return (1L << (HEIGHT - 1)) << col * (HEIGHT + 1);
    }

    private static long bottomMask(int col) {
        return 1L << col * (HEIGHT + 1);
    }

    private static long columnMask(int col) {
        return ((1L << HEIGHT) - 1) << col * (HEIGHT + 1);
    }

    public static void main(String[] args) {
        Connect4Position position = new Connect4Position();

        // Esempio di gioco
        position.play("324324");

        // Stampa la tavola di gioco
        System.out.println("Numero di mosse: " + position.getMoves());
        System.out.println("Chiave della posizione: " + position.key());
    }
}
