package ubc.cosc322;

import java.util.*;

public class Evaluator {
    private static final int W_MOBILITY = 8;
    private static final int W_TERRITORY = 2;
    private static final int W_TRAPPED = 50;

    public static int evaluate(Board b, int myColor) {
        return evaluateSequential(b, myColor);
    }

    private static int evaluateTrappedAmazons(Board b, int myColor, int oppColor) {
        int score = 0;
        Board.Point[] myQueens = b.getQueens(myColor);
        for (Board.Point q : myQueens) {
            if (q == null) continue;

            int moves = countMovesForQueen(b, q);
            if (moves == 0) {
                score -= 1;
            } else {
                score += Math.min(3, moves) / 3;
            }
        }

        Board.Point[] oppQueens = b.getQueens(oppColor);
        for (Board.Point q : oppQueens) {
            if (q == null) continue;

            int moves = countMovesForQueen(b, q);
            if (moves == 0) {
                score += 1;
            }
        }

        return score;
    }

    private static int countMovesForQueen(Board b, Board.Point queen) {
        int count = 0;

        int[][] dirs = {{-1,0},{-1,1},{0,1},{1,1},{1,0},{1,-1},{0,-1},{-1,-1}};

        for (int[] d : dirs) {
            int r = queen.row + d[0];
            int c = queen.col + d[1];

            while (r >= 0 && r < Board.rows && c >= 0 && c < Board.cols &&
                    b.getCell(r, c) == Board.empty) {
                count++;
                r += d[0];
                c += d[1];
            }
        }
        return count;
    }

    private static int evaluateSequential(Board b, int myColor) {
        int opp = (myColor == Board.black) ? Board.white : Board.black;

        int myMoves = b.getAllPossibleMoves(myColor).size();
        int opMoves = b.getAllPossibleMoves(opp).size();
        int mobilityScore = myMoves - opMoves;
        int trappedScore = evaluateTrappedAmazons(b, myColor, opp);
        int territoryScore = territoryDiffSequential(b, myColor, opp);

        return W_MOBILITY * mobilityScore + W_TERRITORY * territoryScore +  W_TRAPPED * trappedScore;
    }

    private static int territoryDiffSequential(Board b, int myColor, int oppColor) {
        int[][] myDist = queenMoveDistanceMap(b, myColor);
        int[][] opDist = queenMoveDistanceMap(b, oppColor);

        return calculateDiff(b, myDist, opDist);
    }

    private static int calculateDiff(Board b, int[][] myDist, int[][] opDist) {
        int my = 0, opp = 0;

        for (int r = 0; r < Board.rows; r++) {
            for (int c = 0; c < Board.cols; c++) {
                if (b.getCell(r, c) != Board.empty) continue;

                int d1 = myDist[r][c];
                int d2 = opDist[r][c];

                if (d1 == INF && d2 == INF) continue;
                if (d2 == INF) { 
                    my++; 
                    continue; 
                }
                if (d1 == INF) { 
                    opp++; 
                    continue; 
                }

                if (d1 < d2) my++;
                else if (d2 < d1) opp++;
            }
        }
        return my - opp;
    }

    private static final int INF = 1_000_000;

    private static int[][] queenMoveDistanceMap(Board b, int color) {
        int[][] dist = new int[Board.rows][Board.cols];
        for (int r = 0; r < Board.rows; r++) 
            Arrays.fill(dist[r], INF);

        ArrayDeque<Board.Point> q = new ArrayDeque<>();

        Board.Point[] queens = b.getQueens(color);
        for (Board.Point p : queens) {
            if (p == null) continue;

            dist[p.row][p.col] = 0;
            q.add(p);
        }

        while (!q.isEmpty()) {
            Board.Point cur = q.removeFirst();
            int cd = dist[cur.row][cur.col];

            for (Board.Point nxt : b.sliderMovesFrom(cur)) {
                if (dist[nxt.row][nxt.col] > cd + 1) {
                    dist[nxt.row][nxt.col] = cd + 1;
                    q.addLast(nxt);
                }
            }
        }

        return dist;
    }
}