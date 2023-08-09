package connectx.BirbaBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * BucketList:
 * 1) Evaluate
 * 	- Euristic function to calculate the relative value of any column.
 * 2) Search 
 * 	- AlphaBeta with Iterative Deepening
 *  - game Tree nodes sorting by best
 *  - 
 *	- Transposition Table
 *
 * TO DO: 
 * [x] simple Evaluate function
 * [x] GameTree che funziona perdavvero
 * [x] AlphaBeta implementation
 * [.] AlphaBeta + Iterative Deepening
 */

/**
 * Our Player
 */
public class BirbaBot implements CXPlayer {
	private int TIMEOUT;
	private long START;
	private boolean first;
	private int M, N, K;
	private CXBoard tmpBoard; // usata per navigare il nostro game tree
	private CXGameState myWin, yourWin;
	private CXCellState me, opponent;
	private TreeNode bestMove, root; // radice del game tree
	private Map<CXCell, TreeNode> EvaluatedBoard; // hashtable dei nodi già valutati, la posizione della cella è la
													// chiave
	private TreeNode oldnode; // radice dell'ultimo albero visitato, utile per ritrovare la valutazione di mosse già viste
	private int nodeCount;

	/* Default empty constructor */
	public BirbaBot() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.K = K;
		TIMEOUT = timeout_in_secs;
		START = System.currentTimeMillis(); // Save starting time during initialization

		this.first = first;
		myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		me = first ? CXCellState.P1 : CXCellState.P2;
		opponent = first ? CXCellState.P2 : CXCellState.P1;
		// create an empty Hashtable
		EvaluatedBoard = new HashMap<>();
		bestMove = null;
		nodeCount = 0;
		root = null;
	}

	/* Selects the best move possible */
	public int selectColumn(CXBoard B) {
		// Siamo il player P1, nostro primo turno
		if (first && root == null) {
			// la letteratura ci dice che la prima mossa migliore è sempre la colonna di
			// mezzo
			B.markColumn(N / 2);
			// creo la radice e genero il game tree
			root = new TreeNode(B.getLastMove());
			tmpBoard = B.copy();
			AlphaBetaStart(root, me);
			System.err.println("nodi visitati: " + nodeCount);
			System.err.println("bestMove: " + bestMove.getCell().j);
			System.err.println("bestMOveValue: " + bestMove.getLabel());
			for (TreeNode i : root.getChildNodes())
				System.err.println("Valore dei figli di root: " + i.getLabel());
			return N / 2;
		}
		// siamo il player P2, nostro primo turno
		else if (!first && root == null) {
			// l'ultima mossa fatta dall'avversario è la radice del mio albero di gioco
			root = new TreeNode(B.getLastMove());
			tmpBoard = B.copy();
			AlphaBetaStart(root, me);
			System.err.println("nodi visitati: " + nodeCount);
			System.err.println("bestMove: " + bestMove.getCell().j);
			System.err.println("bestMOveValue: " + bestMove.getLabel());
			for (TreeNode i : root.getChildNodes())
				System.err.println("Valore dei figli di root: " + i.getLabel());
			return bestMove.getCell().j;
		} else {
			// ho già una parte di tabella valuata, ma può capitare una configurazione non
			// ancora vista
			if (root.getChildnumber() == 0) {
				System.err.println("MinMax al primo turno non ha generato l'albero");
			}
			// copio il nodo della mossa precedente
			// TreeNode oppMove = EvaluatedBoard.get(B.getLastMove());
			// ritorno il primo figlio, visto che l'array dei figli è ordinato
			// decrescente
			// if (!EvaluatedBoard.containsValue(oppMove)) {
				// System.err.println("Cache miss");
				root = new TreeNode(B.getLastMove());
				tmpBoard = B.copy();
				EvaluatedBoard.put(B.getLastMove(), root);
				// try {
				AlphaBetaStart(root, me);
				System.err.println("nodi visitati: " + nodeCount);
				System.err.println("bestMove: " + bestMove.getCell().j);
				System.err.println("bestMOveValue: " + bestMove.getLabel());
				for (TreeNode i : root.getChildNodes())
					System.err.println("Valore dei figli di root: " + i.getLabel());
				return bestMove.getCell().j;
			// } else
			// 	return oppMove.getChild(0).getCell().j;
			// TreeNode oppMove = new TreeNode(B.getLastMove());
			// tmpBoard = B.copy();
			// try {
			// MinMaxStart(oppMove, me);
			// return bestMove.getCell().j;
			// } catch (Exception e) {
			// System.err.println("Timeout turno n, seleziono la prima colonna libera, figli
			// di oppMove "+ oppMove.getChildnumber());
			// Integer[] L = B.getAvailableColumns();
			// return L[0];
			// }
		}
	}

	/**
	 * Comincia l'algoritmo minimax partendo dal giocatore che massimizza, ovvero il
	 * nostro player
	 * Aggiornando il campo BestMove con la mossa migliore da fare dopo il nodo T
	 * Questa funzione va chiamata in <code> SelectCell </code> al posto di MinMax
	 * per avere la bestMove nel nostro turno
	 * 
	 * @param T
	 * @param player
	 */
	private void AlphaBetaStart(TreeNode T, CXCellState player) {// throws TimeoutException {
		nodeCount++;
		// checktime();
		// System.err.println("entrato in Startminmax");
		int eval = -Integer.MAX_VALUE; // eval = -oo
		int bestMoveValue = eval;
		// maximizing player
		T.GenerateMoveList(tmpBoard);
		System.err.println("Numero di mosse contemplabili: " + T.getMovesnumber());
		for (int i : T.getMoves()) { // foreach move in T.list
			// make this move in the list
			tmpBoard.markColumn(i);
			// add this move to the game tree
			TreeNode child = new TreeNode(tmpBoard.getLastMove());
			if (tmpBoard.gameState() != CXGameState.OPEN)
				child.updateLeaf();
			T.addChild(child);
			// evaluate this move by taking the max value between current eval and value
			// calculated by MinMax
			eval = Math.max(eval, AlphaBeta(child, opponent, -1, 1));
			// EvaluatedBoard.put(child.getCell(), child);
			tmpBoard.unmarkColumn();
			// lastNode = child;

			if (eval > bestMoveValue) {
				bestMove = child;
				bestMoveValue = eval;
				T.updateLabel(bestMoveValue);
			}
		}
	}

	/**
	 * Algoritmo MinMax per valutare il Game Tree
	 * 
	 * @param T      nodo dell'albero di gioco da cui partire
	 * @param player turno del giocatore corrente
	 * @return la valutazione di una configurazione di gioco secondo l'algoritmo
	 *         MinMax
	 * @throws TimeoutException
	 */
	private int AlphaBeta(TreeNode T, CXCellState player, int alpha, int beta) {// throws TimeoutException {
		nodeCount++;
		// checktime();
		int eval;
		// System.err.println("entrato in minmax");
		// siamo arrivati alla fine dell'albero, chiamo evaluate: +1 win, -1 loss, 0
		// draw
		if (T.isLeaf()) {
			// System.err.println("entrato nel ramo evaluate");
			eval = evaluate(T, player);
		}
		// Our player is maximizing
		else if (player == me) {
			eval = -Integer.MAX_VALUE; // eval = -oo
			// generate move list
			T.GenerateMoveList(tmpBoard);
			for (int i : T.getMoves()) { // foreach move in T.list
				// make this move in the list
				tmpBoard.markColumn(i);
				// add this move to the game tree
				TreeNode child = new TreeNode(tmpBoard.getLastMove());
				if (tmpBoard.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// System.err.println("è una foglia?" + child.isLeaf());
				// evaluate this move by taking the max value between current eval and value
				// calculated by MinMax
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta));
				alpha = Math.max(eval, alpha);
				// EvaluatedBoard.put(child.getCell(), child);
				tmpBoard.unmarkColumn();
				// lastNode = child;
				if (beta <= alpha)
					break;
			}
		}
		// Opponent minimizing
		else {
			eval = Integer.MAX_VALUE; // eval = +oo
			T.GenerateMoveList(tmpBoard);
			for (int i : T.getMoves()) {
				// make this move in the list
				tmpBoard.markColumn(i);
				// add this move to the game tree
				TreeNode child = new TreeNode(tmpBoard.getLastMove());
				if (tmpBoard.gameState() != CXGameState.OPEN)
					child.updateLeaf();
				T.addChild(child);
				// System.err.println("è una foglia?" + child.isLeaf());
				// evaluate this move by taking the max value between current eval and value
				// calculated by MinMax
				eval = Math.min(eval, AlphaBeta(child, me, alpha, beta));
				beta = Math.min(eval, beta);
				// EvaluatedBoard.put(child.getCell(), child);
				tmpBoard.unmarkColumn();
				// lastNode = child;
				if (beta <= alpha)
					break;
			}
		}
		T.updateLabel(eval);
		return eval;
	}

	/**
	 * Valuta le configurazioni finali del Game Tree
	 * Il giocatore P1 è quello che massimizza(vittoria = -1)
	 * Il giocatore P2 è quello che minimizza(vittoria = 1)
	 * @param T leaf node
	 * @param player current player
	 * @return evaluation of the leaf node
	 * @throws IllegalArgumentException
	 */
	private int evaluate(TreeNode T, CXCellState player) throws IllegalArgumentException {
		// possibile ottimizzazione: usare l'euristica per rendere più precisi i valori di una mossa,
		// anche in una configurazione non finale
		// win = +1
		if (tmpBoard.gameState() == myWin) {
			return 1;
		}
		// loss = -1
		else if (tmpBoard.gameState() == yourWin) {
			return -1;
			// draw = 0
		} else if (tmpBoard.gameState() == CXGameState.DRAW) {
			return 0;
		} else
			throw new IllegalArgumentException("è stata passata una configurazione non finale");
	}

	/**
	 * Throws a <code> TimeoutException </code> if we are at 99 percent of the
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

