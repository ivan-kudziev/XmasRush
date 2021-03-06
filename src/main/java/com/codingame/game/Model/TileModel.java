package com.codingame.game.Model;

import com.codingame.game.Model.StateUpdates.PoppedUpdate;
import com.codingame.game.Model.StateUpdates.PushedUpdate;
import com.codingame.game.Model.StateUpdates.RemoveItemUpdate;
import com.codingame.game.Model.StateUpdates.ShowFrameUpdate;
import com.codingame.game.Utils.Constants;
import com.codingame.game.Utils.Constants.Direction;
import com.codingame.game.Utils.Vector2;

public class TileModel extends MovingModel{
    private static final int PATTERN_LENGTH = 4;

    private String pattern;
    private Item item;

    private Integer playerId;

    //pattern represents path directions (up, right, down, left) as 4 binary digits (1 for path, 0 for no path)
    private void checkRep() {
        assert pattern != null && pattern.matches("[0-1]{" + PATTERN_LENGTH + "}");
    }

    public TileModel(String pattern, Vector2 pos) {
        super(pos);
        this.pattern = pattern;
        checkRep();
    }

    //Player-related methods
    public void setPlayerId(Integer id) {
        playerId = id;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    //Item methods
    public void setItem(Item item) {
        assert this.item == null;
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public void removeItem() {
        assert item != null;
        item = null;
        updateState(new RemoveItemUpdate());
     }

    public boolean hasItem() {
        return item != null;
    }

    //Pattern methods
    public String getPattern() {
        return pattern;
    }

    public boolean hasDirection(Constants.Direction direction) {
         return pattern.charAt(direction.asValue()) == '1';
     }

    public void rotate(int numTimes) {
        int num = numTimes % PATTERN_LENGTH;
        pattern = pattern.substring(PATTERN_LENGTH - num)
                + pattern.substring(0, PATTERN_LENGTH - num);
    }

    //check if it's a 3- or 4-way tile
    public boolean isThreeWayPlus() {
        return this.getPattern().chars().filter(ch -> ch == '1').count() >= 3;
    }

    //Player input methods
    public String patternToString() {
        return pattern;
    }

    public String tileToString() {
        return item != null ? item.getName() + " " + getTilePos().toString() + " " + item.getPlayerId() : "";
    }

    public String opponentTileToString() {
        assert item != null;
        return item.getName() + " " + getOpponentTilePos().toString() + " " + item.getOpponentId();
    }

    private Vector2 getOpponentTilePos() {
        if (playerId != null)
            return Constants.TILE_MODEL_POSITIONS.get(1 - playerId);
        return this.getPos();
    }
    
    private Vector2 getTilePos() {
        if (playerId != null)
            return Constants.TILE_MODEL_POSITIONS.get(playerId);
        return this.getPos();
    }

    @Override
    public void move(Vector2 pos) {
        super.setPos(pos);
        updateState(new ShowFrameUpdate());
    }
    public void push(Direction direction) {
        updateState(new PushedUpdate(direction));
    }
    public void pop(Direction direction) {
        updateState(new PoppedUpdate(direction));
    }
}
