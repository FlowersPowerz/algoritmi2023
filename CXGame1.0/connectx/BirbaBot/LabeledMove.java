package connectx.BirbaBot;

import connectx.CXCell;

public class LabeledMove {
    private int value;
    private CXCell move;

    public LabeledMove(int label, CXCell cell) {
        this.value = label;
        this.move = cell;
    }

    public int getMove() {
        return this.move.j;
    }

    public int getLabel() {
        return this.value;
    }

    // Using counting sort to sort the elements based on significant places in descending order
    private static void countingSortDescending(LabeledMove array[], int size, int place) {
        LabeledMove[] output = new LabeledMove[size];
        int[] count = new int[10];

        for (int i = 0; i < size; i++)
            count[9 - ((array[i].value / place) % 10)]++; // Invert the counting for descending order

        for (int i = 1; i < 10; i++)
            count[i] += count[i - 1];

        for (int i = size - 1; i >= 0; i--) {
            output[count[9 - ((array[i].value / place) % 10)] - 1] = array[i];
            count[9 - ((array[i].value / place) % 10)]--;
        }

        System.arraycopy(output, 0, array, 0, size);
    }

    // Function to get the largest element's value from an array
    private static int getMax(LabeledMove array[], int n) {
        int max = array[0].value;
        for (int i = 1; i < n; i++)
            if (array[i].value > max)
                max = array[i].value;
        return max;
    }

    // Main function to implement radix sort in descending order
    public static void radixSort(LabeledMove array[], int size) {
        // Get maximum element's value
        int max = getMax(array, size);

        // Apply counting sort to sort elements based on place value.
        for (int place = 1; max / place > 0; place *= 10)
            countingSortDescending(array, size, place);
    }
}

	// /**
	//  * Comincia l'algoritmo alphabeta partendo dal giocatore che massimizza, ovvero
	//  * il
	//  * nostro player aggiornando il campo BestMove con la mossa migliore da fare
	//  * dopo il nodo T
	//  * Questa funzione va chiamata in <code> SelectCell </code> al posto di
	//  * AlphaBeta
	//  * per avere la bestMove nel nostro turno
	//  * 
	//  * @param T
	//  * @param player
	//  * @param alpha
	//  * @param beta
	//  */
	// private void AlphaBetaStart(TreeNode T, CXCellState player, int alpha, int beta) {
	// 	nodeCount++;
	// 	checktime();
	// 	int eval = -Integer.MAX_VALUE; // eval = -oo
	// 	int bestMoveValue = eval;
	// 	// maximizing player
	// 	GenerateMoveList(T, player);
	// 	System.err.println("Numero di mosse contemplabili: " + T.getMovesnumber());
	// 	for (LabeledMove i : T.getMoves()) { // foreach move in T.list
	// 		// make this move in the list
	// 		Board.markColumn(i.getMove());
	// 		// add this move to the game tree
	// 		TreeNode child = new TreeNode(Board.getLastMove());
	// 		if (Board.gameState() != CXGameState.OPEN)
	// 			child.updateLeaf();
	// 		T.addChild(child);
	// 		// evaluate this move by taking the max value between current eval and value
	// 		// calculated by Alphabeta
	// 		eval = Math.max(eval, AlphaBeta(child, opponent, alpha, beta));
	// 		Board.unmarkColumn();

	// 		if (eval > bestMoveValue) {
	// 			bestMove = child;
	// 			bestMoveValue = eval;
	// 			T.label = bestMoveValue;
	// 		}

	// 		if (beta <= alpha) {
	// 			break;
	// 		}
	// 	}
	// }