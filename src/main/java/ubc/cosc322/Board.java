package ubc.cosc322;

import java.util.ArrayList;

public class Board {

    public static final int empty = 0;
    public static final int black = 1;
    public static final int white = 2;
    public static final int arrow = 3;
    
    public static final int rows = 10;
    public static final int cols = 10;

    private int[][] board; 
    
    private boolean isBlackTurn = true; 

    //[0] for black [1] for white
    private Point[][] queens; 

    public static class Point {
        public int row;
        public int col;

        public Point(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public String toString() {
            return "(" +row+ ", "+col+")";
        }
    }

    public Board(ArrayList<Integer> gameState) {
        this.board = new int[rows][cols];
        this.queens = new Point[2][4];
        int blackIndex = 0;
        int whiteIndex = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int index = (i + 1) * 11 + (j + 1);
                
                if (index < gameState.size()) {
                     int value = gameState.get(index);
                     this.board[i][j] = value;
                     
                     if (value == black) {
                         if (blackIndex < 4) this.queens[0][blackIndex++] = new Point(i, j);
                     } else if (value == white) {
                         if (whiteIndex < 4) this.queens[1][whiteIndex++] = new Point(i, j);
                     }
                }
            }
        }
    }

    public Board(Board parent) {
        this.board = new int[rows][cols];
        this.queens = new Point[2][4];

        for (int i = 0; i<rows; i++) {
            System.arraycopy(parent.board[i], 0,this.board[i], 0,cols);
        }
        
        for (int team = 0; team<2; team++) {
            for (int k = 0; k<4; k++) {
                if (parent.queens[team][k] != null) {
                    this.queens[team][k] = new Point(parent.queens[team][k].row, parent.queens[team][k].col);
                }
            }
        }
        this.isBlackTurn = parent.isBlackTurn;
    }
    
    public boolean makeMove(Move m) {
        int color = isBlackTurn? black:white;
        
        if (board[m.queenStart.row][m.queenStart.col] != color) {
            System.out.println("Invalid move");
            return false;
        }
        board[m.queenStart.row][m.queenStart.col] = empty;
        board[m.queenEnd.row][m.queenEnd.col] = color;

        int teamIndex = isBlackTurn? 0:1;
        for (int i = 0; i<4; i++) {
            if (queens[teamIndex][i].row == m.queenStart.row && queens[teamIndex][i].col == m.queenStart.col) {
                queens[teamIndex][i].row = m.queenEnd.row;
                queens[teamIndex][i].col = m.queenEnd.col;
                break;
            }
        }
        board[m.arrowPos.row][m.arrowPos.col] = arrow;
        isBlackTurn = !isBlackTurn;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<rows; i++) {
            for (int j = 0; j<cols; j++) {
                int val = board[i][j];
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
        int teamIndex = (color == black)? 0:1;

        for (int q = 0; q < 4; q++) {
            Point start = queens[teamIndex][q];
            ArrayList<Point> destinations = getSliderMoves(start.row, start.col);
            
            for (Point end:destinations) {
                board[start.row][start.col] = empty;
                board[end.row][end.col] = color;
                ArrayList<Point> arrowShots = getSliderMoves(end.row, end.col);
                
                for (Point arrow:arrowShots) {
                    moves.add(new Move(new Point(start.row, start.col), new Point(end.row, end.col), new Point(arrow.row, arrow.col)));
                }
                board[end.row][end.col] = empty;
                board[start.row][start.col] = color;
            }
        }
        return moves;
    }

    private ArrayList<Point> getSliderMoves(int r, int c) {
        ArrayList<Point> targets = new ArrayList<>();
        
        int[][] directions = { 
            {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, 
            {1, 0}, {1, -1}, {0, -1}, {-1, -1} 
        };
        
        for (int[] d:directions) {
            int dr = d[0];
            int dc = d[1];
            int currRow = r + dr;
            int currCol = c + dc;
            
            while (currRow>=0 && currRow<rows && 
                    currCol>=0 && currCol<cols && 
                    board[currRow][currCol] == empty) {
                targets.add(new Point(currRow, currCol));
                currRow += dr;
                currCol += dc;
            }
        }
        return targets;
    }

    public int getCell(int r, int c) { return board[r][c]; }

    public Point[] getQueens(int color) {
        int teamIndex = (color == black) ? 0 : 1;
        Point[] out = new Point[4];
        for (int i = 0; i < 4; i++) {
            Point q = queens[teamIndex][i];
            out[i] = (q == null) ? null : new Point(q.row, q.col);
        }
        return out;
    }

    public ArrayList<Point> sliderMovesFrom(int r, int c) { return getSliderMoves(r, c); }

    public ArrayList<Point> sliderMovesFrom(Point p) { return getSliderMoves(p.row, p.col); }

}
