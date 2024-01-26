package connectx.ZobristBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/**
 * Our Player
 */
public class ZobristBot implements CXPlayer {
	/**  TODO: aggiungere punteggio euristica move ordering a {@link #EvaluateConfiguration()} */

	// constants for the Euristics
	public static final int WIN = 100000;
	public final int LOSS = 0;
	private final int BLOCK_OPP = WIN - 1;
	// variables for initPlayer()
	private int TIMEOUT;
	private long START;
	private int M, N, X; // M righe, N colonne, X allineamenti
	// our copies of the game board
	private CXBoard Board;
	private CXCellState[][] stateBoard;
	// variables for checking gamestate and cells state
	private CXCellState me, opponent;
	private CXGameState myWin, yourWin;
	private int meInt;
	// Transposition table variables
	ZobristTable zobristTable;
	TranspositionTable transpositionTable;
	long currentHash;

	/**
	 * the best move found and the end of iterative deepening
	 */
	private TreeNode bestMove;
	/**
	 * root of the game tree, it's always on opponent's node
	 */
	private TreeNode root;
	// Utility variables
	private Evaluate util;
	private int nodeCount;
	private int old_nodeCount;
	private int turns;
	private float media;

	/* Default empty constructor */
	public ZobristBot() {
	}

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;

		Board = new CXBoard(M, N, X);
		stateBoard = new CXCellState[M][N];

		TIMEOUT = timeout_in_secs;

		myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		me = first ? CXCellState.P1 : CXCellState.P2;
		opponent = first ? CXCellState.P2 : CXCellState.P1;
		meInt = first ? 0 : 1;

		zobristTable = new ZobristTable(M, N);
		transpositionTable = new TranspositionTable();
		currentHash = 0;

		util = new Evaluate(M, N, X);
		bestMove = null;
		nodeCount = 0;
		old_nodeCount = 0;
		turns = 0;
		media = 0;
		root = null;
	}

	/* Selects the best move possible */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time
		nodeCount = 0;
		turns++;
		// look at last opponent move
		CXCell lastOppMove = B.getLastMove();
		// update our Board and stateBoard
		Board = B.copy();
		stateBoard = B.getBoard();
		currentHash = zobristTable.hash(stateBoard);

		try {
			// not the first turn
			if (bestMove != null && lastOppMove != null) {
				// prendo il sottoalbero radicato nell'ultima mossa dell'avversario
				TreeNode chilNode = bestMove.getChildByCell(lastOppMove);
				if (chilNode != null) {
					root = chilNode;
				} else {
					root = new TreeNode(lastOppMove);
					GenerateMoveList(root);
				}
			}
			// first turn
			else {
				// we are player1
				if (lastOppMove == null) {
					bestMove = new TreeNode(makeMove(N / 2));
					return bestMove.getCell().j;
				}
				// we are player2
				else {
					root = new TreeNode(lastOppMove);
					GenerateMoveList(root);
				}
			}

			bestMove = null;
			// start iterative deepening
			IterativeDeepening(root, M * N - B.numOfMarkedCells());

			// compute_average();
			// Debug.printChildren(root);
			return bestMove.getCell().j;

		} catch (TimeoutException e) {
			if (bestMove == null) {
				// compute_average();
				bestMove = new TreeNode(root.getMoves()[0].getCell());
				return bestMove.getCell().j;
			} else {
				// compute_average();
				// Debug.printChildren(root);
				return bestMove.getCell().j;
			}
		}
	}

	/**
	 * Should only be called by {@link #selectColumn(CXBoard)}, this function
	 * searches the game tree in a DFS manner using AlphaBeta algorithm at
	 * increasing depths and
	 * updates <code>bestMove</code> only when all nodes of the same depth are
	 * visited
	 * 
	 * @param T      root node of the game tree
	 * @param player player who's allowed to move
	 * @param depth  depth of search
	 */
	private void IterativeDeepening(TreeNode T, int depth) throws TimeoutException {
		// we start from depth 1, just below the root
		for (int d = 1; d <= depth; d++) {
			checktime();
			nodeCount++;

			int alpha = -Integer.MAX_VALUE; // alpha = -oo
			int beta = Integer.MAX_VALUE; // beta = +oo

			// generate or get already generated move list
			LabeledMove[] moves = T.getMoves();
			TreeNode[] children = T.getChildren();

			// we lost
			if (moves.length == 0) {
				Debug.printTable(stateBoard);
				System.err.println("We lost");
				bestMove = new TreeNode(makeMove(Board.getAvailableColumns()[0]));
				break;
			}

			if (moves.length == 1) {
				// play the only move left
				bestMove = new TreeNode(makeMove(moves[0].getMove()));
				break;
			}

			int eval = alpha, index = 0;
			int bestMoveValue = alpha;
			TreeNode bestMove_yet = null; // migliore mossa trovata con alphabeta fin'ora
			for (LabeledMove i : moves) { // foreach move in T.Moves
				checktime();
				CXCell cell = makeMove(i.getMove());
				// gets already visited node or adds new child and generate its moves
				TreeNode child = getOrCreateChild(T, children, cell, index);

				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta, d - 1));
				alpha = Math.max(eval, alpha);

				undoMove();
				index++;

				if (eval > bestMoveValue) {
					bestMove_yet = child;
					bestMoveValue = eval;
					T.label = bestMoveValue;
				}

				if (beta <= alpha) {
					break;
				}
			}
			// aggiorno bestMove solo dopo una completa ricerca a profondità d
			bestMove = bestMove_yet;
			// System.err.println("depth: " + d);
		}
	}

	/**
	 * AlphaBeta algorithm. This function is invoked by
	 * {@link #IterativeDeepening(TreeNode, CXCellState, int)} and its
	 * implementation provides
	 * euristic evaluation of non-final game configurations and control over the
	 * search's <code>depth</code>.
	 * 
	 * @param T      current game tree node
	 * @param player player who's allowed to move
	 * @param alpha
	 * @param beta
	 * @param depth  remaining depth of the search
	 * @return the evaluated label of <code>T</code>
	 */
	private int AlphaBeta(TreeNode T, CXCellState player, int alpha, int beta, int depth) throws TimeoutException {
		nodeCount++;

		// generate or get already generated move list
		LabeledMove[] moves = T.getMoves();
		TreeNode[] children = T.getChildren();
		TranspositionEntry entry = transpositionTable.search(currentHash);

		// Check transposition table
		if (entry != null && entry.getDepth() >= depth) {
			// Use the stored bounds to potentially prune the search
			if (entry.getType() == EntryType.EXACT) {
				return entry.getEval();
			} else if (entry.getType() == EntryType.LOWERBOUND) {
				alpha = Math.max(alpha, entry.getEval());
			} else if (entry.getType() == EntryType.UPPERBOUND) {
				beta = Math.min(beta, entry.getEval());
			}

			if (beta <= alpha) {
				// Prune the search
				return entry.getEval();
			}
		}

		if (moves.length == 0) {
			if (player == me)
				return -WIN + Board.numOfMarkedCells();
			else
				return WIN - Board.numOfMarkedCells();
		}

		if (moves.length == 1 && moves[0].getValue() == WIN) {
			if (player == me)
				return WIN - Board.numOfMarkedCells();
			else
				return -WIN + Board.numOfMarkedCells();
		}

		// siamo in una configurazione finale oppure la visita in profondità è finita
		if (T.isLeaf() || depth == 0) {
			T.label = evaluate(T);
			// Add entry to transposition table
			// transpositionTable.insert(currentHash, new TranspositionEntry(T.label, depth, EntryType.EXACT));
			return T.label;
		}

		int eval, index = 0;
		// Our player is maximizing
		if (player == me) {
			eval = -Integer.MAX_VALUE; // eval = -oo

			for (LabeledMove i : moves) { // foreach move in T.Moves
				checktime();
				CXCell cell = makeMove(i.getMove());
				// gets already visited node or adds new child and generate its moves
				TreeNode child = getOrCreateChild(T, children, cell, index);

				// evaluate this move by taking the max value between current eval and value
				// calculated by AlphaBeta
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta, depth - 1));
				alpha = Math.max(eval, alpha);

				undoMove();
				index++;

				if (beta <= alpha) {
					break;
				}
			}
		}
		// Opponent minimizing
		else {
			eval = Integer.MAX_VALUE; // eval = +oo

			for (LabeledMove i : moves) { // foreach move in T.Moves
				checktime();
				CXCell cell = makeMove(i.getMove());
				// gets already visited node or adds new child and generate its moves
				TreeNode child = getOrCreateChild(T, children, cell, index);

				// evaluate this move by taking the min value between current eval and value
				// calculated by AlphaBeta
				eval = Math.min(eval, AlphaBeta(child, me, alpha, beta, depth - 1));
				beta = Math.min(eval, beta);

				undoMove();
				index++;

				if (beta <= alpha) {
					break;
				}
			}
		}
		// After evaluating children and determining the best move
		TranspositionEntry newEntry;
		// [10; 25] eval = 5
		if (eval <= alpha) {
			newEntry = new TranspositionEntry(eval, depth, EntryType.UPPERBOUND);
		} else if (eval >= beta) {
			newEntry = new TranspositionEntry(eval, depth, EntryType.LOWERBOUND);
		} else {
			// alpha <= eval <= beta
			newEntry = new TranspositionEntry(eval, depth, EntryType.EXACT);
		}

		// Add entry to transposition table
		transpositionTable.insert(currentHash, newEntry);

		T.label = eval;
		return eval;
	}

	TreeNode getOrCreateChild(TreeNode T, TreeNode[] children, CXCell cell, int index) throws TimeoutException {
		TreeNode child;
		if (children.length > 0) {
			child = children[index];

			if (child == null) {
				child = new TreeNode(cell);
				if (Board.gameState() != CXGameState.OPEN) {
					child.updateLeaf();
					T.addChild(child);
				} else {
					GenerateMoveList(child);
					T.addChild(child);
				}
			}
		} else {
			child = new TreeNode(cell);

			if (Board.gameState() != CXGameState.OPEN) {
				child.updateLeaf();
				T.addChild(child);
			} else {
				GenerateMoveList(child);
				T.addChild(child);
			}
		}
		return child;
	}

	/**
	 * Genera le mosse possibili del nodo prima di entrare nel ciclo for di
	 * {@link #AlphaBeta(TreeNode, CXCellState, int, int, int)}, ordinandole in
	 * ordine decrescente di valore secondo l'euristica.
	 * 
	 * @param T nodo il cui campo <code> T.Moves </code> deve essere
	 *          inizializzato
	 */
	private void GenerateMoveList(TreeNode T) throws TimeoutException {
		Integer[] AM = Board.getAvailableColumns();
		LabeledMove[] moves;
		if (AM.length > 0) {
			moves = possibleNonLosingMoves(AM, Board.currentPlayer() == meInt ? me : opponent);
			if (moves.length <= 1) {
				T.updateMoves(moves);
				return;
			}
			// classico swap
			if (moves.length == 2) {
				if (moves[0].getValue() < moves[1].getValue()) {
					LabeledMove tmp = moves[0];
					moves[0] = moves[1];
					moves[1] = tmp;
					T.updateMoves(moves);
					return;
				}
			}
			// mosse in ordine decrescente
			// Arrays.sort(moves, LabeledMove::compareTo); // 445500.0 media nodi
			LabeledMove.radixSort(moves, moves.length); // 435870.0 media nodi
			T.updateMoves(moves);
		} else {
			System.err.println("GenerateMoveList has been called after game ended");
			Debug.printTable(stateBoard);
		}
	}

	/**
	 * Called by {@link #GenerateMoveList(TreeNode, CXCellState)} to assign the
	 * euristics to
	 * every move possible, following this criteria:
	 * 1) If the player can win in one move return an array containing only that
	 * move,
	 * valued <code>WIN</code>
	 * 2) if the player places a stone under an opponent winning position, our move
	 * is valued <code>LOSS</code>
	 * 3) if the player can block an opponent move (and we cannot win), our move is
	 * valued <code>BLOCK_OPP</code>
	 * 
	 * 
	 * @param AM     array of available moves
	 * @param player whose turn is
	 * @return array of Labeled Move(s) that are worth exploring with alphabeta
	 */
	private LabeledMove[] possibleNonLosingMoves(Integer[] AM, CXCellState player) throws TimeoutException {

		CXCell block = null;
		int blockMoves = 0;
		// LabeledMove[] return_moves = new LabeledMove[AM.length];
		// int index = 0;
		LinkedList<LabeledMove> worth_moves = new LinkedList<>();

		for (Integer col : AM) {
			boolean add_move = true;
			// I need the bottom cell of the column col
			CXCell move = makeMove(col);

			checktime();
			// It is better to win than to simply block the opponent
			int myEval = util.evaluateColumn(stateBoard, move, player == me ? me : opponent);
			if (myEval == WIN) {
				undoMove();
				return new LabeledMove[] { new LabeledMove(WIN, move) };
			}
			checktime();
			// We should always play a column on which the opponent has a winning position
			// in the bottom of the column.
			int oppEval = util.evaluateColumn(stateBoard, move, player == me ? opponent : me);
			if (oppEval == WIN) {
				add_move = false;
				block = move;
				blockMoves++;
				worth_moves.add(new LabeledMove(BLOCK_OPP, move));
				// return_moves[index] = new LabeledMove(BLOCK_OPP, move);
			}

			// We should never play under opponent winning positions.
			if (!Board.fullColumn(col)) {
				// play a stone on top of the current one, it can overwrite BLOCK_OPP case
				makeMove(col);
				if (Board.gameState() == (player == me ? yourWin : myWin)) {
					add_move = false;
					// return_moves[index] = new LabeledMove(LOSS, move);
				}
				undoMove();
			}

			// after all the checks, just add move to the list
			if (add_move) {
				// helpfulness is sum of both POVs
				worth_moves.add(new LabeledMove(myEval + oppEval, move));
				// return_moves[index] = new LabeledMove(myEval + oppEval, move);
			}

			undoMove();
		}

		if (blockMoves > 1 || worth_moves.size() == 0) {
			return new LabeledMove[0]; // { new LabeledMove(LOSS, block) };
		}

		if (block != null) {
			return new LabeledMove[] { new LabeledMove(BLOCK_OPP, block) };
		}

		LabeledMove[] return_moves = new LabeledMove[worth_moves.size()];
		worth_moves.toArray(return_moves);
		return return_moves;
	}

	/**
	 * Called only by {@link #AlphaBeta(TreeNode, CXCellState, int, int, int)}, this
	 * function provides the evaluation of any
	 * <code>TreeNode</code>: be it a leaf
	 * or a non-final move
	 * 
	 * @param T      game tree node
	 * @param player current player
	 * @return evaluation of the node
	 */
	private int evaluate(TreeNode T) throws TimeoutException {
		// final configuration
		if (T.isLeaf()) {
			if (Board.gameState() == myWin) {
				return WIN - Board.numOfMarkedCells();
			} else if (Board.gameState() == yourWin) {
				return -WIN + Board.numOfMarkedCells();
			} else {
				return 0;
			}
			// non-final configuration
		} else {
			return EvaluateConfiguration();
		}
	}

	/**
	 * Invoked by {@link #evaluate(TreeNode, CXCellState)}, this function computes
	 * the euristics on any
	 * non-final configuration by
	 * taking the sum of the helpfulness values from our perspective for every
	 * possible move and subtracting the sum calculated from the opponents POV
	 * 
	 * @param T
	 * @param player
	 * @return
	 */
	private int EvaluateConfiguration() throws TimeoutException {
		int eval = 0;
		for (Integer i : Board.getAvailableColumns()) {
			checktime();
			CXCell freeCell = makeMove(i);
			undoMove();
			eval += util.simpleEvaluateColumn(stateBoard, freeCell, me);
			eval -= util.simpleEvaluateColumn(stateBoard, freeCell, opponent);
		}
		return eval;
	}

	/**
	 * Makes the move on our <code>Board</code> and <code>stateBoard</code>
	 * using the markcolumn() function provided by CXBoard
	 * 
	 * @param col
	 * @param funzione
	 * @return the <code>CXCell</code> type cell of the move
	 */
	private CXCell makeMove(int col) {
		Board.markColumn(col);
		CXCell move = Board.getLastMove();
		currentHash = zobristTable.updateHash(currentHash, stateBoard, move, false);
		stateBoard[move.i][move.j] = move.state;
		return move;
	}

	private void undoMove() {
		CXCell move = Board.getLastMove();
		currentHash = zobristTable.updateHash(currentHash, stateBoard, move, true);
		stateBoard[move.i][move.j] = CXCellState.FREE;
		Board.unmarkColumn();
	}

	private void compute_average() {
		old_nodeCount += nodeCount;
		media = old_nodeCount / turns;
		System.err.println("media: " + media);
	}

	/**
	 * Throws a <code> RuntimeException </code> if we are at 99 percent of the
	 * maximum timeout time
	 */
	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (98.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "ZobristBot";
	}
}