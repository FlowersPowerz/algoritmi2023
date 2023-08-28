package connectx.BirbaBot;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import java.util.LinkedList;
import java.util.List;

/**
 * Nodo del game tree, alla cella è assegnato un valore in base all'euristica
 */
public class TreeNode {

  private boolean Leaf; // è una foglia del game tree?
  public int label; // valore della cella
  private CXCell cell; // cella della mossa giocata in questo nodo
  private LabeledMove[] Moves; // array delle possibili mosse giocabili al prossimo turno
  private List<TreeNode> childNodes; // lista effettiva dei nodi figli del nodo corrente nel game tree

  public TreeNode(CXCell cell) {
    this.label = 0;
    this.cell = cell;
    this.Leaf = false;
    this.childNodes = new LinkedList<>();
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
    this.Leaf = true;
  }

  public CXCell getCell() {
    return cell;
  }

  public boolean isLeaf() {
    return this.Leaf;
  }

  /**
   * 
   * @param cell the move's cell to search for
   * @return the child of the node containing <code> cell </code>'s move, if it
   *         exists
   */
  public TreeNode getChildByCell(CXCell move) {
    if (this.childNodes != null) {
      for (TreeNode n : this.childNodes) {
        if (n == null)
          break;
        // la cella della mossa è stata trovata, ritorna il nodo
        if (n.getCell().i == move.i && n.getCell().j == move.j) {
          return n;
        }
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

  public int getChildnumber() {
    return childNodes.size();
  }
}
