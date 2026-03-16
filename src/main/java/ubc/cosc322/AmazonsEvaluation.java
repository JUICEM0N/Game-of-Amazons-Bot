package ubc.cosc322;

import java.util.*;

public class AmazonsEvaluation {
    // Board representation constants
    public static final int EMPTY = 0;
    public static final int WHITE_AMAZON = 1;
    public static final int BLACK_AMAZON = 2;
    public static final int ARROW = 3;

    // Direction vectors for queen moves (8 directions)
    private static final int[][] QUEEN_DIRS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    // Direction vectors for king moves (8 directions) - same as queen but limited to 1 step
    private static final int[][] KING_DIRS = QUEEN_DIRS; // Same directions, just limited to distance 1
    private static final double KAPPA = 0.1; // First move advantage estimate
    private static final double ENCLOSED_MALUS_THRESHOLD = 10.0; // Malus for enclosed amazon at start
    private static final double ALPHA_ENCLOSED = 5.0; // Threshold for "almost enclosed"

    private int rows;
    private int cols;
    private int[][] board;
    private List<int[]> whiteAmazons;
    private List<int[]> blackAmazons;

    public AmazonsEvaluation(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.board = new int[rows][cols];
        this.whiteAmazons = new ArrayList<>();
        this.blackAmazons = new ArrayList<>();
    }

    public double evaluate(int[][] currentBoard, boolean isWhiteToMove) {
        this.board = currentBoard;
        updateAmazonPositions();
        double w = calculatePhaseWeight();
        double t1 = calculateTerritorialEvaluation1();
        double t2 = calculateTerritorialEvaluation2();
        double c1 = calculateQualityEvaluation1();
        double c2 = calculateQualityEvaluation2();
        double mobility = calculateMobilityTerm();
        double[] weights = calculateDynamicWeights(w);
        double t = weights[0] * t1 + weights[1] * c1 + weights[2] * c2 + weights[3] * t2;
        double finalEval = t + mobility;

        if (isWhiteToMove) {
            finalEval += KAPPA;
        } else {
            finalEval -= KAPPA;
        }

        return finalEval;
    }

    private void updateAmazonPositions() {
        whiteAmazons.clear();
        blackAmazons.clear();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == WHITE_AMAZON) {
                    whiteAmazons.add(new int[]{i, j});
                } else if (board[i][j] == BLACK_AMAZON) {
                    blackAmazons.add(new int[]{i, j});
                }
            }
        }
    }
    private double calculatePhaseWeight() {
        double w = 0.0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == EMPTY) {
                    int d1White = calculateD1(i, j, true);
                    int d1Black = calculateD1(i, j, false);

                    if (d1White < Integer.MAX_VALUE && d1Black < Integer.MAX_VALUE) {
                        w += Math.pow(2, -Math.abs(d1White - d1Black));
                    }
                }
            }
        }

        return w;
    }

    private int calculateD1(int row, int col, boolean forWhite) {
        List<int[]> amazons = forWhite ? whiteAmazons : blackAmazons;
        int minDist = Integer.MAX_VALUE;

        for (int[] amazon : amazons) {
            int dist = calculateQueenDistance(amazon[0], amazon[1], row, col);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private int calculateD2(int row, int col, boolean forWhite) {
        List<int[]> amazons = forWhite ? whiteAmazons : blackAmazons;
        int minDist = Integer.MAX_VALUE;
        for (int[] amazon : amazons) {
            int dist = calculateKingDistance(amazon[0], amazon[1], row, col);
            if (dist < minDist) {
                minDist = dist;
            }
        }

        return minDist;
    }
    private int calculateQueenDistance(int r1, int c1, int r2, int c2) {
        if (r1 == r2 && c1 == c2) return 0;

        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{r1, c1, 0});
        visited[r1][c1] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];
            int dist = current[2];

            for (int[] dir : QUEEN_DIRS) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                while (isInBounds(nr, nc) && !isOccupied(nr, nc)) {
                    if (!visited[nr][nc]) {
                        if (nr == r2 && nc == c2) {
                            return dist + 1;
                        }
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc, dist + 1});
                    }
                    nr += dir[0];
                    nc += dir[1];
                }
            }
        }

        return Integer.MAX_VALUE;
    }

    private int calculateKingDistance(int r1, int c1, int r2, int c2) {
        if (r1 == r2 && c1 == c2) return 0;
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{r1, c1, 0});
        visited[r1][c1] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];
            int dist = current[2];

            for (int[] dir : KING_DIRS) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (isInBounds(nr, nc) && !isOccupied(nr, nc) && !visited[nr][nc]) {
                    if (nr == r2 && nc == c2) {
                        return dist + 1;
                    }
                    visited[nr][nc] = true;
                    queue.add(new int[]{nr, nc, dist + 1});
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private double calculateTerritorialEvaluation1() {
        double t1 = 0.0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == EMPTY) {
                    int d1White = calculateD1(i, j, true);
                    int d1Black = calculateD1(i, j, false);
                    t1 += delta(d1White, d1Black);
                }
            }
        }
        return t1;
    }

    private double calculateTerritorialEvaluation2() {
        double t2 = 0.0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == EMPTY) {
                    int d2White = calculateD2(i, j, true);
                    int d2Black = calculateD2(i, j, false);
                    t2 += delta(d2White, d2Black);
                }
            }
        }

        return t2;
    }

    private double calculateQualityEvaluation1() {
        double c1 = 0.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == EMPTY) {
                    int d1White = calculateD1(i, j, true);
                    int d1Black = calculateD1(i, j, false);

                    if (d1White < Integer.MAX_VALUE) {
                        c1 += Math.pow(2, -d1White);
                    }
                    if (d1Black < Integer.MAX_VALUE) {
                        c1 -= Math.pow(2, -d1Black);
                    }
                }
            }
        }
        return 2.0 * c1;
    }

    private double calculateQualityEvaluation2() {
        double c2 = 0.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == EMPTY) {
                    int d2White = calculateD2(i, j, true);
                    int d2Black = calculateD2(i, j, false);

                    if (d2White < Integer.MAX_VALUE && d2Black < Integer.MAX_VALUE) {
                        double diff = (d2Black - d2White) / 6.0;
                        c2 += Math.min(1.0, Math.max(-1.0, diff));
                    }
                }
            }
        }

        return c2;
    }

    private double delta(int dWhite, int dBlack) {
        if (dWhite == Integer.MAX_VALUE && dBlack == Integer.MAX_VALUE) {
            return 0.0;
        }
        if (dWhite == Integer.MAX_VALUE) {
            return -1.0;
        }
        if (dBlack == Integer.MAX_VALUE) {
            return 1.0;
        }
        if (dWhite < dBlack) {
            return 1.0 - KAPPA;
        }
        if (dWhite > dBlack) {
            return -1.0 + KAPPA;
        }
        return KAPPA;
    }

    private double calculateMobilityTerm() {
        double w = calculatePhaseWeight();
        double m = 0.0;

        for (int[] amazon : blackAmazons) {
            double alpha = calculateAlpha(amazon[0], amazon[1], false);
            m += mobilityPenalty(w, alpha);
        }
        for (int[] amazon : whiteAmazons) {
            double alpha = calculateAlpha(amazon[0], amazon[1], true);
            m -= mobilityPenalty(w, alpha);
        }
        return m;
    }
    private double calculateAlpha(int row, int col, boolean isWhite) {
        double alpha = 0.0;
        for (int[] dir : KING_DIRS) {
            int nr = row + dir[0];
            int nc = col + dir[1];

            if (isInBounds(nr, nc)) {
                double nB = calculateN(nr, nc);
                double d2Weight = 1.0;
                alpha += d2Weight * nB;
            }
        }
        alpha += calculateN(row, col);

        return alpha;
    }

    private double calculateN(int row, int col) {
        double count = 0.0;
        for (int[] dir : KING_DIRS) {
            int nr = row + dir[0];
            int nc = col + dir[1];
            if (isInBounds(nr, nc) && board[nr][nc] == EMPTY) {
                count += 1.0;
            }
        }
        return count;
    }

    private double mobilityPenalty(double w, double alpha) {
        if (w == 0) return 0.0;
        double basePenalty = ENCLOSED_MALUS_THRESHOLD * w;
        if (alpha <= ALPHA_ENCLOSED) {
            return basePenalty;
        } else {
            double factor = Math.max(0, 1.0 - (alpha - ALPHA_ENCLOSED) / (ALPHA_ENCLOSED * 3));
            return basePenalty * factor;
        }
    }

    private double[] calculateDynamicWeights(double w) {
        double[] weights = new double[4];
        double normalizedW = Math.min(1.0, w / 50.0);

        weights[0] = 1.0 - normalizedW;
        weights[3] = normalizedW * 0.3;
        weights[1] = normalizedW * 0.5;
        weights[2] = normalizedW * 0.2;

        double sum = weights[0] + weights[1] + weights[2] + weights[3];
        for (int i = 0; i < 4; i++) {
            weights[i] /= sum;
        }
        return weights;
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private boolean isOccupied(int row, int col) {
        return board[row][col] != EMPTY;
    }
}