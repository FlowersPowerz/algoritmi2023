package connectx.BirbaBot;

import java.util.LinkedList;
import java.util.List;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;

public class Evaluate {
    private final int M, N, X;
    private CXCellState[][] tmpBoard;
    private CXBoard Board;

    private final int Vertical = 0;
    private final int Horizontal = 1;
    private final int PosDiagonal = 2;
    private final int NegDiagonal = 3;

    public Evaluate(int M, int N, int X, CXBoard B, CXCellState[][] tmpBoard) {
        this.M = M;
        this.N = N;
        this.X = X;
        this.Board = B;
        this.tmpBoard = tmpBoard;
    }

    public boolean isWinningPosition(CXCell cell, CXCellState[][] board) {
        int i = cell.i, j = cell.j;
        CXCellState s = board[i][j];
        int n;
        if (s == CXCellState.FREE)
            return false;

        // Horizontal check
        n = 1;
        for (int k = 1; j - k >= 0 && board[i][j - k] == s; k++)
            n++; // backward check
        for (int k = 1; j + k < N && board[i][j + k] == s; k++)
            n++; // forward check
        if (n >= X)
            return true;

        // Vertical check
        n = 1;
        for (int k = 1; i + k < M && board[i + k][j] == s; k++)
            n++;
        if (n >= X)
            return true;

        // Diagonal check
        n = 1;
        for (int k = 1; i - k >= 0 && j - k >= 0 && board[i - k][j - k] == s; k++)
            n++; // backward check
        for (int k = 1; i + k < M && j + k < N && board[i + k][j + k] == s; k++)
            n++; // forward check
        if (n >= X)
            return true;

        // Anti-diagonal check
        n = 1;
        for (int k = 1; i - k >= 0 && j + k < N && board[i - k][j + k] == s; k++)
            n++; // backward check
        for (int k = 1; i + k < M && j - k >= 0 && board[i + k][j - k] == s; k++)
            n++; // forward check
        if (n >= X)
            return true;

        return false;
    }

    public int get_helpfulness(CXCell cell) {
        return evaluateColumn(cell, CXCellState.P1)
                + evaluateColumn(cell, CXCellState.P2);
    }

    /**
     * 
     * @param cell
     * @param player
     * @return
     */
    public int evaluateColumn(CXCell cell, CXCellState player) {
        int helpfulness = 0;
        boolean diagonals = true;
        // placing the piece where we are contemplating is done by alphabeta
        if (M >= X) {
            helpfulness += count(cell, player, Vertical);
        } else
            diagonals = false;
        if (N >= X) {
            helpfulness += count(cell, player, Horizontal);
        } else
            diagonals = false;
        if (diagonals)
            helpfulness += count(cell, player, PosDiagonal) + count(cell, player, NegDiagonal);
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
    private int count(CXCell cell, CXCellState player, int area) {
        if (area == Vertical) {
            int val, lower;
            int upperBound = Math.max(0, cell.i - X + 1);
            int lowerBound = Math.min(M - 1, cell.i + X - 1);
            // vado a trovare il range massimo di celle disponibili per player
            // sotto alla mossa appena fatta ci sono i gettoni dei giocatori, sopra sono
            // celle libere
            for (lower = cell.i; lower < lowerBound; lower++) {
                CXCellState p = tmpBoard[lower][cell.j + 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            if (lower - upperBound + 1 < X)
                return 0;
            else {
                int max_config = val = lower - upperBound - X + 1, max = 0;
                while (lower < cell.i) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[lower][cell.j] == player)
                        val += max;
                    lower++;
                }
            }
            return val;
        }
        if (area == Horizontal) {
            int right, left, val;
            int leftBound = Math.min(0, cell.j - X + 1);
            int rightBound = Math.max(N - 1, cell.j + X - 1);
            // la furbata per il check verticale non si può fare, tocca trovare il range e
            // poi valutare
            for (left = cell.j; left > leftBound; left--) {
                CXCellState p = tmpBoard[cell.i][left - 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            for (right = cell.j; right < rightBound; right++) {
                CXCellState p = tmpBoard[cell.i][right + 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            if (right - left + 1 < X)
                return 0;
            else {
                int max_config = right - left - X + 1, max = 0;
                val = max_config;
                while (left < cell.j) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[left][cell.j] == player)
                        val += max;
                    left++;
                }
                while (cell.j > right) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[right][cell.j] == player)
                        val += max;
                    right--;
                }
                return val;
            }
        }
        if (area == PosDiagonal) {
            // diagonale positiva è così: (/)
            int right, left, upper, lower, val;
            int leftBound = Math.min(0, cell.j - X + 1);
            int rightBound = Math.max(N - 1, cell.j + X - 1);
            int upperBound = Math.max(0, cell.i - X + 1);
            int lowerBound = Math.min(M - 1, cell.i + X - 1);

            for (lower = cell.i, left = cell.j; left > leftBound && lower < lowerBound; left--, lower++) {
                CXCellState p = tmpBoard[lower + 1][left - 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            for (upper = cell.i, right = cell.j; right < rightBound && upper > upperBound; right++, upper--) {
                CXCellState p = tmpBoard[upper - 1][right + 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            if (upper - lower + 1 < X)
                return 0;
            else {
                int max_config = upper - lower - X + 1, max = 0;
                val = max_config;
                while (lower > cell.i) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[lower][left] == player)
                        val += max;
                    lower--;
                    left++;
                }
                while (upper < cell.i) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[upper][right] == player)
                        val += max;
                    upper++;
                    right--;
                }
                return val;
            }
        } else {
            // la diagonale negativa è così: (\)
            int right, left, upper, lower, val;
            int leftBound = Math.min(0, cell.j - X + 1);
            int rightBound = Math.max(N - 1, cell.j + X - 1);
            int upperBound = Math.max(0, cell.i - X + 1);
            int lowerBound = Math.min(M - 1, cell.i + X - 1);

            for (lower = cell.i, right = cell.j; right < rightBound && lower < lowerBound; right++, lower++) {
                CXCellState p = tmpBoard[lower + 1][right + 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            for (upper = cell.i, left = cell.j; left > leftBound && upper > upperBound; left--, upper--) {
                CXCellState p = tmpBoard[upper - 1][left - 1];
                if (p != player || p != CXCellState.FREE)
                    break;
            }
            if (upper - lower + 1 < X)
                return 0;
            else {
                int max_config = upper - lower - X + 1, max = 0;
                val = max_config;
                while (lower > cell.i) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[lower][right] == player)
                        val += max;
                    lower--;
                    right--;
                }
                while (upper < cell.i) {
                    if (max < max_config)
                        max++;
                    if (tmpBoard[upper][left] == player)
                        val += max;
                    upper++;
                    left++;
                }
                return val;
            }
        }
    }
}
