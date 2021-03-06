package com.codingame.game.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.codingame.game.Utils.Constants;
import com.codingame.game.Utils.Vector2;

public class PlayerModelTest {

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false;
    }

    @Test(expected=AssertionError.class)
    public void testSetCardsIncorrectId() throws AssertionError {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Item item1 = new Item("BOOK", id);
        Item item2 = new Item("POTION", 0);
        List<Item> itemList = new ArrayList<>();
        itemList.addAll(Arrays.asList(item1, item2));
        player.setCards(itemList);
    }

    @Test(expected=AssertionError.class)
    public void testSetCardsRepeatedItems() throws AssertionError {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Item item1 = new Item("BOOK", id);
        Item item2 = new Item("BOOK", id);
        List<Item> itemList = new ArrayList<>();
        itemList.addAll(Arrays.asList(item1, item2));
        player.setCards(itemList);
    }

    @Test
    public void testConstructor() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        assertEquals(id, player.id);
        assertEquals(Constants.PLAYER_POSITIONS.get(id), player.getPos());
    }

    @Test
    public void testTileMethods() {
        int id = 0;
        PlayerModel player = new PlayerModel(id);
        Vector2 pos = new Vector2(1, 2);
        TileModel tile1 = new TileModel("1111", pos);
        TileModel tile2 = new TileModel("1001", pos);
        assertNull(tile1.getPlayerId());
        player.setTile(tile1);
        assertTrue(tile1.getPlayerId() == id);
        assertEquals(tile1, player.getTile());
        assertEquals(Constants.TILE_MODEL_POSITIONS.get(id), player.getTile().getPos());
        player.setTile(tile2);
        assertNull(tile1.getPlayerId());
    }

    @Test
    public void testSetGetCards() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Item item1 = new Item("BOOK", id);
        Item item2 = new Item("POTION", id);
        List<Item> itemList = new ArrayList<>();
        itemList.addAll(Arrays.asList(item1, item2));
        player.setCards(itemList);
        assertEquals(2, player.getCards().size());
        assertEquals(item1, player.getCards().get(0).getItem());
        assertEquals(item2, player.getCards().get(1).getItem());
        List<CardModel> cards = player.getCards();
        cards = new ArrayList<>();
        assertFalse(player.getCards().isEmpty());
    }

    @Test
    public void testRemoveItemCard() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Item item1 = new Item("BOOK", id);
        Item item2 = new Item("POTION", id);
        int numVisibleCards = 1;
        List<Item> itemList = Arrays.asList(item1, item2);
        player.setCards(itemList);
        assertFalse(player.removeCard(item1));
        assertFalse(player.removeCard(item2));
        player.setNumVisibleCards(numVisibleCards);
        player.flipCards();
        assertTrue(player.removeCard(item2));
        assertFalse(player.removeCard(item2));
        player.flipCards();
        assertTrue(player.removeCard(item1));
        assertFalse(player.removeCard(item1));
    }

    @Test
    public void testSetNumVisibleCards() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Item item1 = new Item("BOOK", id);
        Item item2 = new Item("POTION", id);
        Item item3 = new Item("ARROW", id);
        Item item4 = new Item("FISH", id);
        int numVisibleCards1 = 1;
        int numVisibleCards2 = 3;
        int numVisibleCards3 = 4;
        List<Item> itemList = Arrays.asList(item1, item2, item3, item4);
        player.setCards(itemList);
        player.setNumVisibleCards(numVisibleCards1);
        player.flipCards();
        assertEquals(1, player.getNumQuestCards());
        assertEquals(3, player.getNumDeckCards());
        player.setNumVisibleCards(numVisibleCards2);
        player.flipCards();
        assertEquals(3, player.getNumQuestCards());
        assertEquals(1, player.getNumDeckCards());
        player.setNumVisibleCards(numVisibleCards3);
        player.flipCards();
        assertEquals(3, player.getNumQuestCards());
        assertEquals(1, player.getNumDeckCards());
    }

    @Test
    public void testFlipCards() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Item item1 = new Item("BOOK", id);
        Item item2 = new Item("POTION", id);
        Item item3 = new Item("ARROW", id);
        Item item4 = new Item("FISH", id);
        int numVisibleCards = 3;
        List<Item> itemList = Arrays.asList(item1, item2, item3, item4);
        player.flipCards(); // shall not produce errors
        assertEquals(0, player.getNumQuestCards());
        assertEquals(0, player.getNumDeckCards());
        player.setCards(itemList);
        player.flipCards();
        assertEquals(0, player.getNumQuestCards());
        assertEquals(4, player.getNumDeckCards());
        player.setNumVisibleCards(numVisibleCards);
        player.flipCards();
        assertEquals(3, player.getNumQuestCards());
        assertEquals(1, player.getNumDeckCards());
        player.flipCards();
        assertEquals(3, player.getNumQuestCards());
        assertEquals(1, player.getNumDeckCards());
    }

    @Test
    public void testPlayerToString() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        Vector2 pos = new Vector2(1, 2);
        TileModel tile = new TileModel("1111", pos);
        player.setTile(tile);
        Item item1 = new Item("BOOK", 1);
        Item item2 = new Item("POTION", 1);
        List<Item> itemList = new ArrayList<>();
        itemList.addAll(Arrays.asList(item1, item2));
        player.setCards(itemList);
        assertEquals("2 6 6 1111", player.playerToString());
    }

    @Test
    public void testMoveToPos() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        assertEquals(new Vector2(6, 6), player.getPos());
        Vector2 pos = new Vector2(6, 5);
        player.move(pos);
        assertEquals(pos, player.getPos());
    }

    @Test
    public void testMoveInDirection() {
        int id = 1;
        PlayerModel player = new PlayerModel(id);
        player.move(Constants.Direction.UP);
        assertEquals(new Vector2(6, 5), player.getPos());
        player.move(Constants.Direction.LEFT);
        assertEquals(new Vector2(5, 5), player.getPos());
        player.move(Constants.Direction.DOWN);
        assertEquals(new Vector2(5, 6), player.getPos());
        player.move(Constants.Direction.RIGHT);
        assertEquals(new Vector2(6, 6), player.getPos());
    }
}