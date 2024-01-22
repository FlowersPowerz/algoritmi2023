package connectx.GameSolver;

import java.util.Arrays;

public class Solver {
    private long nodeCount;
    private int[] columnOrder;

    public Solver() {
        nodeCount = 0;
        columnOrder = new int[Connect4Position.WIDTH];
        for (int i = 0; i < Connect4Position.WIDTH; i++) {
            columnOrder[i] = Connect4Position.WIDTH / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2;
        }
    }

    public int solve(Connect4Position position, boolean weak) {
        nodeCount = 0;
        if (weak) {
            return negamax(position, -1, 1);
        } else {
            return negamax(position, -Connect4Position.WIDTH * Connect4Position.HEIGHT / 2,
                    Connect4Position.WIDTH * Connect4Position.HEIGHT / 2);
        }
    }

    private int negamax(Connect4Position position, int alpha, int beta) {
        assert alpha < beta;
        nodeCount++;

        if (position.getMoves() == Connect4Position.WIDTH * Connect4Position.HEIGHT) {
            return 0;
        }

        for (int x = 0; x < Connect4Position.WIDTH; x++) {
            if (position.canPlay(columnOrder[x]) && position.isWinningMove(columnOrder[x])) {
                return (Connect4Position.WIDTH * Connect4Position.HEIGHT + 1 - position.getMoves()) / 2;
            }
        }

        int max = (Connect4Position.WIDTH * Connect4Position.HEIGHT - 1 - position.getMoves()) / 2;
        if (beta > max) {
            beta = max;
            if (alpha >= beta) {
                return beta;
            }
        }

        for (int x = 0; x < Connect4Position.WIDTH; x++) {
            if (position.canPlay(columnOrder[x])) {
                Connect4Position nextPosition = new Connect4Position(position);
                nextPosition.play(columnOrder[x]);
                int score = -negamax(nextPosition, -beta, -alpha);

                if (score >= beta) {
                    return score;
                }
                if (score > alpha) {
                    alpha = score;
                }
            }
        }

        return alpha;
    }

    public long getNodeCount() {
        return nodeCount;
    }

    public static void main(String[] args) {
        Solver solver = new Solver();
        boolean weak = false;

        if (args.length > 0 && args[0].equals("-w")) {
            weak = true;
        }

        java.util.Scanner scanner = new java.util.Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Connect4Position position = new Connect4Position();
            int moves = position.play(line);

            if (moves != line.length()) {
                System.err.println("Invalid move at position " + (moves + 1) + " \"" + line + "\"");
            } else {
                long startTime = System.nanoTime();
                int score = solver.solve(position, weak);
                long endTime = System.nanoTime();
                long elapsedTime = endTime - startTime;

                System.out.println(line + " " + score + " " + solver.getNodeCount() + " " + elapsedTime / 1000);
            }
        }
    }
}
