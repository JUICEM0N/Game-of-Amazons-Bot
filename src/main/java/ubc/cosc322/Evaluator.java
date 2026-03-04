package ubc.cosc322;

import java.util.*;

public class Evaluator {
    private static final int W_MOBILITY = 8;
    private static final int W_TERRITORY = 2;

    public static int evaluate(Board b, int myColor) {
        int opp = (myColor == Board.black) ? Board.white : Board.black;

        int myMoves = b.getAllPossibleMoves(myColor).size();
        int opMoves = b.getAllPossibleMoves(opp).size();
        int mobilityScore = myMoves - opMoves;

        int territoryScore = territoryDiff(b, myColor, opp);

        // Eval info
        System.out.println(
            "[Eval] mobility=" + mobilityScore +
            " territory=" + territoryScore +
            " total=" + (W_MOBILITY * mobilityScore + W_TERRITORY * territoryScore)
        );

        return W_MOBILITY * mobilityScore + W_TERRITORY * territoryScore;
    }

    private static int territoryDiff(Board b, int myColor, int oppColor) {
        int[][] myDist = queenMoveDistanceMap(b, myColor);
        int[][] opDist = queenMoveDistanceMap(b, oppColor);

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