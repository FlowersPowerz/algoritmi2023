package connectx.BirbaBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * BucketList:
 * 1) Evaluate
 * 	- Euristic function to calculate the relative value of any column.
 * 2) Search 
 * 	- AlphaBeta with Iterative Deepening
 *  - game Tree nodes sorting by best
 *	- Transposition Table
 *
 * TO DO: 
 * [.] Evaluate con Euristica basta sulla helpfulness
 * [.] Transposition Table con le mosse già computate nei turni precedenti in modo da non visitare 
 *     ogni volta un nuovo Game Tree
 * [.] AlphaBeta + Iterative Deepening
 * [.] Move ordering
 */

/**
 * Our Player
 */
public class BirbaBot implements CXPlayer {
	private final int WIN = 100000;
	private final int LOSS = -100000;
	private int TIMEOUT;
	private long START;
	private boolean first;
	private int M, N, X; // M righe, N colonne, X allineamenti
	private CXBoard Board; // variabile x utilizzare le funzioni di CXBoard
	private CXCellState[][] tmpBoard;
	// private Set<CXCell> MC; // Marked Cells hash
	private CXGameState myWin, yourWin;
	private CXCellState me, opponent;
	private TreeNode bestMove, root; // miglior mossa trovata e radice del game tree
	private int nodeCount;
	boolean meFirst_secondturn = false;

	/**
	 * Board grid origin(0,0) is on the upper-left corner
	 */
	enum Grid {
		Vertical, Horizontal, PosDiagonal, NegDiagonal
	};

	/* Default empty constructor */
	public BirbaBot() {
	}

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;

		tmpBoard = new CXCellState[M][N];

		TIMEOUT = timeout_in_secs;
		this.first = first;
		myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		me = first ? CXCellState.P1 : CXCellState.P2;
		opponent = first ? CXCellState.P2 : CXCellState.P1;

		bestMove = null;
		nodeCount = 0;
		root = null;
	}

	/* Selects the best move possible */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time
		nodeCount = 0;
		Board = B.copy();
		// Siamo il player P1, nostro primo turno
		if (first && root == null) {
			// la letteratura ci dice che la prima mossa migliore è sempre la colonna di
			// mezzo
			Board.markColumn(N / 2);
			// creo la radice
			root = new TreeNode(Board.getLastMove());
			meFirst_secondturn = true;
			// non mi serve la best move, posso usare alphabeta per creare il game tree
			// AlphaBeta(root, opponent, -Integer.MAX_VALUE, Integer.MAX_VALUE);
			// System.err.println("nodi visitati: " + nodeCount);
			// System.err.println("bestMove: " + bestMove.getCell().j);
			// System.err.println("bestMOveValue: " + bestMove.getLabel());
			// for (TreeNode i : root.getChildNodes())
			// System.err.println("Valore dei figli di root: " + i.getLabel());
			return N / 2;
		}
		// siamo il player P2, nostro primo turno
		else if (!first && root == null) {
			// l'ultima mossa fatta dall'avversario è la radice del mio albero di gioco
			root = new TreeNode(B.getLastMove());
			try {
				AlphaBetaStart(root, me, -Integer.MAX_VALUE, Integer.MAX_VALUE);
			} catch (Exception e) {
				System.err.println("Scelgo una colonna centrale");
				B.markColumn(N / 2);
				root = new TreeNode(B.getLastMove());
				return N / 2;
			}
			saveMove(bestMove);
			return bestMove.getCell().j;
		} else {
			if (meFirst_secondturn) {
				meFirst_secondturn = false;
				System.err.println("siamo partiti per primi ed è il nostro secondo turno: genero l'albero");
				root = new TreeNode(B.getLastMove());
				try {
					AlphaBetaStart(root, me, -Integer.MAX_VALUE, Integer.MAX_VALUE);
				} catch (Exception e) {
					System.err.println("Scelgo una colonna centrale se disponibile");
					System.err.println("Nodi visitati: " + nodeCount);
					if (!B.fullColumn(N / 2)) {
						B.markColumn(N / 2);
						root = new TreeNode(B.getLastMove());
						return N / 2;
					} else {
						System.err.println("column full, first column available is selected");
						// pedantic check when table is very small(e.g.: 2x2, 3x3)
						Integer[] A = B.getAvailableColumns();
						return A[0];
					}
				}
				saveMove(bestMove);
				return bestMove.getCell().j;
			} else {
				// ho già una parte del game tree valutato, ma può capitare una configurazione
				// non ancora vista, quindi controllo
				// se il nodo della nostra mossa precedente contiene la mossa appena fatta
				// dall'avversario
				TreeNode lastOppMove = root.getChildByCell(B.getLastMove());
				if (lastOppMove == null) {
					// la mossa non è stata trovata: sposto la radice del game tree sul nuovo nodo
					// che non è stato valutato
					System.err.println("Cache miss!");
					root = new TreeNode(B.getLastMove());
					try {
						AlphaBetaStart(root, me, -Integer.MAX_VALUE, Integer.MAX_VALUE);
					} catch (Exception e) {
						System.err.println("Scelgo una colonna centrale se disponibile");
						// codice temporaneo, verrà sostituito da una funzione EvaluateGame(),
						// basato su euristica
						if (!B.fullColumn(N / 2)) {
							B.markColumn(N / 2);
							return N / 2;
						} else {
							Integer[] A = B.getAvailableColumns();
							return A[0];
						}
					}
					saveMove(bestMove);
					return bestMove.getCell().j;
				} else {
					// ho trovato la mossa già valutata nel game tree: ritorno la best move del nodo
					// N.B.: i figli di lastOppMove potrebbero non essere stati generati tutti per
					// via delle potature
					System.err.println("Cache Hit!");

					bestMove = lastOppMove.getChild(0);
					bestMove.label = lastOppMove.getChild(0).label;
					for (TreeNode it : lastOppMove.getChildNodes()) {
						System.err.println("bestMove label: " + bestMove.label);
						System.err.println("it label: " + it.label);
						if (it.label > bestMove.label) {
							bestMove = it;
							bestMove.label = it.label;
						}
					}
					saveMove(lastOppMove);
					return bestMove.getCell().j;
				}
			}
		}
	}

	/**
	 * Comincia l'algoritmo alphabeta partendo dal giocatore che massimizza, ovvero
	 * il
	 * nostro player aggiornando il campo BestMove con la mossa migliore da fare
	 * dopo il nodo T
	 * Questa funzione va chiamata in <code> SelectCell </code> al posto di
	 * AlphaBeta
	 * per avere la bestMove nel nostro turno
	 * 
	 * @param T
	 * @param player
	 * @param alpha
	 * @param beta
	 */
	private void AlphaBetaStart(TreeNode T, CXCellState player, int alpha, int beta) {
		nodeCount++;
		checktime();
		int eval = -Integer.MAX_VALUE; // eval = -oo
		int bestMoveValue = eval;
		// maximizing player
		T.GenerateMoveList(Board);
		System.err.println("Numero di mosse contemplabili: " + T.getMovesnumber());
		for (int i : T.getMoves()) { // foreach move in T.list
			// make this move in the list
			Board.markColumn(i);
			// add this move to the game tree
			TreeNode child = new TreeNode(Board.getLastMove());
			if (Board.gameState() != CXGameState.OPEN)
				child.updateLeaf();
			T.addChild(child);
			// evaluate this move by taking the max value between current eval and value
			// calculated by Alphabeta
			eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta));
			Board.unmarkColumn();

			if (eval > bestMoveValue) {
				bestMove = child;
				bestMoveValue = eval;
				T.label = bestMoveValue;
			}
		}
	}

	/**
	 * Algoritmo alphabeta per valutare il Game Tree
	 * 
	 * @param T      nodo dell'albero di gioco da cui partire
	 * @param player turno del giocatore corrente
	 * @return la valutazione di una configurazione di gioco secondo l'algoritmo
	 *         Alphabeta
	 * @throws TimeoutException
	 */
	private int AlphaBeta(TreeNode T, CXCellState player, int alpha, int beta) {
		nodeCount++;
		checktime();
		int eval;
		// siamo arrivati alla fine dell'albero: chiamo evaluate
		if (T.isLeaf()) {
			eval = evaluate(T, player);
		}
		// Our player is maximizing
		else if (player == me) {
			eval = -Integer.MAX_VALUE; // eval = -oo
			// generate move list
			T.GenerateMoveList(Board);
			for (int i : T.getMoves()) { // foreach move in T.list
				// make this move in the list
				Board.markColumn(i);
				// add this move to the game tree
				TreeNode child = new TreeNode(Board.getLastMove());
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// evaluate this move by taking the max value between current eval and value
				// calculated by AlphaBeta
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta));
				alpha = Math.max(eval, alpha);
				Board.unmarkColumn();
				// lastNode = child;
				if (beta <= alpha)
					break;
			}
		}
		// Opponent minimizing
		else {
			eval = Integer.MAX_VALUE; // eval = +oo
			T.GenerateMoveList(Board);
			for (int i : T.getMoves()) {
				// make this move in the list
				Board.markColumn(i);
				// add this move to the game tree
				TreeNode child = new TreeNode(Board.getLastMove());
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// evaluate this move by taking the max value between current eval and value
				// calculated by AlphaBeta
				eval = Math.min(eval, AlphaBeta(child, me, alpha, beta));
				beta = Math.min(eval, beta);
				Board.unmarkColumn();
				// lastNode = child;
				if (beta <= alpha)
					break;
			}
		}
		T.label = eval;
		return eval;
	}

	/**
	 * Valuta le configurazioni finali del Game Tree
	 * Il nostro giocatore è quello che massimizza (vittoria = (M*N + 1) -
	 * #turni_giocati / 2)
	 * Se vince l'avversario la mossa è valutata come sopra ma con segno negativo
	 * 
	 * @param T      leaf node
	 * @param player current player
	 * @return evaluation of the leaf node
	 */
	private int evaluate(TreeNode T, CXCellState player) {
		// configurazione di gioco finale: uso un punteggio che va in base ai turni
		// giocati per vincere/perdere
		if (Board.gameState() != CXGameState.OPEN) {
			if (Board.gameState() == myWin) {
				return WIN - Board.numOfMarkedCells();
			} else if (Board.gameState() == yourWin) {
				return LOSS + Board.numOfMarkedCells();
			} else {
				return 0;
			}
		} else {
			System.err.println("È stata passata una configurazione non finale");
			return 0;
			// usiamo l'euristica per valutare la mossa in base alla helpfulness
			// return evaluateColumn(T.getCell(), me) + evaluateColumn(T.getCell(),
			// opponent);
		}
	}

	private int evaluateColumn(CXCell cell, CXCellState player) {
		int helpfulness = 0;
		boolean diagonals = true;
		// placing the piece where we are contemplating is done by alphabeta
		// mi ci vuole un modo per calcolare velocemente il numero di gettoni in orizz.
		// e vert.
		if (M >= X) {
			helpfulness += count(cell, player, Grid.Vertical);
		} else
			diagonals = false;
		if (N >= X) {
			helpfulness += count(cell, player, Grid.Horizontal);
		} else
			diagonals = false;
		if (diagonals)
			helpfulness += count(cell, player, Grid.PosDiagonal) + count(cell, player, Grid.NegDiagonal);
		return helpfulness;
	}

	private int count(CXCell cell, CXCellState player, Grid area) {
		tmpBoard = Board.getBoard();
		if (area == Grid.Vertical) {
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
		if (area == Grid.Horizontal) {
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
		if (area == Grid.PosDiagonal) {
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

	private void saveMove(TreeNode move) {
		System.err.println("nodi visitati: " + nodeCount);
		System.err.println("Move: " + move.getCell().j);
		System.err.println("moveLabel: " + move.label);

		for (TreeNode i : move.getChildNodes())
			System.err.println("Valore dei figli di move: " + i.label);

		root = bestMove;
	}

	/**
	 * Throws a <code> RuntimeException </code> if we are at 99 percent of the
	 * maximum timeout time
	 */
	private void checktime() throws RuntimeException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new RuntimeException("TIMEOUT");
	}

	public String playerName() {
		return "BirbaBot";
	}
}