package ubc.cosc322;

public class Evaluator {
    private static AmazonsEvaluation evaluator = null;

    public static void initialize() {
        evaluator = new AmazonsEvaluation(Board.rows, Board.cols);
    }


    public static int evaluate(Board board, int myColor) {
        if (evaluator == null) {
            // Auto-initialize if not done yet
            evaluator = new AmazonsEvaluation(Board.rows, Board.cols);
        }

        // Convert Board to 2D int array
        int[][] boardArray = boardToArray(board);

        // Determine whose turn it is
        boolean isWhiteToMove = !board.isBlackTurn(); // if not black's turn, it's white's turn

        // Get evaluation (returns double, positive favors White)
        double eval = evaluator.evaluate(boardArray, isWhiteToMove);

        // If myColor is Black, negate the score
        if (myColor == Board.black) {
            eval = -eval;
        }

        return (int)(eval * 100);
    }

    private static int[][] boardToArray(Board board) {
        int[][] array = new int[Board.rows][Board.cols];

        for (int r = 0; r < Board.rows; r++) {
            for (int c = 0; c < Board.cols; c++) {
                int cellValue = board.getCell(r, c);

                // Map your board values to AmazonsEvaluation constants
                if (cellValue == Board.arrow) {
                    array[r][c] = AmazonsEvaluation.ARROW;
                } else if (cellValue == Board.white) {
                    array[r][c] = AmazonsEvaluation.WHITE_AMAZON;
                } else if (cellValue == Board.black) {
                    array[r][c] = AmazonsEvaluation.BLACK_AMAZON;
                } else {
                    array[r][c] = AmazonsEvaluation.EMPTY;
                }
            }
        }
        return array;
    }
}