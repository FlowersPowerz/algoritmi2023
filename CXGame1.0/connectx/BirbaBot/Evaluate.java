package connectx.BirbaBot;

import java.util.LinkedList;
import java.util.List;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;

public class Evaluate {
    private final int M, N, X;
    // private CXCellState[][] stateBoard;

    private final int Vertical = 0;
    private final int Horizontal = 1;
    private final int PosDiagonal = 2;
    private final int NegDiagonal = 3;

    public Evaluate(int M, int N, int X) {
        this.M = M;
        this.N = N;
        this.X = X;
        // this.stateBoard = stateBoard;
    }

    public int get_helpfulness(CXCellState[][] stateBoard, CXCell cell) {
        int helpfulness = evaluateColumn(stateBoard, cell, CXCellState.P1)
                + evaluateColumn(stateBoard, cell, CXCellState.P2);
        return helpfulness;
    }

    public int evaluateColumn(CXCellState[][] stateBoard, CXCell cell, CXCellState player) {
        int helpfulness = 0;
        boolean diagonals = false;
        // placing the piece where we are contemplating is done by alphabeta
        if (M >= X) {
            helpfulness += count(stateBoard, cell, player, Vertical);
            diagonals = true;
        }
        if (N >= X) {
            helpfulness += count(stateBoard, cell, player, Horizontal);
            diagonals = true;
        }
        if (diagonals) {
            helpfulness += count(stateBoard, cell, player, PosDiagonal) + count(stateBoard, cell, player, NegDiagonal);
        }
        return helpfulness;
    }

    /**
     * DISCLAIMER: Board grid origin(0,0) is on the upper-left corner.
     * Returns the helpfulness value from the <code> player </code>'s perspective
     * looking only through a specified <code> area </code> of the grid
     * 
     * @param cell
     * @param player
     * @param area
     * @return <code> cell </code>'s value within grid's area
     */
    private int count(CXCellState[][] stateBoard, CXCell cell, CXCellState player, int area) {
        switch (area) {
            case Vertical: {
                int val, lower;
                int upperBound = Math.max(0, cell.i - X + 1);
                int lowerBound = Math.min(M - 1, cell.i + X - 1);
                // vado a trovare il range massimo di celle disponibili per player
                // sotto alla mossa appena fatta ci sono i gettoni dei giocatori, sopra sono
                // celle libere
                for (lower = cell.i; lower < lowerBound; lower++) {
                    CXCellState p = stateBoard[lower + 1][cell.j];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }

                if (lower - upperBound + 1 < X)
                    return 0;

                int max_config = lower - upperBound - X + 2, max = 0;
                val = max_config;
                while (lower > cell.i) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[lower][cell.j] == player)
                        val += max;
                    lower--;
                }
                return val;
            }
            case Horizontal: {
                int right, left, val;
                int leftBound = Math.max(0, cell.j - X + 1);
                int rightBound = Math.min(N - 1, cell.j + X - 1);
                // la furbata per il check verticale non si può fare, tocca trovare il range e
                // poi valutare
                for (left = cell.j; left > leftBound; left--) {
                    CXCellState p = stateBoard[cell.i][left - 1];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }
                for (right = cell.j; right < rightBound; right++) {
                    CXCellState p = stateBoard[cell.i][right + 1];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }

                if (right - left + 1 < X)
                    return 0;

                int max_config = right - left - X + 2, max = 0;
                val = max_config;
                while (left < cell.j) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[cell.i][left] == player) {
                        val += max;
                    }
                    left++;
                }
                max = 0;
                while (right > cell.j) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[cell.i][right] == player) {
                        val += max;
                    }
                    right--;
                }
                return val;
            }
            case PosDiagonal: {
                // diagonale positiva è così: (/)
                int right, left, upper, lower, val;
                int leftBound = Math.max(0, cell.j - X + 1);
                int rightBound = Math.min(N - 1, cell.j + X - 1);
                int upperBound = Math.max(0, cell.i - X + 1);
                int lowerBound = Math.min(M - 1, cell.i + X - 1);

                for (lower = cell.i, left = cell.j; left > leftBound && lower < lowerBound; left--, lower++) {
                    CXCellState p = stateBoard[lower + 1][left - 1];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }
                for (upper = cell.i, right = cell.j; right < rightBound && upper > upperBound; right++, upper--) {
                    CXCellState p = stateBoard[upper - 1][right + 1];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }

                if (lower - upper + 1 < X) {
                    return 0;
                }

                int max_config = lower - upper - X + 2, max = 0;
                val = max_config;
                while (lower > cell.i) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[lower][left] == player)
                        val += max;
                    lower--;
                    left++;
                }
                max = 0;
                while (upper < cell.i) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[upper][right] == player)
                        val += max;
                    upper++;
                    right--;
                }
                return val;
            }
            default: {
                // la diagonale negativa è così: (\)
                int right, left, upper, lower, val;
                int leftBound = Math.max(0, cell.j - X + 1);
                int rightBound = Math.min(N - 1, cell.j + X - 1);
                int upperBound = Math.max(0, cell.i - X + 1);
                int lowerBound = Math.min(M - 1, cell.i + X - 1);

                for (lower = cell.i, right = cell.j; right < rightBound && lower < lowerBound; right++, lower++) {
                    CXCellState p = stateBoard[lower + 1][right + 1];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }
                for (upper = cell.i, left = cell.j; left > leftBound && upper > upperBound; left--, upper--) {
                    CXCellState p = stateBoard[upper - 1][left - 1];
                    if (p != player && p != CXCellState.FREE)
                        break;
                }

                if (lower - upper + 1 < X)
                    return 0;

                int max_config = lower - upper - X + 2, max = 0;
                val = max_config;
                while (lower > cell.i) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[lower][right] == player)
                        val += max;
                    lower--;
                    right--;
                }
                max = 0;
                while (upper < cell.i) {
                    if (max < max_config)
                        max++;
                    if (stateBoard[upper][left] == player)
                        val += max;
                    upper++;
                    left++;
                }
                return val;
            }
        }
    }
}
