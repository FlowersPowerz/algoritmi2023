package connectx.ZobristBot;

import java.util.Random;

import connectx.CXCell;
import connectx.CXCellState;

public class ZobristTable {
    private final int M;
    private final int N;
    private long zobrist[][][];

    public ZobristTable(int rows, int columns) {
        this.M = rows;
        this.N = columns;
        zobrist = new long[M][N][2]; // size = rows * columns * # of pieces
        init_zobrist();
    }

    private void init_zobrist() {
        Random random = new Random();

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < 2; k++) {
                    zobrist[i][j][k] = random.nextLong();
                }
            }
        }
    }

    /**
     * Use after copying the official board into our board
     * 
     * @param board current stateBoard
     * @return the hash code for this board
     */
    public long hash(CXCellState[][] board) {
        long hash = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == CXCellState.P1) {
                    hash ^= zobrist[i][j][0];
                } else if (board[i][j] == CXCellState.P2) {
                    hash ^= zobrist[i][j][1];
                }
                // If cell is FREE, no contribution to hash
            }
        }
        return hash;
    }

    public long updateHash(long currentHash, CXCellState[][] board, CXCell cell, boolean undo) {
        if (undo) {
            // XOR out the old value
            if (board[cell.i][cell.j] != CXCellState.FREE) {
                currentHash ^= zobrist[cell.i][cell.j][board[cell.i][cell.j] == CXCellState.P1 ? 0 : 1];
            }
            return currentHash;
        } else {
            // XOR out the old value
            if (board[cell.i][cell.j] != CXCellState.FREE) {
                currentHash ^= zobrist[cell.i][cell.j][board[cell.i][cell.j] == CXCellState.P1 ? 0 : 1];
            }
            // XOR in the new value
            if (cell.state == CXCellState.P1) {
                currentHash ^= zobrist[cell.i][cell.j][0];
            } else if (cell.state == CXCellState.P2) {
                currentHash ^= zobrist[cell.i][cell.j][1];
            }
            return currentHash;
        }
        // 001011
        // 101110
        // ^
        // 100101
    }
}
