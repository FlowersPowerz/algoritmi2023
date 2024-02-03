package connectx.BirbaBot;

import connectx.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

/**
 * Our Player
 */
public class BirbaBot implements CXPlayer {
	// constants for the Euristics
	public static final int WIN = 100000;
	private final int BLOCK_OPP = WIN - 1;
	// variables for initPlayer()
	private int TIMEOUT;
	private long START;
	private int M, N, X; // M rows, N columns, X alignments
	// our copies of the game board
	private CXBoard Board;
	private CXCellState[][] stateBoard;
	// variables for checking gamestate and cells state
	private CXCellState me, opponent;
	private CXGameState myWin, yourWin;
	private int meInt;
	// Utility variables
	private Evaluate util;

	/**
	 * the best move found and the end of iterative deepening
	 */
	private TreeNode bestMove;
	/**
	 * root of the game tree, it's always on opponent's node
	 */
	private TreeNode root;

	/* Default empty constructor */
	public BirbaBot() {
	}

	/** Initialize the player */
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;

		Board = new CXBoard(this.M, this.N, this.X);
		stateBoard = new CXCellState[M][N];

		TIMEOUT = timeout_in_secs;

		myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		me = first ? CXCellState.P1 : CXCellState.P2;
		opponent = first ? CXCellState.P2 : CXCellState.P1;
		meInt = first ? 0 : 1;

		util = new Evaluate(this.M, this.N, this.X);

		bestMove = null;
		root = null;
	}

	/* Selects the best move possible */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time
		// look at last opponent move
		CXCell lastOppMove = B.getLastMove();
		// update our Board and stateBoard
		Board = B.copy();
		stateBoard = B.getBoard();

		try {
			// not the first turn
			if (bestMove != null && lastOppMove != null) {
				// take the subtree rooted in opponent's move
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
			IterativeDeepening(root, B.numOfFreeCells());

			return bestMove.getCell().j;

		} catch (TimeoutException e) {
			// we couldn't complete the visit at shallow depth, should be a useless check
			if (bestMove == null) {
				bestMove = new TreeNode(root.getMoves()[0].getCell());
			}
			return bestMove.getCell().j;
		}
	}

	/**
	 * Should only be called by {@link #selectColumn(CXBoard)}, this function
	 * searches the game tree in a DFS manner using AlphaBeta algorithm at
	 * increasing depths and
	 * updates <code>bestMove</code> only when all nodes of the same depth are
	 * visited
	 * 
	 * @param T     root node of the game tree
	 * @param depth maximum depth of search
	 */
	private void IterativeDeepening(TreeNode T, int depth) throws TimeoutException {
		// we start from depth 1, just below the root
		for (int d = 1; d <= depth; d++) {
			checktime();

			int alpha = -Integer.MAX_VALUE; // alpha = -oo
			int beta = Integer.MAX_VALUE; // beta = +oo

			// generate or get already generated move list
			LabeledMove[] moves = T.getMoves();
			TreeNode[] children = T.getChildren();

			// we lost
			if (moves.length == 0) {
				bestMove = new TreeNode(makeMove(Board.getAvailableColumns()[0]));
				break;
			}

			// play the only column left
			if (moves.length == 1) {
				bestMove = new TreeNode(makeMove(moves[0].getMove()));
				break;
			}

			int eval = alpha, index = 0;
			int bestMoveValue = alpha;
			TreeNode bestMove_yet = null; // migliore mossa trovata con alphabeta fin'ora

			for (LabeledMove i : moves) { // foreach move in T.Moves
				CXCell cell = makeMove(i.getMove());
				// gets already visited node or adds new child and generates its moves
				TreeNode child = getOrCreateChild(T, children, cell, index);

				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta, d - 1));
				alpha = Math.max(eval, alpha);

				undoMove();
				index++;
				// updates bestMove_yet only when we find a truly better move
				if (eval > bestMoveValue) {
					bestMove_yet = child;
					bestMoveValue = eval;
					T.label = bestMoveValue;
				}

				if (beta <= alpha) {
					break;
				}
			}
			// update bestMove only after a complete search at depth d
			bestMove = bestMove_yet;
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
	 * @return the label of <code>T</code>
	 */
	private int AlphaBeta(TreeNode T, CXCellState player, int alpha, int beta, int depth) throws TimeoutException {
		checktime();

		// get moves and children array
		LabeledMove[] moves = T.getMoves();
		TreeNode[] children = T.getChildren();

		// losing configuration
		if (moves.length == 0) {
			if (player == me)
				return -WIN + Board.numOfMarkedCells();
			else
				return WIN - Board.numOfMarkedCells();
		}

		// winning configuration
		if (moves.length == 1 && moves[0].getValue() == WIN) {
			if (player == me)
				return WIN - Board.numOfMarkedCells();
			else
				return -WIN + Board.numOfMarkedCells();
		}

		// we are in a leaf node or a non-final configuration
		if (T.isLeaf() || depth == 0) {
			T.label = evaluate(T);
			return T.label;
		}

		int eval, index = 0;
		// Our player is maximizing
		if (player == me) {
			eval = -Integer.MAX_VALUE; // eval = -oo

			for (LabeledMove i : moves) { // foreach move in T.Moves
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

		T.label = eval;
		return eval;
	}

	/**
	 * Gets already visited node or adds new child and generates its moves
	 * 
	 * @param T        node of which we want to get the child
	 * @param children children array of <code>T</code> node
	 * @param cell     child move
	 * @param index    index to make sure we add the move in the children array
	 *                 safely
	 * @return child of <code>T</code> node
	 * @throws TimeoutException
	 */
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
	 * Generates and updates the non losing moves of <code>T</code> node in
	 * descending order
	 * 
	 * @param T
	 * @throws TimeoutException
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
			// classic swap
			if (moves.length == 2) {
				if (moves[0].getValue() < moves[1].getValue()) {
					LabeledMove tmp = moves[0];
					moves[0] = moves[1];
					moves[1] = tmp;
					T.updateMoves(moves);
					return;
				}
			}
			// moves in descending order; stable algorithm
			Arrays.sort(moves, LabeledMove::compareTo);
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
	 * move, valued <code>WIN</code>
	 * 2) if the player places a stone under an opponent winning position, that move
	 * is ignored
	 * 3) if the player can block an opponent move (and we cannot win), our move is
	 * valued <code>BLOCK_OPP</code>
	 * 4) if the move doesn't fall in any of the other cases, label it with the
	 * helpfulness
	 * 
	 * 
	 * @param AM     array of available moves
	 * @param player whose turn is
	 * @return array of Labeled Move(s) that are worth exploring with alphabeta
	 */
	private LabeledMove[] possibleNonLosingMoves(Integer[] AM, CXCellState player) throws TimeoutException {

		CXCell block = null;
		int blockMoves = 0;
		LinkedList<LabeledMove> worth_moves = new LinkedList<>();

		for (Integer col : AM) {
			checktime();
			boolean add_move = true;
			// I need the bottom cell of the column col
			CXCell move = makeMove(col);

			// It is better to win than to simply block the opponent
			int myEval = util.evaluateColumn(stateBoard, move, player == me ? me : opponent);
			if (myEval == WIN) {
				undoMove();
				return new LabeledMove[] { new LabeledMove(WIN, move) };
			}

			// We should always play a column on which the opponent has a winning position
			// in the bottom of the column.
			int oppEval = util.evaluateColumn(stateBoard, move, player == me ? opponent : me);
			if (oppEval == WIN) {
				add_move = false;
				block = move;
				blockMoves++;
				worth_moves.add(new LabeledMove(BLOCK_OPP, move));
			}

			// We should never play under opponent winning positions.
			if (!Board.fullColumn(col)) {
				// play a stone on top of the current one, it can overwrite BLOCK_OPP case
				makeMove(col);
				if (Board.gameState() == (player == me ? yourWin : myWin)) {
					add_move = false;
				}
				undoMove();
			}

			// after all the checks, just add move to the list
			if (add_move) {
				// helpfulness is sum of both POVs
				worth_moves.add(new LabeledMove(myEval + oppEval, move));
			}
			undoMove();
		}
		// we found two or more opp winning moves, or we can only play on losing
		// positions
		if (blockMoves > 1 || worth_moves.size() == 0) {
			return new LabeledMove[0];
		}
		// we can block the opponent
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
	 * @param T game tree node
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
	 * @return the euristics of the current configuration
	 */
	private int EvaluateConfiguration() throws TimeoutException {
		int eval = 0;
		for (Integer i : Board.getAvailableColumns()) {
			checktime();
			CXCell freeCell = makeMove(i);
			eval += util.simpleEvaluateColumn(stateBoard, freeCell, me);
			eval -= util.simpleEvaluateColumn(stateBoard, freeCell, opponent);
			undoMove();
		}
		return eval;
	}

	/**
	 * Make the move on our <code>Board</code> and <code>stateBoard</code>
	 * using markcolumn() function provided by CXBoard
	 * 
	 * @param col column to play
	 * @return the <code>CXCell</code> type cell of the move
	 */
	private CXCell makeMove(int col) {
		Board.markColumn(col);
		CXCell move = Board.getLastMove();
		stateBoard[move.i][move.j] = move.state;
		return move;
	}

	/**
	 * Undo the last move made on our <code>Board</code> and <code>stateBoard</code>
	 */
	private void undoMove() {
		CXCell move = Board.getLastMove();
		stateBoard[move.i][move.j] = CXCellState.FREE;
		Board.unmarkColumn();
	}

	/**
	 * Throws a <code> TimeoutException </code> if we are at 95 percent of the
	 * maximum timeout time
	 */
	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (95.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "BirbaBot";
	}
}