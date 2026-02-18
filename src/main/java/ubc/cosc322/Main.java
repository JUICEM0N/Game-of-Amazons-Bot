
package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    private GameClient gameClient; 
    private BaseGameGUI gamegui;
	
    private String userName;
    private String passwd;
    
    private boolean isBlack;
    private ArrayList<Integer> boardState; // eventually want to build a state class instead
 
	
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
                this.boardState = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
                gamegui.setGameState(this.boardState);
                System.out.println("Game state initialized.");
                break;

            case GameMessage.GAME_ACTION_START:
                //signals the start of the game and indicates if we are Black or White
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
                 //Update the GUI with the opponent's move
                gamegui.updateGameState(msgDetails);
                
                //TODO: Update internal boardState here
                
                //our turn, make a move
                makeMove();
                break;
                
            default:
                break;
        }

    	return true;   	
    }
    
     
     
    private void makeMove() {
        System.out.println("Our move...");
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
