package connectx.ZobristBot;

import connectx.CXCell;
import connectx.CXCellState;

/**
 * Nodo del game tree
 */
public class TreeNode implements Comparable<TreeNode> {
  /** cella della mossa giocata in questo nodo */
  private CXCell cell;
  /** etichetta della mossa calcolata dalla visita con AlphaBeta */
  public int label;
  /** è una foglia del game tree? */
  private boolean leaf;
  /** array ordinato delle possibili mosse giocabili al prossimo turno */
  private LabeledMove[] moves;
  /** lista effettiva dei nodi figli del nodo corrente nel game tree */
  private TreeNode[] children;
  private int index;

  public TreeNode(CXCell cell) {
    this.label = 0;
    this.cell = cell;
    this.leaf = false;
    this.moves = new LabeledMove[0];
    this.children = new TreeNode[0];
    this.index = 0;
  }

  public void addChild(TreeNode child) {
    if (index < 0 || index > moves.length - 1) {
      System.err.println("index: " + index);
      throw new RuntimeException("Invalid index value");
    }
    if (children.length == 0) {
      children = new TreeNode[moves.length];
    }
    children[index] = child;
    index++;
  }

  public int getMovesnumber() {
    return moves.length;
  }

  public void updateMoves(LabeledMove[] sortedMoves) {
    this.moves = sortedMoves;
  }

  public LabeledMove[] getMoves() {
    return moves;
  }

  public void updateLeaf() {
    this.leaf = true;
  }

  public CXCell getCell() {
    return cell;
  }

  public boolean isLeaf() {
    return this.leaf;
  }

  @Override
  public int compareTo(TreeNode other) {
    // Compare by value in ascending order (change the order if needed)
    return Integer.compare(other.label, this.label);
  }

  /**
   * 
   * @param move the move's cell to search for
   * @return the child of the node containing <code> cell </code>'s move, if it
   *         exists
   */
  public TreeNode getChildByCell(CXCell move) {
    if (this.children.length == 0)
      return null;

    for (TreeNode n : this.children) {
      // we found the move in one of the children
      if (n.getCell().i == move.i && n.getCell().j == move.j) {
        return n;
      }
    }
    return null;
  }

  public TreeNode[] getChildren() {
    return children;
  }

  /**
   * DISCLAIMER: every <code>TreeNode</code> cointains the cell that has already
   * been played, so this method returns the player that has to make a move. e.g.
   * <code>this.cell.state -> P1, but this method will return P2, who has to
   * make a move from this node
   * 
   * @return player who has to play next
   */
  public CXCellState getCurrentPlayer() {
    return this.cell.state == CXCellState.P1 ? CXCellState.P2 : CXCellState.P1;
  }
}
