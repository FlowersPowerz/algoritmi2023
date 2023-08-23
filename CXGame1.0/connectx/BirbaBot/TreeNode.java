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
  private Integer[] Moves; // array delle possibili mosse giocabili al prossimo turno
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

  // @Override
  // public int compareTo(TreeNode T) {
  // return this.label - T.label;
  // }

  private void countingSort(TreeNode array[], int size, int place) {
    TreeNode[] output = new TreeNode[size + 1];
    int max = array[0].label;
    for (int i = 1; i < size; i++) {
      if (array[i].label > max)
        max = array[i].label;
    }

    int[] count = new int[max + 1];

    for (int i = 0; i < max; ++i)
      count[i] = 0;

    // Calculate count of elements
    for (int i = 0; i < size; i++)
      count[(array[i].label / place) % 10]++;

    // Calculate cumulative count
    for (int i = 1; i < 10; i++)
      count[i] += count[i - 1];
    
    // Place the elements in sorted order
    for (int i = size - 1; i >= 0; i--) {
      output[count[(array[i].label / place) % 10] - 1].label = array[i].label;
      count[(array[i].label / place) % 10]--;
    }
    System.err.println("breakpoint");
    for (int i = 0; i < size; i++)
      array[i] = output[i];
  }

  // Function to get the largest element from an array
  private int getMax(TreeNode array[], int n) {
    int max = array[0].label;
    for (int i = 1; i < n; i++)
      if (array[i].label > max)
        max = array[i].label;
    return max;
  }

  // Main function to implement radix sort
  public void radixSort(TreeNode array[], int size) {
    // Get maximum element
    int max = getMax(array, size);
    // Apply counting sort to sort elements based on place value.
    for (int place = 1; max / place > 0; place *= 10)
      countingSort(array, size, place);
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
        if (i == null)
          break;
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
