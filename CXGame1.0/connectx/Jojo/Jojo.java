package connectx.Jojo;
import java.io.*;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGameState;

public class Jojo implements CXPlayer {

    // variabili di istanza
    private int M;
    private int N;
    private int X;
    private boolean first;
    private int timeout;
    public Jojo (){};

    @Override
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        this.M = M;
        this.N = N;
        this.X = X;
        this.first = first;
        this.timeout = timeout_in_secs;
    }

    @Override
    public int selectColumn(CXBoard B) {
        // qui implementerai l'algoritmo per la scelta della colonna
        System.err.println("not implemented");
        // Prepara una lista per memorizzare i punteggi delle colonne
        int[] scores = new int[N];
        for (int col = 0; col < N; col++) {
            if (!B.fullColumn(col)) {
                // Simula la mossa nella colonna
                B.markColumn(col);
                // Valuta la mossa
                scores[col] = evaluateBoard(B);
                // Annulla la mossa simulata
                B.unmarkColumn();
            } else {
                // Se la colonna è piena, assegna un punteggio molto basso
                scores[col] = Integer.MIN_VALUE;
            }
        }

        // Scegli la colonna con il punteggio più alto
        int bestCol = 0;
        int bestScore = scores[0];
        for (int col = 1; col < N; col++) {
            if (scores[col] > bestScore) {
                bestScore = scores[col];
                bestCol = col;
            }
        }

        return bestCol;
    }

    public int evaluateBoard( CXBoard B ){

    }

    @Override
    public String playerName() {
        return "Jojo";  // sostituisci con il nome del tuo giocatore
    }

}
