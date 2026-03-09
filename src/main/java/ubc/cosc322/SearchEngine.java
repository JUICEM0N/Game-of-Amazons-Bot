package ubc.cosc322;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchEngine {

    private static final int INF = 1_000_000_000;
    private static final int MAX_DEPTH = 50;
    private static final boolean DEBUG = true;
    // private long prunedBranches = 0;
    // private long nodes = 0;

    private long nodes = 0;
    private long cutoffs = 0;
    private long prunedMoves = 0;

    private long totalNodes = 0;
    private long totalCutoffs = 0;
    private long totalPrunedMoves = 0;
    private long totalSearchTimeMs = 0;
    private long totalMovesPlayed = 0;
    // Killer moves: store 2 moves per depth
    private Move[][] killerMoves = new Move[MAX_DEPTH][2]; 

    // History heuristic: track move effectiveness across the entire tree
    private Map<String, Integer> historyTable = new HashMap<>();
    
    public Move chooseMove(Board root, int myColor, long timeLimitMs) {
        long deadline = System.nanoTime() + timeLimitMs * 1_000_000L;

        nodes = 0;
        cutoffs = 0;
        prunedMoves = 0;

        long moveStartMs = System.currentTimeMillis();

        Move bestMove = null;
        int bestScore = -INF;

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {

            if (System.nanoTime() >= deadline) break;

            long iterStartMs = System.currentTimeMillis();
            SearchResult res = alphaBetaRoot(root, depth, myColor, deadline);
            long iterElapsed = System.currentTimeMillis() - iterStartMs;

            // SearchResult res = alphaBetaRoot(root, depth, myColor, deadline);

            // long elapsed = System.currentTimeMillis() - moveStartMs;

            if (DEBUG) {
                long nps = (nodes * 1000L) / (Math.max(1, (System.currentTimeMillis() - moveStartMs)));
                System.out.println("[ID] depth=" + depth +
                        " bestScore=" + res.score +
                        " bestMove=" + res.bestMove +
                        " iterTime=" + iterElapsed + "ms" +
                        " nodes=" + nodes +
                        " cutoffs=" + cutoffs +
                        " prunedMoves=" + prunedMoves +
                        " nps=" + nps);
            }

            // System.out.println(
            //     "[Search Stats] depth=" + depth +
            //     " nodes=" + nodes +
            //     " pruned=" + prunedBranches +
            //     " time=" + elapsed + "ms"
            // );

            if (res.timedOut) break;
            if (res.bestMove != null) {
                bestMove = res.bestMove;
                bestScore = res.score;
            }
        }

        if (bestMove == null) {
            ArrayList<Move> moves = root.getAllPossibleMoves(myColor);
            if (!moves.isEmpty()) 
                return moves.get(0);
        }

        long moveElapsedMs = System.currentTimeMillis() - moveStartMs;

        totalNodes += nodes;
        totalCutoffs += cutoffs;
        totalPrunedMoves += prunedMoves;
        totalSearchTimeMs += moveElapsedMs;
        totalMovesPlayed++;

        if (DEBUG) System.out.println("[MOVE DONE] time=" + moveElapsedMs + "ms bestScore=" + bestScore + " bestMove=" + bestMove);
        
        return bestMove;
    }

    private SearchResult alphaBetaRoot(Board root, int depth, int myColor, long deadline) {
        ArrayList<Move> moves = root.getAllPossibleMoves(myColor);
        
        if (moves.isEmpty()) 
            return new SearchResult(null, -INF + 1, false);
        
        moves.sort((a, b) -> Integer.compare(
            quickMoveScore(root, b, myColor),
            quickMoveScore(root, a, myColor)
        ));

        int alpha = -INF;
        int beta = INF;

        Move bestMove = null;
        int bestScore = -INF;

        for (Move m : moves) {
            if (System.nanoTime() >= deadline) 
                return new SearchResult(bestMove, bestScore, true);

            Board child = new Board(root);
            child.makeMove(m);

            int score = alphaBeta(child, depth - 1, alpha, beta, false, myColor, deadline);

            // Eval Info
            System.out.println(
                "[Root Eval] depth=" + depth +
                " move=" + m +
                " score=" + score
            );

            if (score > bestScore) {
                bestScore = score;
                bestMove = m;
            }
            alpha = Math.max(alpha, bestScore);

            // System.out.println("[Stats] prunedBranches=" + prunedBranches);
        }

        return new SearchResult(bestMove, bestScore, false);
    }

    private int alphaBeta(Board node, int depth, int alpha, int beta, boolean maximizing, int myColor, long deadline) {
        nodes++;
        if (System.nanoTime() >= deadline) return 0; 

        int toMove = maximizing ? myColor : opponent(myColor);

        ArrayList<Move> moves = node.getAllPossibleMoves(toMove);
        
        // Move ordering:
        // 1. Put killer moves first if present at this depth
        for (int k = 0; k < 2; k++) {
            Move km = killerMoves[depth][k];
            if (km != null && moves.contains(km)) {
                moves.remove(km);
                moves.add(0, km);
            }
        }
        if (moves.isEmpty()) 
            return maximizing ? (-INF + 10) : (INF - 10);
        
        if (depth == 0) 
            return Evaluator.evaluate(node, myColor);
        

        moves.sort((a, b) -> {
            int sa = quickMoveScore(node, a, toMove) + getHistoryScore(a);
            int sb = quickMoveScore(node, b, toMove) + getHistoryScore(b);
            return maximizing ? Integer.compare(sb, sa) : Integer.compare(sa, sb);
        });

        if (maximizing) {
            int value = -INF;
            for (Move m : moves) {
                if (System.nanoTime() >= deadline) break;
                
                Board child = new Board(node);
                child.makeMove(m);
                value = Math.max(value, alphaBeta(child, depth - 1, alpha, beta, false, myColor, deadline));
                alpha = Math.max(alpha, value);
                
                if (alpha >= beta) {
                    cutoffs++;
                    prunedMoves += (moves.size() - moves.indexOf(m) - 1);
                     // 1. Update history heuristic
                    incrementHistoryScore(m, depth);

                    // 2. Update killer moves if not already present
                    if (killerMoves[depth][0] == null || !killerMoves[depth][0].equals(m)) {
                        killerMoves[depth][1] = killerMoves[depth][0];
                        killerMoves[depth][0] = m;
                    }
                    break;
                }
            }
            return value;
        } else {
            int value = INF;
            for (Move m : moves) {
                if (System.nanoTime() >= deadline) break;
                
                Board child = new Board(node);
                child.makeMove(m);
                value = Math.min(value, alphaBeta(child, depth - 1, alpha, beta, true, myColor, deadline));
                beta = Math.min(beta, value);
                
                if (alpha >= beta) {
                    cutoffs++;
                    prunedMoves += (moves.size() - moves.indexOf(m) - 1);
                    break;
                }
            }
            return value;
        }
    }
    private int getHistoryScore(Move m) {
        String key = m.queenStart.row + "," + m.queenStart.col + "->" + m.queenEnd.row + "," + m.queenEnd.col;
        return historyTable.getOrDefault(key, 0);
    }
    private void incrementHistoryScore(Move m, int depth) {
        String key = m.queenStart.row + "," + m.queenStart.col + "->" + m.queenEnd.row + "," + m.queenEnd.col;
        int prev = historyTable.getOrDefault(key, 0);
        historyTable.put(key, prev + (1 << depth)); // weight by depth
    }
    private int quickMoveScore(Board b, Move m, int moverColor) {
        Board child = new Board(b);
        child.makeMove(m);

        int myMob = child.getAllPossibleMoves(moverColor).size();
        int opMob = child.getAllPossibleMoves(opponent(moverColor)).size();
        return myMob - opMob;
    }

    private int opponent(int color) {
        return (color == Board.black) ? Board.white : Board.black;
    }

    public static class SearchResult {
        Move bestMove;
        int score;
        boolean timedOut;

        public SearchResult(Move bestMove, int score, boolean timedOut) {
            this.bestMove = bestMove;
            this.score = score;
            this.timedOut = timedOut;
        }
    }

    public void printGameStats() {
        System.out.println("\n========== GAME SEARCH STATS ==========");
        System.out.println("Moves played (by this agent): " + totalMovesPlayed);
        System.out.println("Total search time (ms): " + totalSearchTimeMs);
        System.out.println("Total nodes: " + totalNodes);
        System.out.println("Total cutoffs: " + totalCutoffs);
        System.out.println("Total pruned sibling moves: " + totalPrunedMoves);

        long avgNodes = (totalMovesPlayed == 0) ? 0 : totalNodes / totalMovesPlayed;
        long avgTime = (totalMovesPlayed == 0) ? 0 : totalSearchTimeMs / totalMovesPlayed;
        long nps = (totalSearchTimeMs == 0) ? 0 : (totalNodes * 1000L) / totalSearchTimeMs;

        System.out.println("Avg nodes/move: " + avgNodes);
        System.out.println("Avg time/move (ms): " + avgTime);
        System.out.println("Avg nodes/sec: " + nps);
        System.out.println("======================================\n");
    }

}