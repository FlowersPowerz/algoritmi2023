package connectx;

import java.util.List;
import java.util.ArrayList;

public class ReadyPlayerOne implements CXPlayer {
	private int M, N, X;
	private boolean first;
	private int timeout_in_secs;
	private long startTime;
	private int maxDepth;

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;
		this.first = first;
		this.timeout_in_secs = timeout_in_secs;
		setMaxDepth();
	}

	public int selectColumn(CXBoard B) {
		startTime = System.currentTimeMillis();
		int bestMove = -1;
		for (int depth = 1; !timeIsUp(); depth++) {
			bestMove = iterativeDeepening(B, depth);
		}
		return bestMove;
	}

	public int[] sortColumnsByHeuristic(CXBoard board, boolean maximizingPlayer) {
		List<Integer> availableColumns = board.getAvailableColumns();
		int[] columnOrder = new int[availableColumns.size()];
		Double[] columnValues = new Double[availableColumns.size()];

		for (int i = 0; i < availableColumns.size(); i++) {
			int column = availableColumns.get(i);
			CXBoard newBoard = board.copy();
			newBoard.markColumn(column);
			columnValues[i] = (double) evaluateBoard(newBoard);
		}

		Integer[] columnIndices = IntStream.range(0, availableColumns.size()).boxed().toArray(Integer[]::new);
		Arrays.sort(columnIndices, (a, b) -> Double.compare(columnValues[b], columnValues[a]));

		for (int i = 0; i < columnOrder.length; i++) {
			columnOrder[i] = availableColumns.get(columnIndices[i]);
		}

		return columnOrder;
	}

	private int iterativeDeepening(CXBoard B, int maxDepth) {
		int bestValue = Integer.MIN_VALUE;
		int bestMove = -1;
		int[] columnOrder = sortColumnsByHeuristic(B, true);

		for (int col : columnOrder) {
			CXBoard copy = B.copy();
			copy.markColumn(col);
			int value = minimax(copy, 0, false, Integer.MIN_VALUE, Integer.MAX_VALUE, maxDepth);
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
		return "MyConnectXPlayer";
	}

	private int minimax(CXBoard board, int depth, boolean isMaximizing, int alpha, int beta, int maxDepth) {
		if (depth >= maxDepth || timeIsUp()) {
			return evaluateBoard(board);
		}

		CXGameState gameState = board.gameState();
		if (gameState == CXGameState.WINP1 || gameState == CXGameState.WINP2) {
			return isMaximizing ? -1 : 1;
		} else if (gameState == CXGameState.DRAW) {
			return 0;
		}

		if (isMaximizing) {
			int bestValue = Integer.MIN_VALUE;
			for (int col : board.getAvailableColumns()) {
				CXBoard copy = board.copy();
				copy.markColumn(col);
				int value = minimax(copy, depth + 1, false, alpha, beta, maxDepth);
				bestValue = Math.max(bestValue, value);
				alpha = Math.max(alpha, bestValue);
				if (beta <= alpha) {
					break;
				}
			}
			return bestValue;
		} else {
			int bestValue = Integer.MAX_VALUE;
			for (int col : board.getAvailableColumns()) {
				CXBoard copy = board.copy();
				copy.markColumn(col);
				int value = minimax(copy, depth + 1, true, alpha, beta, maxDepth);
				bestValue = Math.min(bestValue, value);
				beta = Math.min(beta, bestValue);
				if (beta <= alpha) {
					break;
				}
			}
			return bestValue;
		}
	}

	private boolean timeIsUp() {
		long elapsedTime = System.currentTimeMillis() - startTime;
		return elapsedTime >= (timeout_in_secs - 1) * 1000;
	}

	private int evaluateBoard(CXBoard board) {
		int score = 0;
		final int[] DIRECTIONS = {-1, 0, 1};
		int[][] positionValues = createPositionValuesMatrix();

		for (int row = 0; row < M; row++) {
			for (int col = 0; col < N; col++) {
				CXCellState cellState = board.cellState(row, col);
				if (cellState != CXCellState.FREE) {
					int cell = (cellState == CXCellState.P1) ? 1 : -1;
					score += cell * positionValues[row][col];

					for (int dr : DIRECTIONS) {
						for (int dc : DIRECTIONS) {
							if (dr != 0 || dc != 0) {
								int countConsecutive = 1;
								int countOpenEnds = 0;

								int r = row + dr;
								int c = col + dc;
								while (r >= 0 && r < M && c >= 0 && c < N && board.cellState(r, c) == cellState) {
									countConsecutive++;
									r += dr;
									c += dc;
								}
								if (r >= 0 && r < M && c >= 0 && c < N && board.cellState(r, c) == CXCellState.FREE) {
									countOpenEnds++;
								}

								int scoreFactor = (cell == 1) ? 1 : -1;
								if (countConsecutive >= X) {
									return countConsecutive * 1000 * scoreFactor;
								} else if (countConsecutive == X - 1 && countOpenEnds > 0) {
									score += 100 * scoreFactor;
								} else if (countConsecutive == X - 2) {
									score += 10 * scoreFactor;
								} else if (countConsecutive == X - 3) {
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

	private int[][] createPositionValuesMatrix() {
		// Inizializza la matrice con i valori che ritieni appropriati per le posizioni
		int[][] positionValues = new int[M][N];

		// Aggiungi il tuo codice qui per popolare la matrice con i valori delle posizioni

		return positionValues;
	}

	private void setMaxDepth() {
		int gridSize = M * N;
		float cellsPerSecond = (float) gridSize / timeout_in_secs;
		if (cellsPerSecond <= 2) {
			maxDepth = 4;
		} else if (cellsPerSecond <= 4) {
			maxDepth = 6;
		} else if (cellsPerSecond <= 8) {
			maxDepth = 8;
		} else {
			maxDepth = 10;
		}
	}
}