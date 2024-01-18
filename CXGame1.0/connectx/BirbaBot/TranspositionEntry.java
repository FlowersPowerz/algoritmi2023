package connectx.BirbaBot;

public class TranspositionEntry {
    private int eval;
    private int depth;
    private TreeNode bestMove;

    public TranspositionEntry(int evaluation, int depth, TreeNode bestMove) {
        this.eval = evaluation;
        this.depth = depth;
        this.bestMove = bestMove;
    }

    public int getEval() {
        return eval;
    }

    public int getDepth() {
        return depth;
    }

    public TreeNode getBestMove() {
        return bestMove;
    }
}
