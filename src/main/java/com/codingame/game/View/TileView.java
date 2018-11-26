package com.codingame.game.View;

import com.codingame.game.Model.Item;
import com.codingame.game.Model.StateUpdates.RemoveItemUpdate;
import com.codingame.game.Model.StateUpdates.ShowFrameUpdate;
import com.codingame.game.Model.TileModel;
import com.codingame.game.Utils.Constants;
import com.codingame.game.Utils.Vector2;
import com.codingame.gameengine.module.entities.*;
import com.codingame.view.tooltip.TooltipModule;

import java.util.HashMap;
import java.util.Observable;

public class TileView extends MovingView {
    private TooltipModule tooltipModule;

    private Group group;

    private Sprite decors;
    private Sprite directions;
    private Sprite background;
    private Sprite frame;
    private Sprite item;

    private boolean showFrame = false;

    private Item tileItem;
    private TileModel model;

    public TileView(GraphicEntityModule entityModule, TooltipModule tooltipModule, TileModel tile) {
        super(entityModule);
        this.tooltipModule = tooltipModule;
        this.model = tile;
        this.tileItem = model.getItem();
        tile.addObserver(this);

        createTileView();
        tooltipModule.registerEntity(group.getId(), new HashMap<>());
    }

    private void createTileView() {
        decors = entityModule.createSprite()
                .setImage(String.format("decors_%s.png", model.getPattern()))
                .setBaseWidth(Constants.TILE_SIZE)
                .setBaseHeight(Constants.TILE_SIZE)
                .setAnchor(0.5)
                .setZIndex(1);
        directions = entityModule.createSprite()
                .setImage(String.format("paths_%s.png", model.getPattern()))
                .setBaseWidth(Constants.TILE_SIZE)
                .setBaseHeight(Constants.TILE_SIZE)
                .setAnchor(0.5)
                .setZIndex(2);
        frame = entityModule.createSprite()
                .setImage("frame.png")
                .setBaseWidth(Constants.TILE_SIZE)
                .setBaseHeight(Constants.TILE_SIZE)
                .setAnchor(0.5)
                .setZIndex(2)
                .setVisible(false);
        background = entityModule.createSprite()
                .setImage("tile_background.png")
                .setBaseWidth(Constants.TILE_SIZE)
                .setBaseHeight(Constants.TILE_SIZE)
                .setAlpha(0.3)
                .setAnchor(0.5)
                .setZIndex(0);
        group = entityModule.createGroup()
                .setScale(1);
        group.add(decors, directions, frame, background);

        addItem();
    }

    private void addItem() {
        if (tileItem != null) {
            String itemsPath = "items" + System.getProperty("file.separator") + "item_%s_%d.png";
            String spritePath = String.format(itemsPath, tileItem.getName(), tileItem.getPlayerId());
            item = entityModule.createSprite()
                .setImage(spritePath)
                .setAnchor(0.5)
                .setZIndex(2);
            group.add(item);
        }
    }

    private void removeItem() {
        this.item.setVisible(false);
        group.remove(this.item);
    }

    public void updateView(){
        if (model.getPlayerId() != null) {
            //player tile is always on top of everything
            group.setZIndex(4);
            //always show frame for player's tiles
            frame.setVisible(true);
            entityModule.commitEntityState(0, frame);

            Vector2 pos = Constants.TILE_POSITIONS.get(model.getPlayerId());
            group.setX(pos.getX()).setY(pos.getY());
        } else {
            //only show frames for moving map tiles
            if (showFrame) {
                frame.setVisible(true);
                entityModule.commitEntityState(0, frame);
                //get pushed tile zIndex back
                group.setZIndex(1);
                frame.setVisible(false);
                showFrame = false;
            }
            setMapPos(group, model.getPos());
        }
        tooltipModule.updateExtraTooltipText(group, model.getPos().toTooltip());
    }

    public void update(Observable observable, Object update) {
        super.update(model, update);
        if (update instanceof RemoveItemUpdate)
            removeItem();
        else if (update instanceof ShowFrameUpdate)
            showFrame = true;
    }

    public Entity getEntity() {
        return group;
    }
}
