package connectx.BirbaBot;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
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
 * [x] simple Evaluate function
 * [x] GameTree che funziona perdavvero
 * [x] AlphaBeta implementation
 * [.] Evaluate con Euristica basta sulla helpfulness
 * [.] HashMap?? con le mosse già computate nei turni precedenti, in modo da non visitare 
 *     ogni volta un nuovo Game Tree
 * [.] AlphaBeta + Iterative Deepening
 */

/**
 * Our Player
 */
public class BirbaBot implements CXPlayer {
	private int TIMEOUT;
	private long START;
	private boolean first;
	private int M, N, X; // M righe, N colonne, X allineamenti
	private CXBoard tmpBoard; // usata per navigare il nostro game tree
	private CXGameState myWin, yourWin;
	private CXCellState me, opponent;
	private TreeNode bestMove, root; // miglior mossa trovata e radice del game tree
	private int nodeCount;

	/* Default empty constructor */
	public BirbaBot() {
	}

	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		this.M = M;
		this.N = N;
		this.X = X;
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
		// Siamo il player P1, nostro primo turno
		if (first && root == null) {
			// la letteratura ci dice che la prima mossa migliore è sempre la colonna di
			// mezzo
			B.markColumn(N / 2);
			// creo la radice e genero il game tree
			root = new TreeNode(B.getLastMove());
			tmpBoard = B.copy();
			AlphaBetaStart(root, me, -1, 1);
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
			AlphaBetaStart(root, me, -1, 1);
			System.err.println("nodi visitati: " + nodeCount);
			System.err.println("bestMove: " + bestMove.getCell().j);
			System.err.println("bestMOveValue: " + bestMove.getLabel());
			for (TreeNode i : root.getChildNodes())
				System.err.println("Valore dei figli di root: " + i.getLabel());
			return bestMove.getCell().j;
		} else {
			tmpBoard = B.copy();
			if (root.getChildnumber() == 0) {
				System.err.println("AlphaBeta al nostro primo turno non ha generato l'albero");
			}
			// ho già una parte del game tree valutato, ma può capitare una configurazione
			// non ancora vista, quindi controllo
			// se il nodo della nostra mossa precedente contiene la mossa appena fatta
			// dall'avversario
			TreeNode lastOppMove = bestMove.getChildByCell(tmpBoard.getLastMove());
			if (lastOppMove == null) {
				// la mossa non è stata trovata, sposto la radice del game tree sul nuovo nodo
				// che non è stato valutato
				System.err.println("Cache miss!");
				TreeNode new_child = new TreeNode(tmpBoard.getLastMove());
				root = new_child;
				AlphaBetaStart(root, me, -1, 1);
				System.err.println("nodi visitati: " + nodeCount);
				System.err.println("bestMove: " + bestMove.getCell().j);
				System.err.println("bestMOveValue: " + bestMove.getLabel());
				for (TreeNode i : root.getChildNodes())
					System.err.println("Valore dei figli di root: " + i.getLabel());
				return bestMove.getCell().j;
			} else {
				// ho trovato la mossa già valutata nel game tree, ritorno la best move del nodo
				System.err.println("Cache Hit!");
				lastOppMove.sortChildren();
				return lastOppMove.getChild(0).getCell().j;
			}
		}
	}

	/**
	 * Comincia l'algoritmo alphabeta partendo dal giocatore che massimizza, ovvero
	 * il
	 * nostro player aggiornando il campo BestMove con la mossa migliore da fare
	 * dopo il nodo T
	 * Questa funzione va chiamata in <code> SelectCell </code> al posto di MinMax
	 * per avere la bestMove nel nostro turno
	 * 
	 * @param T
	 * @param player
	 */
	private void AlphaBetaStart(TreeNode T, CXCellState player, int alpha, int beta) {// throws TimeoutException {
		nodeCount++;
		// checktime();
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
			eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta));
			tmpBoard.unmarkColumn();

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
		// siamo arrivati alla fine dell'albero, chiamo evaluate: +1 win, -1 loss, 0
		// draw
		if (T.isLeaf()) {
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
				// evaluate this move by taking the max value between current eval and value
				// calculated by MinMax
				eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta));
				alpha = Math.max(eval, alpha);
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
				// evaluate this move by taking the max value between current eval and value
				// calculated by MinMax
				eval = Math.min(eval, AlphaBeta(child, me, alpha, beta));
				beta = Math.min(eval, beta);
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
	 * 
	 * @param T      leaf node
	 * @param player current player
	 * @return evaluation of the leaf node
	 * @throws IllegalArgumentException
	 */
	private int evaluate(TreeNode T, CXCellState player) throws IllegalArgumentException {
		// possibile ottimizzazione: usare l'euristica per rendere più precisi i valori
		// di una mossa,
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