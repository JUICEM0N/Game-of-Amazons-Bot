package ubc.cosc322;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Board {

    public static final byte empty = 0;
    public static final byte black = 1;
    public static final byte white = 2;
    public static final byte arrow = 3;

    public static final int rows = 10;
    public static final int cols = 10;
    private static final int CELL_COUNT = rows * cols;
    private static final int PIECE_STATES = 4; // empty, black, white, arrow

    private static final long ZOBRIST_SEED = 322_2026_03_09L;
    private static final long[][] ZOBRIST = createZobristTable();
    private static final long ZOBRIST_SIDE;

    static {
        Random rng = new Random(ZOBRIST_SEED ^ 0xDEADBEEFL);
        ZOBRIST_SIDE = rng.nextLong();
    }

    private byte[] board;

    // indices 0-3 are black, 4-7 are white
    private int[] queenIndices;

    private boolean isBlackTurn = true;
    private long zHash = 0L;

    public static class Point {
        public int row;
        public int col;

        public Point(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public String toString() {
            return "(" + row + ", " + col + ")";
        }
    }

    public Board(ArrayList<Integer> gameState) {
        this.board = new byte[CELL_COUNT];
        this.queenIndices = new int[8];
        Arrays.fill(this.queenIndices, -1);

        int blackIndex = 0;
        int whiteIndex = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int correctIndex = (i + 1) * 11 + (j + 1);
                int boardIndex = i * cols + j;

                if (correctIndex < gameState.size()) {
                    int value = gameState.get(correctIndex);
                    this.board[boardIndex] = (byte) value;

                    if (value == black) {
                        if (blackIndex < 4) this.queenIndices[blackIndex++] = boardIndex;
                    } else if (value == white) {
                        if (whiteIndex < 4) this.queenIndices[4 + whiteIndex++] = boardIndex;
                    }
                }
            }
        }
        this.zHash = computeFullZHash();
        if (isBlackTurn) this.zHash ^= ZOBRIST_SIDE;
    }

    public Board(Board parent) {
        this.board = new byte[CELL_COUNT];
        this.queenIndices = new int[8];

        System.arraycopy(parent.board, 0, this.board, 0, this.board.length);
        System.arraycopy(parent.queenIndices, 0, this.queenIndices, 0, this.queenIndices.length);

        this.isBlackTurn = parent.isBlackTurn;
        this.zHash = parent.zHash;
    }

    public boolean makeMove(Move m) {
        int startIdx = toIndex(m.queenStart.row, m.queenStart.col);
        int endIdx = toIndex(m.queenEnd.row, m.queenEnd.col);
        int arrowIdx = toIndex(m.arrowPos.row, m.arrowPos.col);

        if (!isValidIndex(startIdx) || !isValidIndex(endIdx) || !isValidIndex(arrowIdx)) return false;

        byte color = isBlackTurn ? black : white;

        if (board[startIdx] != color) return false;
        if (board[endIdx] != empty) return false;

        setCell(startIdx, empty);
        setCell(endIdx, color);

        if (board[arrowIdx] != empty) {
            setCell(endIdx, empty);
            setCell(startIdx, color);
            return false;
        }

        setCell(arrowIdx, arrow);

        int teamStart = isBlackTurn ? 0 : 4;
        int teamEnd = teamStart + 4;
        for (int i = teamStart; i < teamEnd; i++) {
            if (queenIndices[i] == startIdx) {
                queenIndices[i] = endIdx;
                break;
            }
        }

        zHash ^= ZOBRIST_SIDE;
        isBlackTurn = !isBlackTurn;
        return true;
    }

    public void undoMove(Move m) {
        isBlackTurn = !isBlackTurn;
        byte color = isBlackTurn ? black : white;

        int startIdx = toIndex(m.queenStart.row, m.queenStart.col);
        int endIdx = toIndex(m.queenEnd.row, m.queenEnd.col);
        int arrowIdx = toIndex(m.arrowPos.row, m.arrowPos.col);

        setCell(arrowIdx, empty);
        setCell(endIdx, empty);
        setCell(startIdx, color);

        zHash ^= ZOBRIST_SIDE;

        int teamStart = isBlackTurn ? 0 : 4;
        int teamEnd = teamStart + 4;
        for (int i = teamStart; i < teamEnd; i++) {
            if (queenIndices[i] == endIdx) {
                queenIndices[i] = startIdx;
                break;
            }
        }
    }

    public long getZHash() {
        return zHash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int val = board[i * cols + j];
                char sym = '.';
                if (val == black) sym = 'B';
                else if (val == white) sym = 'W';
                else if (val == arrow) sym = 'X';
                sb.append(sym).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public ArrayList<Move> getAllPossibleMoves(int color) {
        ArrayList<Move> moves = new ArrayList<>();
        int teamStart = (color == black) ? 0 : 4;
        int teamEnd = teamStart + 4;

        for (int i = teamStart; i < teamEnd; i++) {
            int currentPos = queenIndices[i];
            if (currentPos == -1) continue;

            int r = currentPos / cols;
            int c = currentPos % cols;
            Point startPoint = new Point(r, c);

            ArrayList<Integer> destinations = getSliderMovesIndices(r, c);

            for (int dest : destinations) {
                board[currentPos] = empty;
                board[dest] = (byte) color;

                int destR = dest / cols;
                int destC = dest % cols;
                Point endPoint = new Point(destR, destC);

                ArrayList<Integer> arrowShots = getSliderMovesIndices(destR, destC);

                for (int arrow : arrowShots) {
                    int arrowR = arrow / cols;
                    int arrowC = arrow % cols;
                    moves.add(new Move(startPoint, endPoint, new Point(arrowR, arrowC)));
                }

                board[dest] = empty;
                board[currentPos] = (byte) color;
            }
        }
        return moves;
    }

    public int getCell(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return -1;
        return board[r * cols + c];
    }

    public Point[] getQueens(int color) {
        int teamStart = (color == black) ? 0 : 4;
        Point[] queens = new Point[4];
        for (int i = 0; i < 4; i++) {
            int idx = queenIndices[teamStart + i];
            if (idx != -1) queens[i] = new Point(idx / cols, idx % cols);
        }
        return queens;
    }

    public ArrayList<Point> sliderMovesFrom(Point p) {
        return sliderMovesFrom(p.row, p.col);
    }

    public ArrayList<Point> sliderMovesFrom(int r, int c) {
        ArrayList<Point> moves = new ArrayList<>();
        ArrayList<Integer> indices = getSliderMovesIndices(r, c);
        for (int idx : indices) moves.add(new Point(idx / cols, idx % cols));
        return moves;
    }

    private ArrayList<Integer> getSliderMovesIndices(int r, int c) {
        ArrayList<Integer> targets = new ArrayList<>();
        int[][] directions = {
                {-1, 0}, {-1, 1}, {0, 1}, {1, 1},
                {1, 0}, {1, -1}, {0, -1}, {-1, -1}
        };

        for (int[] d : directions) {
            int currRow = r + d[0];
            int currCol = c + d[1];

            while (currRow >= 0 && currRow < rows && currCol >= 0 && currCol < cols) {
                int idx = currRow * cols + currCol;
                if (board[idx] != empty) break;
                targets.add(idx);
                currRow += d[0];
                currCol += d[1];
            }
        }
        return targets;
    }

    private int toIndex(int row, int col) {
        return row * cols + col;
    }

    private boolean isValidIndex(int idx) {
        return idx >= 0 && idx < CELL_COUNT;
    }

    private void setCell(int idx, byte newValue) {
        byte oldValue = board[idx];
        if (oldValue == newValue) return;
        zHash ^= ZOBRIST[idx][oldValue];
        board[idx] = newValue;
        zHash ^= ZOBRIST[idx][newValue];
    }

    private long computeFullZHash() {
        long hash = 0L;
        for (int i = 0; i < CELL_COUNT; i++) hash ^= ZOBRIST[i][board[i]];
        return hash;
    }

    private static long[][] createZobristTable() {
        long[][] table = new long[CELL_COUNT][PIECE_STATES];
        Random rng = new Random(ZOBRIST_SEED);
        for (int i = 0; i < CELL_COUNT; i++) {
            for (int s = 0; s < PIECE_STATES; s++) table[i][s] = rng.nextLong();
        }
        return table;
    }

    public boolean isBlackTurn(){
        return isBlackTurn;
    }
}