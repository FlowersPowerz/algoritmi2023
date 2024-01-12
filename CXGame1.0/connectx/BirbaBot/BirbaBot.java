package connectx.BirbaBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import java.util.concurrent.TimeoutException;

/**
 * Our Player
 */
public class BirbaBot implements CXPlayer {
	/**
	 * TODO: possibilità di revisitare l'albero e aggiungere nuovi nodi
	 * [.] Transposition Table con le mosse già computate nei turni precedenti in
	 * modo da non visitare ogni volta un nuovo Game Tree
	 */

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
	private int meint;
	private CXCellState me, opponent;
	private CXGameState myWin, yourWin;

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

	/* Default empty constructor */
	public BirbaBot() {
	}

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;

		Board = new CXBoard(M, N, X);
		stateBoard = new CXCellState[M][N];

		TIMEOUT = timeout_in_secs;

		meint = first ? 0 : 1;
		myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		me = first ? CXCellState.P1 : CXCellState.P2;
		opponent = first ? CXCellState.P2 : CXCellState.P1;

		util = new Evaluate(this.M, this.N, this.X);
		bestMove = null;
		nodeCount = 0;
		root = null;
	}

	/* Selects the best move possible */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time
		nodeCount = 0;
		// look at last opponent move
		CXCell lastOppMove = B.getLastMove();
		// update our Board and stateBoard
		Board = B.copy();
		stateBoard = B.getBoard();
		// System.err.println("====================================");
		// not the first turn
		if (bestMove != null && lastOppMove != null) {
			// prendo il sottoalbero radicato nell'ultima mossa dell'avversario
			// TreeNode chilNode = bestMove.getChildByCell(lastOppMove);
			// if (chilNode != null) {
			// 	root = chilNode;
			// }
			root = new TreeNode(lastOppMove);
			GenerateMoveList(root);
		}
		// first turn
		else {
			// first round
			if (lastOppMove == null) {
				bestMove = new TreeNode(makeMove(N / 2, 0));
				return bestMove.getCell().j;
			}
			// second round
			else {
				root = new TreeNode(lastOppMove);
				GenerateMoveList(root);
			}
		}
		// start iterative deepening
		try {
			IterativeDeepening(root, me, M * N - B.numOfMarkedCells(), B);
		} catch (TimeoutException e) {
			if (bestMove == null) {
				System.err.println("Visited nodes: " + nodeCount);
				return root.getMoves()[0].getCell().j;
			} else {
				System.err.println("Visited nodes: " + nodeCount);
				return bestMove.getCell().j;
			}
		}

		// Debug.printTable(stateBoard);
		// System.err.println("best move: " + bestMove.getCell().j + ", label: " +
		// bestMove.label);
		System.err.println("Visited nodes: " + nodeCount);
		return bestMove.getCell().j;
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
	private void IterativeDeepening(TreeNode T, CXCellState player, int depth, CXBoard B) throws TimeoutException {
		// differenziare OldBestMove da NewBestMove, la prima deve essere il
		// risultato di una ricerca completa a profondità d, la seconda è quella
		// calcolata fino a quel momento

		// nella mia testa depth = 0 è la radice quindi se voglio fare una visita solo
		// al
		// primo livello del sottoalbero radicato in T la depth = 1
		for (int d = 1; d <= depth; d++) {
			checktime();
			nodeCount++;

			int alpha = -Integer.MAX_VALUE; // alpha = -oo
			int beta = Integer.MAX_VALUE; // beta = +oo
			int bestMoveValue = alpha;

			// generate or get already generated move list
			LabeledMove[] children = T.getMoves();
			// // should be a useless check since we generate them in select cell
			// if (children.length == 0) {
			// GenerateMoveList(T);
			// children = T.getMoves();
			// }

			if (children.length == 1 && children[0].getValue() == WIN) {
				// play the winnig move and evaluate it immediately
				TreeNode child = new TreeNode(makeMove(children[0].getMove(), 1));
				// child is a leaf node
				child.updateLeaf();
				child.label = evaluate(child);
				T.addChild(child);

				undoMove();

				bestMove = child;
				bestMoveValue = child.label;
				T.label = bestMoveValue;
				break;
			}

			int eval = alpha;
			TreeNode bestMove_yet = null; // migliore mossa trovata con alphabeta fin'ora
			for (LabeledMove i : children) { // foreach move in T.Moves
				checktime();
				// make this move in the list and add it to the game tree
				CXCell move = makeMove(i.getMove(), 3);
				TreeNode child = new TreeNode(move);
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// chiamo Alphabeta con depth decrementata (se ho d=1 Alphabeta(depth = 0) farà
				// subito un evaluate dei figli di root)
				// che aggiorna la bestMove se l'eval trovato è migliore di prima
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta, d - 1));
				alpha = Math.max(eval, alpha);

				undoMove();

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

		// siamo in una configurazione finale oppure la visita in profondità è finita
		if (T.isLeaf() || depth == 0) {
			T.label = evaluate(T);
			return T.label;
		}
		// generate or get already generated move list
		LabeledMove[] children = T.getMoves();
		// if this node has not been discovered yet generate its moves
		if (children.length == 0) {
			GenerateMoveList(T);
			children = T.getMoves();
		}

		if (children.length == 1 && children[0].getValue() == WIN) {
			// play the winning move and evaluate it immediately
			TreeNode child = new TreeNode(makeMove(children[0].getMove(), 1));
			// child is a leaf node
			child.updateLeaf();
			child.label = evaluate(child);
			T.addChild(child);

			undoMove();

			T.label = child.label;
			return T.label;
		}

		int eval;
		// Our player is maximizing
		if (player == me) {
			eval = -Integer.MAX_VALUE; // eval = -oo

			for (LabeledMove i : children) { // foreach move in T.Moves
				checktime();
				// make this move in the list and add it to the game tree
				TreeNode child = new TreeNode(makeMove(i.getMove(), 1));
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);

				// evaluate this move by taking the max value between current eval and value
				// calculated by AlphaBeta
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta, depth - 1));
				alpha = Math.max(eval, alpha);

				undoMove();

				if (beta <= alpha) {
					break;
				}
			}
		}
		// Opponent minimizing
		else {
			eval = Integer.MAX_VALUE; // eval = +oo

			for (LabeledMove i : children) { // foreach move in T.Moves
				checktime();
				// make this move in the list and add it to the game tree
				TreeNode child = new TreeNode(makeMove(i.getMove(), 1));
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);

				// evaluate this move by taking the min value between current eval and value
				// calculated by AlphaBeta
				eval = Math.min(eval, AlphaBeta(child, me, alpha, beta, depth - 1));
				beta = Math.min(eval, beta);

				undoMove();

				if (beta <= alpha) {
					break;
				}
			}
		}
		T.label = eval;
		return eval;
	}

	/**
	 * Genera le mosse possibili del nodo prima di entrare nel ciclo for di
	 * {@link #AlphaBeta(TreeNode, CXCellState, int, int, int)}, ordinandole in
	 * ordine decrescente di valore secondo l'euristica.
	 * 
	 * @param T nodo il cui campo <code> T.Moves </code> deve essere
	 *          inizializzato
	 */
	private void GenerateMoveList(TreeNode T) {
		Integer[] AM = Board.getAvailableColumns();
		LabeledMove[] moves;
		if (AM.length > 0) {
			moves = possibleNonLosingMoves(AM, T.getCurrentPlayer());
			if (moves.length == 1) {
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
			LabeledMove.radixSort(moves, moves.length);
			T.updateMoves(moves);
		} else
			System.err.println("GenerateMoveList has been called after game ended");
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
	private LabeledMove[] possibleNonLosingMoves(Integer[] AM, CXCellState player) {

		int index = 0;
		LabeledMove[] return_moves = new LabeledMove[AM.length];

		for (Integer col : AM) {
			boolean add_move = true;
			// I need the bottom cell of the column col
			CXCell move = makeMove(col, 2);

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
				return_moves[index] = new LabeledMove(BLOCK_OPP, move);
			}

			// We should never play under opponent winning positions.
			if (!Board.fullColumn(col)) {
				// play a stone on top of the current one, it can overwrite BLOCK_OPP case
				makeMove(col, 2);
				if (Board.gameState() == (player == me ? yourWin : myWin)) {
					add_move = false;
					return_moves[index] = new LabeledMove(LOSS, move);
				}
				undoMove();
			}

			// after all the checks, just add move to the list
			if (add_move) {
				// helpfulness is sum of both POVs
				return_moves[index] = new LabeledMove(myEval + oppEval, move);
			}

			index++;
			undoMove();
		}

		// If the opponent has more than two directly playable winning positions
		// there is nothing we can do
		// if (oppWinningMoves > 1) {
		// return new LabeledMove[0];
		// }

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
		// configurazione di gioco finale: uso un punteggio che va in base ai turni
		// giocati per vincere/perdere
		if (Board.gameState() != CXGameState.OPEN) {
			if (Board.gameState() == myWin) {
				return WIN - Board.numOfMarkedCells();
			} else if (Board.gameState() == yourWin) {
				return -WIN + Board.numOfMarkedCells();
			} else {
				return 0;
			}
		} else {
			// System.err.println("È stata passata una configurazione non finale");
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
	private int EvaluateConfiguration(TreeNode T) throws TimeoutException {
		int eval = 0;
		for (Integer i : Board.getAvailableColumns()) {
			checktime();
			CXCell freeCell = makeMove(i, 1);
			eval += util.evaluateColumn(stateBoard, freeCell, me);
			eval -= util.evaluateColumn(stateBoard, freeCell, opponent);
			undoMove();
		}
		if (eval > WIN)
			return WIN - Board.numOfMarkedCells() + 1;
		else if (eval < -WIN)
			return -WIN + Board.numOfMarkedCells() + 1;
		else
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
	private CXCell makeMove(int col, int funzione) {
		if (Board.gameState() == CXGameState.OPEN && !Board.fullColumn(col)) {
			Board.markColumn(col);
			CXCell move = Board.getLastMove();
			stateBoard[move.i][move.j] = move.state;
			return move;
		} else {
			switch (funzione) {
				case 0:
					System.err.println("OPS! makeMove chiamato da select_column");
				case 1:
					System.err.println("OPS! makeMove chiamato da Alphabeta");
				case 2:
					System.err.println("OPS! makeMove chiamato da possibleNonLosingMoves");
				case 3:
					System.err.println("OPS! makeMove chiamato da IterativeDeepening");
			}
			if (Board.gameState() != CXGameState.OPEN) {
				System.err.println("hai provato a giocare dopo che la partita è finita");
			}
			if (Board.fullColumn(col)) {
				System.err.println("hai giocato su una colonna già piena: " + col);
			}
			if (col < 0 && col > N) {
				System.err.println("hai cagato fuori dal vaso");
			}
			if (Board.currentPlayer() == meint)
				System.err.println("tocca a noi");
			else
				System.err.println("tocca all'avversario");
			Debug.printTable(stateBoard);
			Board.markColumn(col);
			CXCell move = Board.getLastMove();
			stateBoard[move.i][move.j] = move.state;
			return move;
		}
	}

	private void undoMove() {
		CXCell move = Board.getLastMove();
		stateBoard[move.i][move.j] = CXCellState.FREE;
		Board.unmarkColumn();
	}

	/**
	 * Throws a <code> RuntimeException </code> if we are at 99 percent of the
	 * maximum timeout time
	 */
	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "BirbaBot";
	}
}