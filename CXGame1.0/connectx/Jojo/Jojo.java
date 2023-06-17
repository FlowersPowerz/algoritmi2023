package connectx.Jojo;
import java.io.*;
import java.util.Random;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;

public class Jojo implements CXPlayer {
    private int numeroRighe;
    private int numeroColonne;
    private int numeroGettoni;
    private boolean primoGiocatore;
    private int timeout_in_secs;
    private int giocatore;
public Jojo(){};

    @Override
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        this.numeroRighe = M;
        this.numeroColonne = N;
        this.numeroGettoni = X;
        this.primoGiocatore = first;
        this.timeout_in_secs = timeout_in_secs;
        this.giocatore = first ? 1 : 2;
    }

    @Override
    public int selectColumn(CXBoard tabellone) {
        Integer[] colonneDisponibili = tabellone.getAvailableColumns();
        int colonnaMigliore = colonneDisponibili[new Random().nextInt(colonneDisponibili.length)];  // inizializzazione casuale
        double punteggioMigliore = Double.NEGATIVE_INFINITY;
        long tempoLimite = System.currentTimeMillis() + this.timeout_in_secs * 1000;

        for (int profondita = 1; System.currentTimeMillis() < tempoLimite; profondita++) {
            for (int colonna : colonneDisponibili) {
                tabellone.markColumn(colonna);
                double punteggio = ricercaIterativeDeepening(tabellone, profondita, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, tempoLimite, false);
                tabellone.unmarkColumn();
                if (punteggio > punteggioMigliore) {
                    punteggioMigliore = punteggio;
                    colonnaMigliore = colonna;
                }
            }
        }

        return colonnaMigliore;
    }

    private double ricercaIterativeDeepening(CXBoard tabellone, int profondita, double alfa, double beta, long tempoLimite, boolean turnoGiocatore) {
        if (System.currentTimeMillis() >= tempoLimite || profondita == 0 || tabellone.gameState() != CXGameState.OPEN) {
            return valutaTabellone(tabellone);
        }

        if (turnoGiocatore) {
            double punteggioMax = Double.NEGATIVE_INFINITY;
            for (int colonna : tabellone.getAvailableColumns()) {
                tabellone.markColumn(colonna);
                double punteggio = ricercaIterativeDeepening(tabellone, profondita - 1, alfa, beta, tempoLimite, false);
                tabellone.unmarkColumn();
                punteggioMax = Math.max(punteggioMax, punteggio);
                alfa = Math.max(alfa, punteggio);
                if (alfa >= beta) {
                    break;
                }
            }
            return punteggioMax;
        } else {
            double punteggioMin = Double.POSITIVE_INFINITY;
            for (int colonna : tabellone.getAvailableColumns()) {
                tabellone.markColumn(colonna);
                double punteggio = ricercaIterativeDeepening(tabellone, profondita - 1, alfa, beta, tempoLimite, true);
                tabellone.unmarkColumn();
                punteggioMin = Math.min(punteggioMin, punteggio);
                beta = Math.min(beta, punteggio);
                if (alfa >= beta) {
                    break;
                }
            }
            return punteggioMin;
        }
    }

    private double valutaTabellone(CXBoard tabellone) {
        if (tabellone.gameState() == CXGameState.WINP1 && primoGiocatore || tabellone.gameState() == CXGameState.WINP2 && !primoGiocatore) {
            return Double.POSITIVE_INFINITY;
        } else if (tabellone.gameState() == CXGameState.WINP1 && !primoGiocatore || tabellone.gameState() == CXGameState.WINP2 && primoGiocatore) {
            return Double.NEGATIVE_INFINITY;
        } else {
            int gettoniGiocatore = 0;
            int gettoniAvversario = 0;
            for (CXCell cella : tabellone.getMarkedCells()) {
                if (tabellone.currentPlayer() == giocatore) {
                    gettoniGiocatore++;
                } else {
                    gettoniAvversario++;
                }
            }
            return gettoniGiocatore - gettoniAvversario;
        }
    }

    @Override
    public String playerName() {
        return "Jojo";
    }
}
