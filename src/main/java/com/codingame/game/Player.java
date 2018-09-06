package com.codingame.game;
import com.codingame.game.InputActions.AbstractAction;
import com.codingame.game.InputActions.InvalidAction;
import com.codingame.game.InputActions.MoveAction;
import com.codingame.game.InputActions.PushAction;
import com.codingame.game.Utils.Constants;
import com.codingame.game.Utils.Vector2;
import com.codingame.gameengine.core.AbstractMultiplayerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Player extends AbstractMultiplayerPlayer {
    private Vector2 pos;

    public AbstractAction getAction() throws TimeoutException, InvalidAction {
        try {
            String playerAction = this.getOutputs().get(0);
            Matcher matchPush = Constants.PLAYER_INPUT_PUSH_PATTERN.matcher(playerAction);
            Matcher matchMove = Constants.PLAYER_INPUT_MOVE_PATTERN.matcher(playerAction);
            if (matchPush.matches()) {
                return new PushAction(Integer.parseInt(matchPush.group("id")),
                        Constants.Direction.valueOf(matchPush.group("direction")));
            } else if (matchMove.matches()) {
                Matcher tokensMatcher = Constants.PLAYER_INPUT_MOVE_TOKENS_PATTERN.matcher(playerAction);
                MoveAction moveAction = new MoveAction();
                while (tokensMatcher.find()) {
                    moveAction.addAction(Integer.parseInt(tokensMatcher.group("amount")),
                            Constants.Direction.valueOf(tokensMatcher.group("direction")));
                }
                return moveAction;
            } else {
                throw new InvalidAction("Invalid output.");
            }
        } catch (TimeoutException | InvalidAction e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidAction("Invalid output.");
        }
    }

    public void setAgentPosition(Vector2 pos) {
        this.pos = pos;
    }

    public Vector2 getAgentPosition() {
        return this.pos;
    }

    public void addItemCard(Item item) {
        this.cards.add(item);
    }

    public List<Item> getCards() {
        return this.cards;
    }

    public Item getTopCard() {
        return this.cards.get(this.cards.size()-1);
    }

    public void removeCard(Item item) {
        this.cards.remove(item);
    }

    @Override
    public int getExpectedOutputLines() {
        // Returns the number of expected lines of outputs for a player
        return 1;
    }
}
