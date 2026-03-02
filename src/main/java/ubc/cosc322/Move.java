package ubc.cosc322;

import ubc.cosc322.Board.Point;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Move {
    public Point queenStart;
    public Point queenEnd;
    public Point arrowPos;

public Move(Point queenStart, Point queenEnd, Point arrowPos){
    this.queenStart = queenStart;
    this.queenEnd = queenEnd;
    this.arrowPos = arrowPos;
}

private Point toPoint(Object pos){
    ArrayList<Integer> p = (ArrayList<Integer>) pos;
    return new Point(p.get(0)-1, p.get(1)-1);
}

private ArrayList<Integer> convertToList(Point p) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(p.row+1);
        list.add(p.col+1);
        return list;
    }

public Move(Map<String, Object> actionMap){
    this.queenStart = toPoint(actionMap.get(AmazonsGameMessage.QUEEN_POS_CURR));
    this.queenEnd = toPoint(actionMap.get(AmazonsGameMessage.QUEEN_POS_NEXT));
    this.arrowPos = toPoint(actionMap.get(AmazonsGameMessage.ARROW_POS));
}

public Map<String, Object> toServerMap(){
    Map<String, Object> map = new HashMap<>();
    map.put(AmazonsGameMessage.QUEEN_POS_CURR, convertToList(queenStart));
    map.put(AmazonsGameMessage.QUEEN_POS_NEXT, convertToList(queenEnd));
    map.put(AmazonsGameMessage.ARROW_POS, convertToList(arrowPos));
    return map;
}

}
