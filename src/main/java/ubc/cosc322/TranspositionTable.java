package ubc.cosc322;

import java.util.concurrent.ConcurrentHashMap;

public class TranspositionTable {

    public enum BoundType {
        EXACT,
        LOWER_BOUND,
        UPPER_BOUND
    }

    public static class TTEntry {
        private final long key;
        private final int depth;
        private final int value;
        private final BoundType boundType;
        private final Move bestMove;

        public TTEntry(long key, int depth, int value, BoundType boundType, Move bestMove) {
            this.key = key;
            this.depth = depth;
            this.value = value;
            this.boundType = boundType;
            this.bestMove = bestMove;
        }

        public long getKey() {
            return key;
        }

        public int getDepth() {
            return depth;
        }

        public int getValue() {
            return value;
        }

        public BoundType getBoundType() {
            return boundType;
        }

        public Move getBestMove() {
            return bestMove;
        }
    }

    private final int maxEntries;
    private final ConcurrentHashMap<Long, TTEntry> map;

    public TranspositionTable(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.map = new ConcurrentHashMap<>(this.maxEntries);
    }

    public TTEntry get(long key) {
        return map.get(key);
    }

    public void put(long key, TTEntry candidate) {
        TTEntry existing = map.get(key);
        if (existing == null || candidate.getDepth() >= existing.getDepth()) {
            map.put(key, candidate);
        }
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
    }
}
