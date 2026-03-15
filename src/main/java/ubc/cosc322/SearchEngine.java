package ubc.cosc322;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchEngine {

    private static final int INF = 1_000_000_000;
    private static final int MAX_DEPTH = 50;
    private static final boolean DEBUG = true;
    private static final int TT_MAX_ENTRIES = 1_000_000;

    private long nodes = 0;
    private long cutoffs = 0;
    private long prunedMoves = 0;
    private long ttHits = 0;

    private long totalNodes = 0;
    private long totalCutoffs = 0;
    private long totalPrunedMoves = 0;
    private long totalSearchTimeMs = 0;
    private long totalMovesPlayed = 0;
    private long totalTtHits = 0;

    private final TranspositionTable tt = new TranspositionTable(TT_MAX_ENTRIES);

    private Move[][] killerMoves = new Move[MAX_DEPTH][2];

    private Map<String, Integer> historyTable = new HashMap<>();

    public Move chooseMove(Board root, int myColor, long timeLimitMs) {
        long deadline = System.nanoTime() + timeLimitMs * 1_000_000L;

        nodes = 0;
        cutoffs = 0;
        prunedMoves = 0;
        ttHits = 0;

        long moveStartMs = System.currentTimeMillis();

        Move bestMove = null;
        int bestScore = -INF;

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            if (System.nanoTime() >= deadline) break;

            long iterStartMs = System.currentTimeMillis();
            SearchResult res = alphaBetaRoot(root, depth, myColor, deadline);
            long iterElapsed = System.currentTimeMillis() - iterStartMs;

            if (DEBUG) {
                long nps = (nodes * 1000L) / (Math.max(1, (System.currentTimeMillis() - moveStartMs)));
                System.out.println("[ID] depth=" + depth +
                        " bestScore=" + res.score +
                        " bestMove=" + res.bestMove +
                        " iterTime=" + iterElapsed + "ms" +
                        " nodes=" + nodes +
                        " cutoffs=" + cutoffs +
                        " prunedMoves=" + prunedMoves +
                        " ttHits=" + ttHits +
                        " nps=" + nps);
            }

            if (res.timedOut) break;
            if (res.bestMove != null) {
                bestMove = res.bestMove;
                bestScore = res.score;
            }
        }

        if (bestMove == null) {
            ArrayList<Move> moves = root.getAllPossibleMoves(myColor);
            if (!moves.isEmpty()) return moves.get(0);
        }

        long moveElapsedMs = System.currentTimeMillis() - moveStartMs;

        totalNodes += nodes;
        totalCutoffs += cutoffs;
        totalPrunedMoves += prunedMoves;
        totalSearchTimeMs += moveElapsedMs;
        totalMovesPlayed++;
        totalTtHits += ttHits;

        if (DEBUG) {
            System.out.println("[Move Done] Best Move: " + bestMove +
                    " | Score: " + bestScore +
                    " | Nodes: " + nodes +
                    " | TT Hits: " + ttHits +
                    " | TT Size: " + tt.size() +
                    " | Time: " + moveElapsedMs + "ms");
        }

        return bestMove;
    }

    private SearchResult alphaBetaRoot(Board root, int depth, int myColor, long deadline) {
        ArrayList<Move> moves = root.getAllPossibleMoves(myColor);
        if (moves.isEmpty()) return new SearchResult(null, -INF + 1, false);

        orderMoves(root, moves, myColor, true);

        int alpha = -INF;
        int beta = INF;
        Move bestMove = null;
        int bestScore = -INF;

        for (int i = 0; i < moves.size(); i++) {
            if (System.nanoTime() >= deadline) return new SearchResult(bestMove, bestScore, true);

            Move m = moves.get(i);
            Board child = new Board(root);
            child.makeMove(m);

            int score = alphaBeta(child, depth - 1, alpha, beta, false, myColor, deadline);

            if (DEBUG) {
                System.out.println("[Root Eval] depth=" + depth +
                        " move=" + m +
                        " score=" + score);
            }

            if (System.nanoTime() >= deadline) return new SearchResult(bestMove, bestScore, true);

            if (score > bestScore) {
                bestScore = score;
                bestMove = m;
            }
            alpha = Math.max(alpha, bestScore);
        }

        tt.put(root.getZHash(), new TranspositionTable.TTEntry(
                root.getZHash(), depth, bestScore, TranspositionTable.BoundType.EXACT, bestMove
        ));

        return new SearchResult(bestMove, bestScore, false);
    }

    private int alphaBeta(Board node, int depth, int alpha, int beta, boolean maximizing, int myColor, long deadline) {
        nodes++;
        if (System.nanoTime() >= deadline) return 0;

        int alphaOriginal = alpha;
        int betaOriginal = beta;

        long key = node.getZHash();
        TranspositionTable.TTEntry entry = tt.get(key);
        if (entry != null && entry.getDepth() >= depth) {
            ttHits++;
            if (entry.getBoundType() == TranspositionTable.BoundType.EXACT) return entry.getValue();
            if (entry.getBoundType() == TranspositionTable.BoundType.LOWER_BOUND) alpha = Math.max(alpha, entry.getValue());
            else if (entry.getBoundType() == TranspositionTable.BoundType.UPPER_BOUND) beta = Math.min(beta, entry.getValue());
            if (alpha >= beta) return entry.getValue();
        }

        int toMove = maximizing ? myColor : opponent(myColor);
        ArrayList<Move> moves = node.getAllPossibleMoves(toMove);

        if (moves.isEmpty()) return maximizing ? (-INF + 10) : (INF - 10);
        if (depth == 0) return Evaluator.evaluate(node, myColor);

        maybePromoteTtMove(moves, entry);
        promoteKillerMoves(moves, depth);
        orderMoves(node, moves, toMove, maximizing);

        int value = maximizing ? -INF : INF;
        Move bestMove = null;

        for (int i = 0; i < moves.size(); i++) {
            if (System.nanoTime() >= deadline) break;

            Move m = moves.get(i);
            Board child = new Board(node);
            child.makeMove(m);

            int childValue = alphaBeta(child, depth - 1, alpha, beta, !maximizing, myColor, deadline);

            if (maximizing) {
                if (childValue > value) {
                    value = childValue;
                    bestMove = m;
                }
                alpha = Math.max(alpha, value);
            } else {
                if (childValue < value) {
                    value = childValue;
                    bestMove = m;
                }
                beta = Math.min(beta, value);
            }

            if (alpha >= beta) {
                cutoffs++;
                prunedMoves += (moves.size() - i - 1);
                incrementHistoryScore(m, depth);

                if (depth < MAX_DEPTH && (killerMoves[depth][0] == null || !killerMoves[depth][0].equals(m))) {
                    killerMoves[depth][1] = killerMoves[depth][0];
                    killerMoves[depth][0] = m;
                }
                break;
            }
        }

        TranspositionTable.BoundType boundType = classifyBound(value, alphaOriginal, betaOriginal);
        tt.put(key, new TranspositionTable.TTEntry(key, depth, value, boundType, bestMove));

        return value;
    }

    private void orderMoves(Board node, ArrayList<Move> moves, int moverColor, boolean maximizing) {
        Map<Move, Integer> scores = moves.parallelStream().collect(
                Collectors.toMap(m -> m, m -> quickMoveScore(node, m, moverColor) + getHistoryScore(m), (a, b) -> a)
        );
        moves.sort((a, b) -> {
            int sa = scores.get(a);
            int sb = scores.get(b);
            return maximizing ? Integer.compare(sb, sa) : Integer.compare(sa, sb);
        });
    }

    private void promoteKillerMoves(ArrayList<Move> moves, int depth) {
        if (depth >= MAX_DEPTH) return;
        for (int k = 0; k < 2; k++) {
            Move km = killerMoves[depth][k];
            if (km != null && moves.contains(km)) {
                moves.remove(km);
                moves.add(0, km);
            }
        }
    }

    private void maybePromoteTtMove(ArrayList<Move> moves, TranspositionTable.TTEntry entry) {
        if (entry == null || entry.getBestMove() == null || moves.isEmpty()) return;
        Move ttMove = entry.getBestMove();
        int idx = moves.indexOf(ttMove);
        if (idx > 0) {
            moves.set(idx, moves.get(0));
            moves.set(0, ttMove);
        }
    }

    private TranspositionTable.BoundType classifyBound(int value, int alphaOriginal, int betaOriginal) {
        if (value <= alphaOriginal) return TranspositionTable.BoundType.UPPER_BOUND;
        if (value >= betaOriginal) return TranspositionTable.BoundType.LOWER_BOUND;
        return TranspositionTable.BoundType.EXACT;
    }

    private int getHistoryScore(Move m) {
        String key = m.queenStart.row+","+m.queenStart.col+"->"+m.queenEnd.row+","+m.queenEnd.col;
        return historyTable.getOrDefault(key, 0);
    }

    private void incrementHistoryScore(Move m, int depth) {
        String key=m.queenStart.row +","+m.queenStart.col+"->"+m.queenEnd.row+","+m.queenEnd.col;
        int prev = historyTable.getOrDefault(key, 0);
        historyTable.put(key, prev+(1 << depth));
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
        System.out.println("Total TT hits: " + totalTtHits);
        System.out.println("Current TT size: " + tt.size());

        long avgNodes = (totalMovesPlayed == 0) ? 0 : totalNodes / totalMovesPlayed;
        long avgTime = (totalMovesPlayed == 0) ? 0 : totalSearchTimeMs / totalMovesPlayed;
        long nps = (totalSearchTimeMs == 0) ? 0 : (totalNodes * 1000L) / totalSearchTimeMs;

        System.out.println("Avg nodes/move: " + avgNodes);
        System.out.println("Avg time/move (ms): " + avgTime);
        System.out.println("Avg nodes/sec: " + nps);
        System.out.println("======================================\n");
    }
}