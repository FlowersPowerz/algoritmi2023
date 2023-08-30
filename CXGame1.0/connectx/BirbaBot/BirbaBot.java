package connectx.BirbaBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import java.util.LinkedList;
import java.util.List;

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
	private int M, N, X; // M righe, N colonne, X allineamenti
	// our copies of the game board
	private CXBoard Board;
	private CXCellState[][] stateBoard;
	// variables for checking gamestate and cells state
	private int meint, oppint;
	private CXCellState me, opponent;
	private CXGameState myWin, yourWin;
	// private Set<CXCellState> AC; // Available Columns
	/**
	 * game tree nodes: bestmove and root of the game tree: root has to be the last
	 * move made by the opponent
	 * if it's the first round, make a move and evaluate all the moves so root is
	 * the best move among them
	 */
	private TreeNode bestMove, root;
	// Utility variables
	private Evaluate util;
	private int nodeCount;
	// private boolean meFirst_secondturn = false;

	/* Default empty constructor */
	public BirbaBot() {
	}

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;

		stateBoard = new CXCellState[M][N];

		TIMEOUT = timeout_in_secs;
		meint = first ? 0 : 1;
		oppint = first ? 0 : 1;
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
		CXCell lastOppMove = null;
		lastOppMove = B.getLastMove();
		// update our Board and stateBoard
		if (lastOppMove != null) {
			//makeMove(lastOppMove.j, 0);
			Board = B.copy();
			stateBoard = B.getBoard();
		}
		// not the first turn
		if (bestMove != null && lastOppMove != null) {
			TreeNode myOppMove = bestMove.getChildByCell(lastOppMove);
			// ho già calcolato la migliore mossa: aggiorna root
			if (myOppMove != null) {
				root = myOppMove;
			}
			// altrimenti prendo il sottoalbero radicato nell'ultima mossa dell'avversario
			else {
				root = new TreeNode(lastOppMove);
				GenerateMoveList(root);
			}
		}
		// first turn
		else {
			// first round
			if (lastOppMove == null) {
				Board = B.copy();
				stateBoard = B.getBoard();
				bestMove = new TreeNode(makeMove(N / 2, 0));
				GenerateMoveList(bestMove);
				// root = new TreeNode(makeMove(bestMove.getMoves()[0].getMove()));
				makeMove(bestMove.getCell().j, 0);
				return bestMove.getCell().j;
				// second round
			} else {
				Board = B.copy();
				stateBoard = B.getBoard();
				root = new TreeNode(Board.getLastMove());
				GenerateMoveList(root);
			}
		}
		// start iterative deepening
		// try {
			IterativeDeepening(root, me, 10);
		// } catch (Exception e) {
		// 	System.err.println("Exception occurred, printing table");
		// 	Debug.printTable(stateBoard);
		// 	makeMove(bestMove.getCell().j);
		// 	return bestMove.getCell().j;
		// }
		makeMove(bestMove.getCell().j, 0);
		return bestMove.getCell().j;
	}

	/**
	 * Should only be called by {@link #selectColumn(CXBoard)}, this function
	 * searches the game tree in a BFS manner using AlphaBeta algorithm
	 * Updates <code>bestMove</code> only when all nodes of same depth are visited
	 * 
	 * @param T      root node of the game tree
	 * @param player player who's allowed to move
	 * @param depth  depth of search
	 */
	private void IterativeDeepening(TreeNode T, CXCellState player, int depth) {
		int alpha = -Integer.MAX_VALUE; // alpha = -oo
		int beta = Integer.MAX_VALUE; // beta = +oo
		int eval = alpha, bestMoveValue = eval;
		// nella mia testa depth = 0 è la radice quindi se voglio fare una visita solo
		// al
		// primo livello del sottoalbero radicato in T la depth = 1
		for (int d = 1; d <= depth; d++) {
			checktime();
			// generate or get already generated move list
			LabeledMove[] children = T.getMoves();
			if (children == null)
				GenerateMoveList(T);
			System.err.println("figlio di root");
			for (LabeledMove i : T.getMoves()) { // foreach move in T.Moves
				// make this move in the list and add it to the game tree
				TreeNode child = new TreeNode(makeMove(i.getMove(), 3));
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
					bestMove = child;
					bestMoveValue = eval;
					T.label = bestMoveValue;
				}
				if (beta <= alpha) {
					break;
				}
			}
			System.err.println("depth: " + d);
			Debug.printTable(stateBoard);
			// fai partire alpahabeta
			// eval = AlphaBeta(T, player, alpha, beta, d);
			// if (eval > bestMoveValue) {
			// bestMoveValue = eval;
			// bestMove = T;
			// }
			// Debug.printTable(stateBoard);
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
		// siamo in una configurazione finale oppure la visita in profondità è finita
		if (T.isLeaf() || depth == 0) {
			// System.err.println("ramo evaluate");
			// Debug.printTable(stateBoard);
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
		if (children.length == 1) {
			if (T.getMoves()[0].getLabel() == WIN) {
				if (player == me)
					return WIN - Board.numOfMarkedCells();
				else
					return LOSS + Board.numOfMarkedCells();
			} else {
				if (player == me)
					return LOSS + Board.numOfMarkedCells();
				else
					return WIN - Board.numOfMarkedCells();
			}
		}
		int eval;
		// Our player is maximizing
		if (player == me) {
			// Debug.breakpoint();
			eval = -Integer.MAX_VALUE; // eval = -oo
			//System.err.println("ramo max");
			for (LabeledMove i : children) { // foreach move in T.Moves
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
			//System.err.println("ramo min");
			for (LabeledMove i : children) { // foreach move in T.Moves
				// make this move in the list and add it to the game tree
				TreeNode child = new TreeNode(makeMove(i.getMove(), 1));
				if (Board.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// evaluate this move by taking the max value between current eval and value
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
	 * Inizializza le mosse possibili del nodo prima di entrare nel ciclo for di
	 * {@link #AlphaBeta(TreeNode, CXCellState, int, int, int)}, ordinandole in
	 * ordine decrescente di
	 * valore secondo l'euristica.
	 * 1) Se esiste una mossa che ci fa vincere immediatamente, T.moves avrà solo quella mossa valuata a <code>WIN</code>
	 * 2) Se esiste una una mossa che fa vincere immediatamente l'avversario, essa sarà valuata a <code>BLOCK_OPP</code>
	 * 3) Se esistono più di due mosse vincenti per l'avversario, T.moves avrà una sola mossa valuata a <code>LOSS</code>
	 * 4) Se la mossa porta ad un pareggio(ovvero è l'ultima mossa giocabile) viene valuata a 0
	 * 
	 * @param T      nodo il cui campo <code> T.Moves </code> deve essere
	 *               inizializzato
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
			// classico swap
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
	 * @param AM array of available moves
	 * @return array of Labeled Move(s) that are worth exploring with alphabeta
	 */
	private LabeledMove[] possibleNonLosingMoves(Integer[] AM) {
		int oppWinningMoves = 0;
		List<LabeledMove> WorthMoves = new LinkedList<>();
		for (Integer col : AM) {
			boolean add_move = true;
			CXCell move = makeMove(col, 2);
			// We should always play a column on which the opponent has a winning position
			// in the bottom of the column.
			if (Board.gameState() != CXGameState.OPEN) {
				if (Board.gameState() == myWin) {
					LabeledMove[] win = new LabeledMove[1];
					win[0] = new LabeledMove(WIN, move);
					return win;
				} else if (Board.gameState() == yourWin) {
					WorthMoves.add(new LabeledMove(BLOCK_OPP, move));
					add_move = false;
					oppWinningMoves++;
				} else {
					WorthMoves.add(new LabeledMove(0, move));
					add_move = false;
				}
			}
			// We should never play under an opponent winning positions.
			// if I win by placing a stone on top of move position, then the stone under it
			// is worth evaluating
			if (!Board.fullColumn(col) && Board.gameState() == CXGameState.OPEN) {
				makeMove(col, 2);
				// corner case: only 1 column available and we lose by making move
				if (Board.gameState() == yourWin && AM.length == 1) {
					LabeledMove[] loss = new LabeledMove[1];
					loss[0] = new LabeledMove(LOSS, move);
					return loss;
				}
				else if (Board.gameState() == yourWin){
					add_move = false;
				}
				undoMove();
			}
			// If the opponent has more than two directly playable winning positions, then
			// we cannot do anything and we will lose.
			if (oppWinningMoves >= 2) {
				LabeledMove[] loss = new LabeledMove[1];
				loss[0] = new LabeledMove(LOSS, move);
				return loss;
			}
			// after all the checks, just add move to the list
			else if (add_move) {
				WorthMoves.add(new LabeledMove(util.get_helpfulness(stateBoard, move), move));
			}
			undoMove();
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
	private int EvaluateConfiguration(TreeNode T) {
		int eval = 0;
		for (int i = 0; i < Board.getAvailableColumns().length; i++) {
			eval += util.evaluateColumn(stateBoard, T.getCell(), me);
			eval -= util.evaluateColumn(stateBoard, T.getCell(), opponent);
		}
		return eval;
	}

	private CXCell makeMove(int col, int funzione) {
		if (Board.gameState() == CXGameState.OPEN && !Board.fullColumn(col)) {
			Board.markColumn(col);
			CXCell move = Board.getLastMove();
			stateBoard[move.i][move.j] = move.state;
			return move;
		} else {
			if (funzione == 0)
				System.err.println("OPS! makeMove chiamato da select_column");
			else if (funzione == 1)
				System.err.println("OPS! makeMove chiamato da Alphabeta");
			else if (funzione == 2)
				System.err.println("OPS! makeMove chiamato da possibleNonLosingMoves");
			else if (funzione == 3)
				System.err.println("OPS! makeMove chiamato da IterativeDeepening");
			if (Board.gameState() != CXGameState.OPEN) {
				System.err.println("hai provato a giocare dopo che la partita è finita");
			}
			if (Board.fullColumn(col)) {
				System.err.println("hai giocato su una colonna già piena");
			}
			if (col < 0 && col > N) {
				System.err.println("hai cagato fuori dal vaso");
			}
			//Debug.printTable(stateBoard);
			return null;
		}
	}

	private void undoMove() {
		CXCell move = Board.getLastMove();
		stateBoard[move.i][move.j] = CXCellState.FREE;
		Board.unmarkColumn();
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