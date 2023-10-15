package connectx.BirbaBot;

import connectx.CXCell;
import connectx.CXCellState;

import java.util.LinkedList;
import java.util.List;

/**
 * Nodo del game tree
 */
public class TreeNode {
  /** cella della mossa giocata in questo nodo */
  private CXCell cell;
  /** etichetta della mossa calcolata dalla visita con AlphaBeta */
  public int label;
  /** è una foglia del game tree? */
  private boolean leaf;
  /** array ordinato delle possibili mosse giocabili al prossimo turno */
  private LabeledMove[] Moves;
  /** lista effettiva dei nodi figli del nodo corrente nel game tree */
  private List<TreeNode> childNodes;

  public TreeNode(CXCell cell) {
    this.label = 0;
    this.cell = cell;
    this.leaf = false;
    this.childNodes = new LinkedList<>();
    this.Moves = null;
  }

  public void addChild(TreeNode childNode) {
    this.childNodes.add(childNode);
  }

  public int getMovesnumber() {
    return Moves.length;
  }

  public void updateMoves(LabeledMove[] sortedMoves) {
    this.Moves = sortedMoves;
  }

  public LabeledMove[] getMoves() {
    return Moves;
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

  /**
   * 
   * @param move the move's cell to search for
   * @return the child of the node containing <code> cell </code>'s move, if it
   *         exists
   */
  public TreeNode getChildByCell(CXCell move) {
    if (this.childNodes.size() == 0)
      return null;

    for (TreeNode n : this.childNodes) {
      // la cella della mossa è stata trovata, ritorna il nodo
      if (n.getCell().i == move.i && n.getCell().j == move.j) {
        return n;
      }
    }
    return null;
  }

  public TreeNode getChild(int index) {
    return childNodes.get(index);
  }

  public List<TreeNode> getChildNodes() {
    return childNodes;
  }

  /**
   * DISCLAIMER: every <code>TreeNode</code> cointains the cell that has already
   * been played, so this method returns the player that has to make a move. e.g.
   * <code>this.cell.state -> P1, but this method will return P2, which has to
   * make a move from this node
   * 
   * @return the player whose turn is to play
   */
  public CXCellState getCurrentPlayer() {
    return this.cell.state == CXCellState.P1 ? CXCellState.P2 : CXCellState.P1;
  }
}
