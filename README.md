# Game-of-Amazons-Bot
 
A competitive AI bot for the [Game of the Amazons](https://en.wikipedia.org/wiki/Game_of_the_Amazons), built in Java as part of COSC 322 (Introduction to Artificial Intelligence) at UBC. The bot connects to a remote game server and plays against other bots or human opponents in real time using iterative-deepening alpha-beta search with a multi-component positional heuristic.
 
---
 
## About the Game
 
The Game of the Amazons is a two-player territory strategy game played on a 10x10 board. Each player controls four amazons (queens). On each turn a player must:
 
1. Move one of their amazons along any unobstructed horizontal, vertical, or diagonal line (like a chess queen).
2. From the amazon's new position, shoot an arrow along any unobstructed line. The arrow permanently blocks that square.
 
The first player unable to make a move loses. The game combines elements of chess-like movement with Go-like territory control; as arrows accumulate, the board partitions into regions and the player who controls more reachable squares wins.
 
---
 
## Architecture Overview
 
The project follows a straightforward pipeline:
 
1. **Main** -- Handles server connection, GUI integration, message dispatch, and turn management.
2. **Board** -- Maintains the 10x10 game state, generates legal moves, and supports make/undo with Zobrist hashing.
3. **SearchEngine** -- Iterative-deepening alpha-beta search with time control.
4. **Evaluator / AmazonsEvaluation** -- Multi-component positional evaluation combining territory, quality, and mobility metrics.
5. **TranspositionTable** -- Zobrist-keyed cache for previously evaluated positions.
6. **Move** -- Data class representing a single (queen-start, queen-end, arrow) triple.
 
---
 
## Search Engine
 
The bot uses **iterative-deepening alpha-beta search** with a configurable time limit (default 10 seconds per move). Key features:
 
- **Alpha-beta pruning** with full-width search. Cutoff and pruned-move counters are tracked for diagnostics.
- **Iterative deepening** from depth 1 up to a maximum of 50. Search terminates early when the time deadline is reached, returning the best move found so far.
- **Move ordering** at every node: moves are scored by a quick mobility heuristic (difference in legal move count after the move) and sorted accordingly. At maximizing nodes moves are sorted highest-first; at minimizing nodes, lowest-first.
- **Transposition table lookups**: before expanding a node the engine checks the TT for an entry at equal or greater depth. Exact entries are returned immediately; lower/upper bounds tighten the alpha-beta window.
- **TT move promotion**: if the transposition table contains a best move for the current position, that move is promoted to the front of the move list before sorting.
 
After each move the engine accumulates lifetime statistics (total nodes, cutoffs, pruned moves, TT hits, average nodes/sec) that can be printed at game end.
 
---
 
## Evaluation Function
 
The evaluation lives in `AmazonsEvaluation` and returns a score from White's perspective. It combines five components with dynamically weighted blending:
 
### Distance Metrics
 
Two distance functions are computed for every empty square on the board:
 
- **Queen distance (d1)** -- Minimum number of queen-style moves (sliding along unobstructed lines) from any friendly amazon to that square, computed via BFS.
- **King distance (d2)** -- Minimum number of king-style moves (one step in any of 8 directions, through unobstructed squares) from any friendly amazon to that square, also via BFS.
 
### Components
 
1. **Territorial evaluation t1** -- For each empty square, compares the queen distance from the nearest white amazon vs. the nearest black amazon. Squares closer to white score +1 (minus a small kappa offset); squares closer to black score -1 (plus kappa). Unreachable squares by one side count as fully owned by the other.
 
2. **Territorial evaluation t2** -- Same logic as t1 but using king distance instead of queen distance.
 
3. **Quality evaluation c1** -- Sums `2^(-d1)` for white amazons and subtracts `2^(-d1)` for black amazons across all empty squares. This gives higher weight to squares that are very close to a player's amazons, capturing "quality" of territorial control rather than just binary ownership. The result is doubled.
 
4. **Quality evaluation c2** -- For each empty square reachable by both sides, computes a clamped normalized difference `(d2_black - d2_white) / 6`, bounded to [-1, 1]. This is a softer version of t2 that distinguishes between small and large distance advantages.
 
5. **Mobility penalty** -- For each amazon, measures how enclosed it is by examining the number of empty king-adjacent squares around it and its neighbors (the alpha value). Amazons with low alpha receive a penalty scaled by the phase weight, discouraging moves that trap your own pieces.
 
### Phase-Aware Weighting
 
A phase weight `w` is computed by summing `2^(-|d1_white - d1_black|)` across all empty squares. When both players' nearest amazons are similar distances from a square (contested territory), the contribution to `w` is high; when one side clearly owns a square, the contribution is low. A higher `w` indicates an earlier, more contested game phase.
 
The four evaluation components are blended using dynamic weights derived from `w`:
 
- Early game (high `w`): heavier weight on quality evaluations (c1, c2) and king-distance territory (t2).
- Late game (low `w`): heavier weight on queen-distance territory (t1), which better reflects actual reachable territory in partitioned positions.
 
A small kappa bonus (+0.1) is added for the side to move, reflecting the first-move advantage.
 
---
 
## Transposition Table
 
The transposition table is a `ConcurrentHashMap<Long, TTEntry>` keyed by Zobrist hash values. Each entry stores:
 
- The Zobrist key (for verification)
- Search depth at which the entry was computed
- The evaluation value
- The bound type: EXACT, LOWER_BOUND, or UPPER_BOUND
- The best move found at that position
 
Replacement policy: a new entry replaces an existing one only if its search depth is greater than or equal to the stored depth. The table has a configurable maximum size (default 1,000,000 entries).
 
---
 
## Board Representation
 
- The board is stored as a flat `byte[100]` array (10x10), with values 0 (empty), 1 (black), 2 (white), 3 (arrow).
- Queen positions are tracked in an `int[8]` array (indices 0-3 for black, 4-7 for white) for fast iteration.
- **Zobrist hashing** is used for incremental hash updates on make/undo. A deterministic PRNG seeds the Zobrist table for reproducibility. A separate Zobrist key is XORed on each turn change to distinguish positions with different sides to move.
- Move generation uses a slider-move approach: for each queen, iterate along all 8 directions until hitting a boundary or occupied square. For each destination, temporarily place the queen there and generate arrow shots using the same slider logic.
 
---
 
## Project Structure
 
```
Game-of-Amazons-Bot/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    └── main/
        └── java/
            └── ubc/
                └── cosc322/
                    ├── Main.java                 # Entry point, server connection, game loop
                    ├── Board.java                # Board state, move generation, Zobrist hashing
                    ├── Move.java                 # Move representation and server serialization
                    ├── SearchEngine.java         # Iterative-deepening alpha-beta search
                    ├── Evaluator.java            # Bridge between Board and AmazonsEvaluation
                    ├── AmazonsEvaluation.java    # Heuristic evaluation function
                    └── TranspositionTable.java   # Zobrist-keyed position cache
```
 
---
 
## Prerequisites
 
- **Java 21** or later
- **Maven** for dependency management and building
- Access to the COSC 322 game server (the bot depends on the `ygraph-ai-smartfox-client` library hosted at UBC)
 
---
 
## Building and Running
 
```bash
# Clone the repository
git clone https://github.com/JUICEM0N/Game-of-Amazons-Bot.git
cd Game-of-Amazons-Bot
 
# Build with Maven
mvn clean compile
 
# Run the bot (connects to the game server with a random username)
mvn exec:java -Dexec.mainClass="ubc.cosc322.Main"
```
 
The bot will log in to the game server, join a room, and begin playing automatically when a game starts. A GUI window displays the current board state.
 
---
 
## Configuration
 
Key constants that can be adjusted in the source:
 
| Constant | Location | Default | Description |
|---|---|---|---|
| `TIME_LIMIT` | `Main.java` | 10000 ms | Time allowed per move |
| `MAX_DEPTH` | `SearchEngine.java` | 50 | Maximum iterative-deepening depth |
| `TT_MAX_ENTRIES` | `SearchEngine.java` | 1,000,000 | Transposition table capacity |
| `DEBUG` | `SearchEngine.java` | false | Enable per-move search diagnostics |
| `KAPPA` | `AmazonsEvaluation.java` | 0.1 | First-move advantage bonus |
 
---
 
## Branches
 
| Branch | Description |
|---|---|
| `main` | Current stable version with full evaluation and transposition tables |
| `heurstic-function2` | Alternate heuristic function iteration |
| `heuristic-function` | Earlier heuristic development |
| `Heuristic-DFS` | DFS-based search with heuristic evaluation |
| `addHistoryKiller` | Experiments with history/killer move heuristics |
| `feature/transposition-tables` | Initial transposition table implementation |
| `feature/transposition-table-fix` | Bug fixes for transposition table |
| `feature/optimization-parallel-search` | Parallel search experiments |
