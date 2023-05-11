package connectx.ReadyPlayerOne;

import connectx.CXPlayer;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
public class ReadyPlayerOne implements CXPlayer {
	private int ROWS_M, COLUMNS_N, TO_CONNECT_X; //M rows, N columns, connect X
	private boolean first; //who am i?
	private int timeout_in_secs;
	private long startTime;

	//constructor
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.ROWS_M = M;
		this.COLUMNS_N = N;
		this.TO_CONNECT_X = X;
		this.first = first;
		this.timeout_in_secs = timeout_in_secs;
	}

	//"main function" we need to implement
	public int selectColumn(CXBoard Board) {
		startTime = System.currentTimeMillis();
		int bestMove = -1;
		for (int depth = 1; !timeIsUp(); depth++) {
			bestMove = iterativeDeepening(Board, depth);
		}
		return bestMove;
	}

	public Integer[] sortColumnsByHeuristic(CXBoard board, boolean maximizingPlayer) {
		Integer[] availableColumns = board.getAvailableColumns();
		Integer[] columnOrder = new Integer[availableColumns.length];
		Double[] columnValues = new Double[availableColumns.length];

		for (int i = 0; i < availableColumns.length; i++) {
			int column = availableColumns[i];
			CXBoard newBoard = board.copy();
			newBoard.markColumn(column);
			columnValues[i] = (double) evaluateBoard(newBoard);
		}

		Integer[] columnIndices = IntStream.range(0, availableColumns.length).boxed().toArray(Integer[]::new);
		Arrays.sort(columnIndices, (a, b) -> Double.compare(columnValues[b], columnValues[a]));

		for (int i = 0; i < columnOrder.length; i++) {
			columnOrder[i] = availableColumns[columnIndices[i]];
		}

		return columnOrder;
	}
/*
* function IterativeDeepening(Node T , bool playerA, int depth) → int

α = MinAlpha
β = MaxBeta
eval = 0
for d = 0, · · · , depth do
if time is running out() then break
eval =AlphaBeta(T , playerA,α,β,d)
return eval
*/
	private int iterativeDeepening(CXBoard Board) {
		//TODO
		int bestValue = Integer.MIN_VALUE;
		int bestMove = -1;
		Integer[] columnOrder = sortColumnsByHeuristic(Board, true);

		for (int col : columnOrder) {
			CXBoard copy = Board.copy();
			copy.markColumn(col);
			int value = minimax(copy, 0, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (value > bestValue) {
				bestValue = value;
				bestMove = col;
			}
			if (timeIsUp()) {
				break;
			}
		}
		return bestMove;
	}

	public String playerName() {
		return "ReadyPlayerOne";
	}

	private int minimax(CXBoard board, int depth, boolean isMinimizing, int alpha, int beta) {
		if ( timeIsUp()) {
			return evaluateBoard(board);
		}

		CXGameState gameState = board.gameState();
		if (gameState == CXGameState.WINP1 || gameState == CXGameState.WINP2) {
			return isMinimizing ? 1 : -1;
		} else if (gameState == CXGameState.DRAW) {
			return 0;
		}

		int bestValue = Integer.MIN_VALUE;
		for (int col : board.getAvailableColumns()) {
			CXBoard copy = board.copy();
			copy.markColumn(col);
			int value = minimax(copy, depth + 1, isMinimizing, alpha, beta );
			if(isMinimizing) {
				bestValue = Math.max(bestValue, value);
				alpha = Math.max(alpha, bestValue);
			} else {
				bestValue = Math.min(bestValue, value);
				beta = Math.min(beta, bestValue);
			}
			if (beta <= alpha) {
				break;
			}
		}
		return bestValue;
	}

	private boolean timeIsUp() {
		long elapsedTime = System.currentTimeMillis() - startTime;
		return elapsedTime >= (timeout_in_secs - 1) * 1000;
	}

	// ???
	private int[][] createPositionValuesMatrix() {
		int[][] positionValues = new int[ROWS_M][COLUMNS_N];
		int maxDistance = (ROWS_M + COLUMNS_N) / 2;

		for (int row = 0; row < ROWS_M; row++) {
			for (int col = 0; col < COLUMNS_N; col++) {
				int distanceRow = Math.min(row, ROWS_M - 1 - row);
				int distanceCol = Math.min(col, COLUMNS_N - 1 - col);
				int distance = Math.min(distanceRow, distanceCol);
				positionValues[row][col] = maxDistance - distance;
			}
		}

		return positionValues;
	}

	private int evaluateBoard(CXBoard board) {
		int score = 0;
		final int[] DIRECTIONS = {-1, 0, 1};
		int[][] positionValues = createPositionValuesMatrix();

		for (int row = 0; row < ROWS_M; row++) {
			for (int col = 0; col < COLUMNS_N; col++) {
				CXCellState cellState = board.cellState(row, col);
				if (cellState != CXCellState.FREE) {
					int cell = (cellState == CXCellState.P1) ? 1 : -1;
					score += cell * positionValues[row][col];

					for (int dr : DIRECTIONS) {
						for (int dc : DIRECTIONS) {
							if (dr != 0 || dc != 0) {
								int countConsecutive = 1;
								int countOpenEnds = 0;

								int r = row + dr; //direzione per le righe
								int c = col + dc; //direzione per le colonne
								while (r >= 0 && r < ROWS_M && c >= 0 && c < COLUMNS_N && board.cellState(r, c) == cellState) {
									countConsecutive++;
									r += dr;
									c += dc;
								}
								if (r >= 0 && r < ROWS_M && c >= 0 && c < COLUMNS_N && board.cellState(r, c) == CXCellState.FREE) {
									countOpenEnds++;
								}

								int scoreFactor = (cell == 1) ? 1 : -1;
								if (countConsecutive >= TO_CONNECT_X) {
									return countConsecutive * 1000 * scoreFactor;
								} else if (countConsecutive == TO_CONNECT_X - 1 && countOpenEnds > 0) {
									score += 100 * scoreFactor;
								} else if (countConsecutive == TO_CONNECT_X - 2) {
									score += 10 * scoreFactor;
								} else if (countConsecutive == TO_CONNECT_X - 3) {
									score += 2 * scoreFactor;
								}
							}
						}
					}
				}
			}
		}

		return score;
	}


}