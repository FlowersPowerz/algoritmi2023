package connectx.BirbaBot;
import java.util.Arrays;
import connectx.CXCellState;

public class Debug {

    /**
     * Utility which prints the current state of the table to the standard error output.
     *
     * @param stateBoard The state of the board to be printed.
     */
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
                        throw new RuntimeException("Hey! How did you get here?!");
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

    // Other methods and classes can go here...
    public static void breakpoint() {
		System.err.println("breakpoint");
	}
}

