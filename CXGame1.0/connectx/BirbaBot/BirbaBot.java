package connectx.BirbaBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;

import java.util.LinkedList;
import java.util.List;
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
 * TODO: check isWinningPosition: might be that stateBoard is not updated just by calling Board.markcolumn(col) 
 * [x] Evaluate con Euristica basta sulla helpfulness
 * [.] Transposition Table con le mosse già computate nei turni precedenti in modo da non visitare 
 *     ogni volta un nuovo Game Tree
 * [x] AlphaBeta + Iterative Deepening
 * [x] Move ordering
 */

/**
 * Our Player
 */
public class BirbaBot implements CXPlayer {
	// constants for the Euristics
	private final int WIN = 100000;
	private final int LOSS = -100000, BLOCK_OPP = WIN - 1;
	// variables of initPlayer()
	private int TIMEOUT;
	private long START;
	private boolean first;
	private int M, N, X; // M righe, N colonne, X allineamenti
	// our copies of the game board
	private CXBoard Board;
	private CXCellState[][] stateBoard;
	// variables for checking gamestate and cells state
	private int meint, oppint;
	private CXCellState me, opponent;
	private Set<CXCellState> AC; // Available Columns
	private CXGameState myWin, yourWin;
	/**
	 * game tree nodes: bestmove and root of the game tree, root is initialized to
	 * bestMove at the end of every single one of our turns
	 */
	private TreeNode bestMove, root;

	private Evaluate util;
	private int nodeCount;
	private boolean meFirst_secondturn = false;

	/* Default empty constructor */
	public BirbaBot() {
	}

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;

		stateBoard = new CXCellState[M][N];

		TIMEOUT = timeout_in_secs;
		this.first = first;
		meint = first ? 0 : 1;
		oppint = first ? 0 : 1;
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
		// look at last opponent move
		CXCell lastMove = null;
		lastMove = B.getLastMove();
		// update our Board and stateBoard
		if (lastMove != null) {
			Board.markColumn(lastMove.j);
			stateBoard[lastMove.i][lastMove.j] = opponent;
		}
		// moves to be inserted in T.Moves
		// LabeledMove[] moves;
		// not the first turn
		if (bestMove != null && lastMove != null) {
			TreeNode myMove = bestMove.getChildByCell(lastMove);
			// root è la mia mossa già presente nel game tree
			if (myMove != null) {
				root = myMove;
				//moves = root.getMoves();
			}
			// altrimenti prendo il sottoalbero radicato nell'ultima mossa dell'avversario
			else {
				root = new TreeNode(lastMove);
				GenerateMoveList(root);
			}
		}
		// first turn
		else {
			if (lastMove == null) {
				Board = B.copy();
				stateBoard = B.getBoard();
				Board.markColumn(N/2);
				root = new TreeNode(Board.getLastMove());
				GenerateMoveList(root);
			}
			else {
				Board = B.copy();
				stateBoard = B.getBoard();
				root = new TreeNode(Board.getLastMove());
				GenerateMoveList(root);
			}
		}
		// start iterative deepening
		try {
			IterativeDeepening(root, Board.currentPlayer() == meint ? me : opponent, 6);
		} catch (Exception e) {
			System.err.println("time ran out: return best move found yet");
			saveMove(bestMove);
			return bestMove.getCell().j;
		}

		saveMove(bestMove);
		return bestMove.getCell().j;
	}

	/**
	 * Should only be called by {@link #selectColumn(CXBoard)}, this function
	 * searches the game tree in a BFS manner using AlphaBeta algorithm
	 * Updates <code>bestMove</code> only when all nodes of same depth are visited
	 * @param T	root node of the game tree
	 * @param player player who's allowed to move
	 * @param depth	depth of search
	 */
	private void IterativeDeepening(TreeNode T, CXCellState player, int depth) {
		int alpha = -Integer.MAX_VALUE; // alpha = -oo
		int beta = Integer.MAX_VALUE; // beta = +oo
		int eval = 0, bestMoveValue = alpha;
		for (int d = 0; d <= depth; d++) {
			checktime();
			// fai partire alpahabeta
			eval = AlphaBeta(T, player, alpha, beta, depth);
			if (eval > bestMoveValue) {
				bestMoveValue = eval;
				bestMove = T;
			}
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
	private int AlphaBeta(TreeNode T, CXCellState player, int alpha, int beta, int depth) {
		nodeCount++;
		checktime();
		int eval;
		// siamo in una foglia oppure la visita in profondità è finita
		if (T.isLeaf() || depth == 0) {
			eval = evaluate(T);
		}
		// Our player is maximizing
		else if (player == me) {
			eval = -Integer.MAX_VALUE; // eval = -oo
			// generate or get already generated move list
			LabeledMove[] children = T.getMoves();
			if (children == null)
				GenerateMoveList(T);
			for (LabeledMove i : children) { // foreach move in T.Moves
				// make this move in the list
				Board.markColumn(i.getMove());
				// add this move to the game tree
				TreeNode child = new TreeNode(Board.getLastMove());
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// evaluate this move by taking the max value between current eval and value
				// calculated by AlphaBeta
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta, depth - 1));
				alpha = Math.max(eval, alpha);
				Board.unmarkColumn();
				if (beta <= alpha)
					break;
			}
		}
		// Opponent minimizing
		else {
			eval = Integer.MAX_VALUE; // eval = +oo
			// generate or get already generated move list
			LabeledMove[] children = T.getMoves();
			if (children == null)
				GenerateMoveList(T);
			for (LabeledMove i : children) { // foreach move in T.Moves
				// make this move in the list
				Board.markColumn(i.getMove());
				// add this move to the game tree
				TreeNode child = new TreeNode(Board.getLastMove());
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// evaluate this move by taking the max value between current eval and value
				// calculated by AlphaBeta
				eval = Math.min(eval, AlphaBeta(child, me, alpha, beta, depth - 1));
				beta = Math.min(eval, beta);
				Board.unmarkColumn();
				if (beta <= alpha)
					break;
			}
		}
		T.label = eval;
		return eval;
	}

	/**
	 * Inizializza le mosse possibili del nodo prima di entrare nel ciclo for di
	 * {@link #AlphaBeta(TreeNode, CXCellState, int, int, int)}, ordinandole in
	 * ordine decrescente di
	 * valore secondo l'euristica.
	 * Se esiste una mossa che ci fa vincere immediatamente, l'unica mossa possibile
	 * sarà quella.
	 * Se esistono almeno due mosse immediatamente vincenti per l'avversario,
	 * l'array sarà vuoto: l'eval del nodo sarà -oo se è il nostro turno, +oo se gioca l'avversario
	 * 
	 * @param T      nodo il cui campo <code> T.Moves </code> deve essere
	 *               inizializzato
	 * @param player il giocatore corrente
	 */
	private void GenerateMoveList(TreeNode T) {
		Integer[] AM = Board.getAvailableColumns();
		LabeledMove[] moves;
		if (AM.length > 0) {
			moves = possibleNonLosingMoves(AM);
			if (moves.length <= 1) {
				T.updateMoves(moves);
				return;
			}
			if (moves.length == 2) {
				if (moves[0].getLabel() < moves[1].getLabel()) {
					LabeledMove tmp = moves[0];
					moves[0] = moves[1];
					moves[1] = tmp;
					T.updateMoves(moves);
					return;
				}
			}
			LabeledMove.radixSort(moves, moves.length);
			T.updateMoves(moves);
		} else
			System.err.println("GenerateMoveList has been called after game ended");
	}

	/**
	 * Must only be called when generating Moves in
	 * {@link #GenerateMoveList(TreeNode, CXCellState)} to
	 * explore with alphabeta.
	 * REMEMBER: It is better to win than to simply block the opponent: how do I
	 * translate it to values? For now: win = 100000, opponent win = 99999
	 * 
	 * @param AM     array of available moves
	 * @return array of Labeled Move(s) that are worth exploring with alphabeta
	 */
	private LabeledMove[] possibleNonLosingMoves(Integer[] AM) {
		int oppWinningMoves = 0;
		List<LabeledMove> WorthMoves = new LinkedList<>();
		for (Integer col : AM) {
			Board.markColumn(col);
			CXCell move = Board.getLastMove();
			util = new Evaluate(M, N, X, Board, stateBoard);
			// We should always play a column on which the opponent has a winning position
			// in the bottom of the column.
			if (util.isWinningPosition(move, stateBoard)) {
				if (move.state == me) {
					// WorthMoves.add(new LabeledMove(WIN, move));
					LabeledMove[] winningCell = new LabeledMove[1];
					winningCell[0] = new LabeledMove(WIN, move);
					return winningCell;
				} else {
					WorthMoves.add(new LabeledMove(BLOCK_OPP, move));
					oppWinningMoves++;
				}
			}
			if (!Board.fullColumn(move.j)) {
				Board.markColumn(move.j);
				CXCell upperCell = Board.getLastMove();
				// We should never play under an opponent winning positions.
				// if I win by placing a stone at upperCell's position, then the stone under it
				// is worth evaluating
				if (upperCell.state == me) {
					if (util.isWinningPosition(upperCell, stateBoard)) {
						WorthMoves.add(new LabeledMove(util.get_helpfulness(move), move));
					}
				}
				Board.unmarkColumn();
			}
			// If the opponent has more than two directly playable winning positions, then
			// we cannot do anything and we will lose.
			if (oppWinningMoves >= 2) {
				LabeledMove[] empty = new LabeledMove[0];
				return empty;
			}
			// after all the checks, just add move to the list
			else {
				WorthMoves.add(new LabeledMove(util.get_helpfulness(move), move));
			}
			Board.unmarkColumn();
		}
		LabeledMove[] return_moves = new LabeledMove[WorthMoves.size()];
		WorthMoves.toArray(return_moves);
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
	private int evaluate(TreeNode T) {
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
			return EvaluateConfiguration(T);
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
	private int EvaluateConfiguration(TreeNode T) {
		int eval = 0;
		util = new Evaluate(M, N, X, Board, stateBoard);
		for (int i = 0; i < Board.getAvailableColumns().length; i++) {
			eval += util.evaluateColumn(T.getCell(), me);
			eval -= util.evaluateColumn(T.getCell(), opponent);
		}
		return eval;
	}

	private void saveMove(TreeNode move) {
		System.err.println("nodi visitati: " + nodeCount);
		System.err.println("Move: " + move.getCell().j);
		System.err.println("moveLabel: " + move.label);

		for (TreeNode i : move.getChildNodes())
			System.err.println("Valore dei figli di move: " + i.label);

		root = bestMove;
	}


	public static void breakpoint() {
		System.err.println("breakpoint");
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