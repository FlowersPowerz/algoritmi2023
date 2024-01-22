package connectx.BirbaBot;
import java.util.Arrays;

import connectx.CXCellState;

public class Debug {

    public static void printMoves(LabeledMove[] moves) {
        for (LabeledMove i : moves) {
            System.err.println("Mossa: " + i.getMove() + ", Valore: " + i.getValue());
        }
    }

    public static void printChildren(TreeNode root) {
        int count = 0;
        for (TreeNode i : root.getChildren()) {
            System.err.println("figlio " + count + ", label -> " + i.label);
            count++;
        }
    }

    /**
     * Utility which prints the current state of the table to the standard error
     * output,
     * including values inside the free columns.
     *
     * @param stateBoard The state of the board to be printed.
     * @param moves      Array of labeled moves representing values in the free
     *                   columns.
     */
    public static void printValueTable(CXCellState[][] stateBoard, LabeledMove[] moves) {
        int M = stateBoard.length;
        int N = stateBoard[0].length;

        class PrintValueTable {
            final CXCellState state;
            final int value;

            public PrintValueTable(CXCellState state, int value) {
                this.state = state;
                this.value = value;
            }

            @Override
            public String toString() {
                switch (state) {
                    case FREE:
                        if (value >= 0) {
                            return Integer.toString(value);
                        } else {
                            return " ";
                        }
                    case P1:
                        return "X";
                    case P2:
                        return "O";
                    default:
                        throw new RuntimeException("Hey! How did you get here?!");
                }
            }
        }

        PrintValueTable[][] printable = new PrintValueTable[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                printable[i][j] = new PrintValueTable(stateBoard[i][j], -1);
            }
        }

        for (LabeledMove move : moves) {
            printable[0][move.getMove()] = new PrintValueTable(CXCellState.FREE, move.getValue());
        }

        int length = 4 * N + 1;
        char[] c = new char[length];
        Arrays.fill(c, '-');
        for (int i = 0; i < length; i += 4) {
            c[i] = '+';
        }
        String lines = new String(c);

        Arrays.fill(c, ' ');
        for (int i = 0; i < length; i += 4) {
            c[i] = '|';
        }

        for (int i = 0; i < M; i++) {
            System.err.println(lines);

            char[] arr = new char[length];
            System.arraycopy(c, 0, arr, 0, length);

            for (int j = 0, n = 1; j < N; j++, n += 4) {
                String s = printable[i][j].toString();
                arr[n + 1] = s.charAt(0);
                if (s.length() == 2) {
                    arr[n + 2] = s.charAt(1);
                }
            }

            System.err.println(arr);
        }
        System.err.println(lines);
    }

    public static void printTable(CXCellState[][] stateBoard) {
        int M = stateBoard.length;
        int N = stateBoard[0].length;

        class Printable {
            final CXCellState state;

            public Printable(CXCellState state) {
                this.state = state;
            }

            @Override
            public String toString() {
                switch (state) {
                    case FREE:
                        return " ";
                    case P1:
                        return "X";
                    case P2:
                        return "O";
                    default:
                        throw new RuntimeException("Bro che ci fai qui");
                }
            }
        }

        Printable[][] printable = new Printable[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                printable[i][j] = new Printable(stateBoard[i][j]);
            }
        }
        int length = 4 * N + 1;
        char[] c = new char[length];
        Arrays.fill(c, '-');
        for (int i = 0; i < length; i += 4) {
            c[i] = '+';
        }
        String lines = new String(c);

        Arrays.fill(c, ' ');
        for (int i = 0; i < length; i += 4) {
            c[i] = '|';
        }
        for (int i = 0; i < M; i++) {
            System.err.println(lines);

            char[] arr = new char[length];
            System.arraycopy(c, 0, arr, 0, length);

            for (int j = 0, n = 1; j < N; j++, n += 4) {
                String s = printable[i][j].toString();
                arr[n + 1] = s.charAt(0);
            }

            System.err.println(arr);
        }
        System.err.println(lines);
    }

    public static void breakpoint() {
        System.err.println("breakpoint");
    }

}
