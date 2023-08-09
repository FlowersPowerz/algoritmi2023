package connectx.BirbaBot;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Nodo del game tree, alla cella è assegnato un valore in base all'euristica
 */
public class TreeNode implements Comparable<TreeNode> {

  private boolean Leaf; // è una foglia del game tree?
  private int label; // valore della cella
  private CXCell cell; // cella della mossa da effettuare in questo nodo
  private Integer[] Moves; // array delle possibili mosse effettuabili al prossimo turno
  private List<TreeNode> childNodes; // lista effettiva dei nodi figli del nodo corrente nel game tree

  public TreeNode(CXCell cell) {
    // maximizing player
    this.label = 10;
    this.cell = cell;
    this.Leaf = false;
    this.childNodes = new LinkedList<>();
  }

  public void addChild(TreeNode childNode) {
    this.childNodes.add(childNode);
  }

  /**
   * Aggiunge tutte le mosse possibili alla lista Moves controllando se le colonne
   * sono libere
   * 
   * @param B tabella di gioco
   */
  public void GenerateMoveList(CXBoard B) {
    this.Moves = B.getAvailableColumns(); // lista delle colonne ancora libere
    // possibile ottimizzazione: generare la lista delle possibili mosse andando
    // subito ad escludere
    // quelle che ci faranno perdere
    // TO DO: creare una funzione possibleNonLosingMoves() che ritorna la lista
    // delle mosse degne di essere valutate
  }

  // private Integer[] possibleNonLosingMoves() {

  // }

  @Override
  public int compareTo(TreeNode T) {
    return this.label - T.label;
  }

  public void sortChildren() {
    Collections.sort(childNodes);
  }

  public int getMovesnumber() {
    return Moves.length;
  }

  public Integer[] getMoves() {
    return Moves;
  }

  public int getLabel() {
    return label;
  }

  public void updateLabel(int val) {
    this.label = val;
  }

  public void updateLeaf() {
    this.Leaf = true;
  }

  public CXCell getCell() {
    return cell;
  }

  public boolean isLeaf() {
    return this.Leaf;// || T.childNodes.isEmpty();
  }

  /**
   * 
   * @param cell the move's cell to search for
   * @return the child of the node containing <code> cell </code>'s move, if it
   *         exists
   */
  public TreeNode getChildByCell(CXCell move) {
    if (childNodes != null) {
      for (TreeNode i : this.childNodes) {
        if (i == null) break;
        // la cella della mossa è stata trovata, ritorna il nodo
        if (i.getCell().i == move.i && i.getCell().j == move.j) {
          return i;
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
