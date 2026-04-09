
package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;


/**
 * An example illustrating how to implement a GamePlayer
 * @author Yong Gao (yong.gao@ubc.ca)
 * Jan 5, 2021
 *
 */
public class Main extends GamePlayer {

    private static final int TIME_LIMIT = 2900; // In ms

    private GameClient gameClient; 
    private BaseGameGUI gamegui;
	
    private String userName;
    private String passwd;
    
    private boolean isBlack;
    private Board board; 

    /**
     * The main method
     * @param args for name and passwd (current, any string would work)
     */
    public static void main(String[] args) {
    	String userName = "User-" + (new Random()).nextInt(1000);
    	String passwd = "";
    	
    	Main player = new Main(userName, passwd);

		if(player.getGameGUI() == null) {
            player.Go();
    	}
    	else {
    		BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                	player.Go();
                }
            });
    	}
    }
	
    /**
     * Any name and passwd 
     * @param userName
      * @param passwd
     */
    public Main(String userName, String passwd) {
    	this.userName = userName;
    	this.passwd = passwd;

    	//To make a GUI-based player, create an instance of BaseGameGUI
    	//and implement the method getGameGUI() accordingly
    	this.gamegui = new BaseGameGUI(this);
    }

    @Override
    public void onLogin() {
        System.out.println("Login Successful!");
        userName = gameClient.getUserName();
    
    	//join room
		if (gamegui != null) {
			gamegui.setRoomInformation(gameClient.getRoomList());
		}
    }

	@SuppressWarnings("unchecked")
    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
    	//called by the GameClient when it receives a game-related message
        System.out.println("Message type: " + messageType + "\nDetails: " + msgDetails.toString());
    
        switch (messageType) {
                case GameMessage.GAME_STATE_BOARD:
                    //initial board state sent when joining a game
                    ArrayList<Integer> gameState = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
                    this.board = new Board(gameState);
                    gamegui.setGameState(gameState);
                    System.out.println("initialized...");
                    break;

                case GameMessage.GAME_ACTION_START:

                    String blackPlayerName = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);
                    String whitePlayerName = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
                    
                    if (this.userName.equals(blackPlayerName)) {
                        this.isBlack = true;
                        makeMove(); 
                    } else if (this.userName.equals(whitePlayerName)) {
                        this.isBlack = false;
                        System.out.println("Waiting for Black to move...");
                    }
                    break;

                case GameMessage.GAME_ACTION_MOVE:
                    updateGUI(msgDetails);
                    
                    Move opponentMove = new Move(msgDetails);
                    board.makeMove(opponentMove);              
                    makeMove();
                    break;
                    
                default:
                    break;
            }

        return true;   	
    }

    private final SearchEngine engine = new SearchEngine();

    private void makeMove() {
        int myColor = this.isBlack ? Board.black : Board.white;
        Move selectedMove = engine.chooseMove(board, myColor, TIME_LIMIT);

        if (selectedMove == null) {
            String me = userName;
            String winner = isBlack ? "White" : "Black";
            String loser  = isBlack ? "Black" : "White";

            System.out.println("GAME OVER: " + loser + " (" + me + ") has no legal moves, rip bozo");
            System.out.println("WINNER: " + winner + " player!");

            engine.printGameStats();
            return;
        }

        System.out.println("Chosen: " + selectedMove);

        gameClient.sendMoveMessage(selectedMove.toServerMap());
        board.makeMove(selectedMove);
        updateGUI(selectedMove.toServerMap());
    }

    @SuppressWarnings("unchecked")
    private void updateGUI(Map<String, Object> msgDetails) {
        ArrayList<Integer> qCurr = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
        ArrayList<Integer> qNext = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
        ArrayList<Integer> arrow = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);
        gamegui.updateGameState(qCurr, qNext, arrow);
    }

    @Override
    public String userName() {
        return userName;
    }

	@Override
	public GameClient getGameClient() {
		return this.gameClient;
	}

	@Override
	public BaseGameGUI getGameGUI() {
		return this.gamegui;
	}

	@Override
	public void connect() {
        gameClient = new GameClient(userName, passwd, this);			
	}

}