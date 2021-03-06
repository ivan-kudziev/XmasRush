package com.codingame.game;

import com.codingame.game.InputActions.*;
import com.codingame.game.Model.*;
import com.codingame.game.Utils.Constants;
import com.codingame.game.Utils.Vector2;
import com.codingame.game.View.BoardView;
import com.codingame.game.View.ViewController;
import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.view.endscreen.EndScreenModule;
import com.codingame.view.nicknameshandler.NicknamesHandlerModule;
import com.codingame.view.tooltip.TooltipModule;

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule entityModule;
    @Inject private TooltipModule tooltipModule;
    @Inject private EndScreenModule endScreenModule;
    @Inject private NicknamesHandlerModule nicknamesHandlerModule;
    public static NicknamesHandlerModule nicksModule;
    public static MultiplayerGameManager<Player> myGameManager;
    public static Action.Type turnType;
    private GameBoard gameBoard;

    private ViewController view;
    public static BoardView observer;

    private List<PlayerModel> players = new ArrayList<>();
    private List<Map<Player, PushAction>> pushActions = new ArrayList<>();
    private Map<Player, MoveAction> moveActions = new HashMap<>();
    private boolean passActions = false;

    //Score
    private static final int POINTS_PER_ITEM = 1;
    //Game turns
    public static int gameTurnsLeft;
    //Number of turns required to accommodate the worst case scenario:
    //max move step frames per MOVE turn + (1 row frame + 1 push frame) per PUSH turn
    //+ 1 extra frame to return "Max turns reached!", if required
    private static final int MAX_NUM_TURNS = (Constants.MAX_MOVE_STEPS + 2) * Constants.MAX_GAME_TURNS / 2 + 1;

    //League stuff
    private static int leagueLevel;
    private static List<String> availablePatterns;
    private static int numCardsPerPlayer;
    private static int numVisibleCards;
    private static boolean threeWayTiles; //place items on 3+ tiles only

    public void init() {
        nicksModule = nicknamesHandlerModule;
        myGameManager = gameManager;
        Properties params = gameManager.getGameParameters();
        Constants.random = new Random(getSeed(params));

        gameTurnsLeft = Constants.MAX_GAME_TURNS;
        leagueLevel = gameManager.getLeagueLevel();
        gameManager.setMaxTurns(MAX_NUM_TURNS);

        switch (leagueLevel) {
            //numCardsPerPlayer and numVisibleCards <= 12!!!
            //make sure you have enough 3+ tiles when setting threeWayTiles to true
            case 0: //demo case
                availablePatterns = new ArrayList<>(Constants.TILE_PATTERNS.get(1));
                numCardsPerPlayer = 3;
                numVisibleCards = 1;
                threeWayTiles = false;
                break;
            case 1: // First league
                availablePatterns = new ArrayList<>(Constants.TILE_PATTERNS.get(1));
                numCardsPerPlayer = 1;
                numVisibleCards = 1;
                threeWayTiles = true;
                break;
            case 2: // Second league
                availablePatterns = new ArrayList<>(Constants.TILE_PATTERNS.get(1));
                numCardsPerPlayer = 6;
                numVisibleCards = 1;
                threeWayTiles = true;
                break;
            case 3: // Third league
                availablePatterns = new ArrayList<>(Constants.TILE_PATTERNS.get(1));
                numCardsPerPlayer = 12;
                numVisibleCards = 3;
                threeWayTiles = false;
                break;
            case 4:
            case 5:
                availablePatterns = new ArrayList<>(Constants.TILE_PATTERNS.get(2));
                numCardsPerPlayer = 12;
                numVisibleCards = 3;
                threeWayTiles = false;
                break;
            default: // All other leagues
                availablePatterns = new ArrayList<>(Constants.TILE_PATTERNS.get(3));
                numCardsPerPlayer = 12;
                numVisibleCards = 3;
                threeWayTiles = false;
                break;
        }

        // align the cards based on league data
        int cardsPosX = (Constants.MAP_POS_X - Constants.TILE_SIZE / 2
                - Math.max(0, numVisibleCards - 1) * (Constants.CARD_SIZE + Constants.CARDS_OFFSET_X)) / 2;
        Constants.CARD_POSITIONS.get(0).setX(cardsPosX);
        Constants.CARD_POSITIONS.get(1).setX(Constants.SCREEN_WIDTH - cardsPosX);
        Constants.DECK_POSITIONS.get(0).setX(cardsPosX);
        Constants.DECK_POSITIONS.get(1).setX(Constants.SCREEN_WIDTH - cardsPosX);

        loadSpriteSheets();
        createBoard();
        createPlayers();
        createView();

        // make sure the view is initialized at frame 0
        entityModule.commitWorldState(0);
    }

    private Long getSeed(Properties params) {
        try {
            return Long.parseLong(params.getProperty("seed", "0"));
        } catch(NumberFormatException nfe) {
            return 0L;
        }
    }

    private void loadSpriteSheets() {
        entityModule.createSpriteSheetLoader()
                .setSourceImage("items_sheet.png")
                .setImageCount(12)
                .setWidth(48)
                .setHeight(48)
                .setOrigRow(0)
                .setOrigCol(0)
                .setImagesPerRow(5)
                .setName("items_sheet")
                .load();
        entityModule.createSpriteSheetLoader()
                .setSourceImage("tile_decorators.png")
                .setImageCount(12)
                .setWidth(312)
                .setHeight(312)
                .setOrigRow(0)
                .setOrigCol(0)
                .setImagesPerRow(3)
                .setName("tile_decorators")
                .load();
        entityModule.createSpriteSheetLoader()
                .setSourceImage("tile_paths.png")
                .setImageCount(12)
                .setWidth(312)
                .setHeight(312)
                .setOrigRow(0)
                .setOrigCol(0)
                .setImagesPerRow(3)
                .setName("tile_paths")
                .load();
    }

    //Models
    private void createBoard() {
        gameBoard = new GameBoard(availablePatterns);
    }

    private void createPlayers() {
        List<List<Item>> itemList = getPlayerItems(numCardsPerPlayer);
        List<TileModel> tileList = getPlayerTiles(availablePatterns.get(0));

        for (Player player : gameManager.getActivePlayers()) {
            PlayerModel playerModel = player.createPlayer();
            players.add(playerModel);

            playerModel.setNumVisibleCards(numVisibleCards);
            playerModel.setCards(itemList.get(player.getIndex()));
            playerModel.setTile(tileList.get(player.getIndex()));
        }
        gameBoard.placeItems(itemList, threeWayTiles);
    }

    private List<List<Item>> getPlayerItems(int numCardsPerPlayer) {
        assert numCardsPerPlayer <= Constants.ITEM_NAMES.size();

        List<List<Item>> items = new ArrayList<>();
        List<String> shuffledItemNames = shuffleItemNames();
        for (int i = 0; i < Constants.NUM_PLAYERS; i++) {
            List<String> playerItems = shuffledItemNames.subList(0, numCardsPerPlayer);
            items.add(i, createItems(playerItems, i));
        }
        return items;
    }

    private List<String> shuffleItemNames() {
        List<String> shuffledItems = new ArrayList<>(Constants.ITEM_NAMES);
        Random rnd = Constants.random;

        //Fisher-Yates shuffle
        for (int i = shuffledItems.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(shuffledItems.size());
            String a = shuffledItems.get(index);
            shuffledItems.set(index, shuffledItems.get(i));
            shuffledItems.set(i, a);
        }
        return shuffledItems;
    }

    private List<Item> createItems(List<String> itemNames, int playerId) {
        List<Item> itemList = new ArrayList<>();
        for (String name : itemNames)
            itemList.add(new Item(name, playerId));
        return itemList;
    }

    private List<TileModel> getPlayerTiles(String pattern) {
        List<TileModel> tileList = new ArrayList<>();
        for (int i = 0; i < Constants.NUM_PLAYERS; i++) {
            TileModel playerTile = new TileModel(pattern, Constants.TILE_MODEL_POSITIONS.get(i));
            playerTile.rotate(i * 2);
            tileList.add(i, playerTile);
        }
        return tileList;
    }

    //Views
    private void createView() {
        view = new ViewController(entityModule, tooltipModule);
        createTextView();
        createBoardView();
        createPlayersView();
        updateView();
    }

    private void createTextView() {
        view.createTextView();
    }

    private void createBoardView() {
        for (int y = 0; y < Constants.MAP_HEIGHT; y++) {
            for (int x = 0; x < Constants.MAP_WIDTH; x++) {
                TileModel tile = gameBoard.getTile(x, y);
                view.createTileView(tile);
            }
        }
    }

    private void createPlayersView() {
        for (Player player : gameManager.getPlayers()) {
            view.createPlayerView(player);
            PlayerModel model = player.getPlayer();

            for (CardModel card : model.getCards())
                view.createCardView(card);

            model.flipCards();

            view.createTileView(model.getTile());

            view.createCardDeckView(player);
        }
    }

    private void updateView() {
        view.update();
    }

    //Arrow updates
    public static void setObserver(BoardView view) {
        observer = view;
    }

    private void updateObserver(AbstractMap.SimpleEntry<String, Integer> action) {
        observer.update(action);
    }

    //todo
    //Game turn
    public void gameTurn(int turn) {
        if (hasActions())
            forceAnimationFrame();
        else {
            if (gameTurnsLeft <= 0) {
                //allows both players to complete the action
                forceAnimationFrame();
                gameManager.addToGameSummary(GameManager.formatErrorMessage("Max turns reached!"));
                forceGameEnd();
            }
            checkForWinner();

            if (gameManager.getActivePlayers().isEmpty()) {
                gameManager.setFrameDuration(1);
                return;
            }

            turnType = (turnType == Action.Type.PUSH) ? Action.Type.MOVE : Action.Type.PUSH;
            if (turnType == Action.Type.PUSH) {
                gameManager.setFrameDuration(2000);
            } else {
                gameManager.setFrameDuration(700);
            }
            gameTurnsLeft--;
            forceGameFrame();
            flipCards();
            sendPlayerInputs();
            getPlayerActions();
        }
        //continues only if receives actions
        if (hasActions()) {
            doPlayerActions();
            updateView();
            checkForFinishedItems();
        }
    }

    //Flip cards
    private void flipCards() {
        gameManager.getActivePlayers().stream()
                .forEach(player -> player.getPlayer()
                        .flipCards());
    }

    private void sendPlayerInputs() {
        for (Player player : gameManager.getActivePlayers()) {
            // Turn type
            player.sendInputLine(Integer.toString(turnType.getValue()));

            // Game gameBoard
            gameBoard.sendMapToPlayer(player);

            // Player information
            int firstPlayerIndex = player.getIndex();
            int secondPlayerIndex = 1 - firstPlayerIndex;
            //int secondPlayerIndex = (firstPlayerIndex + 1) % Constants.NUM_PLAYERS;
            for (int i = 0; i < Constants.NUM_PLAYERS; i++) {
                // always send the first input to the current player, then to the other player
                int playerIndex = (i + firstPlayerIndex) % Constants.NUM_PLAYERS;
                player.sendInputLine(players.get(playerIndex).playerToString());
            }

            // Items
            gameBoard.sendItemsToPlayer(player);

            // Cards
            int numQuests = players.get(firstPlayerIndex).getNumQuestCards()
                    + players.get(secondPlayerIndex).getNumQuestCards();
            player.sendInputLine(Integer.toString(numQuests));
            for (int i = 0; i < Constants.NUM_PLAYERS; i++) {
                // always send the first input to the current player, then to the other player
                int playerIndex = (i + firstPlayerIndex) % Constants.NUM_PLAYERS;
                players.get(playerIndex).sendCardsToPlayer(player);
            }
            player.execute();
        }
    }

    //Player actions
    private boolean hasActions() {
        return !pushActions.isEmpty() || !moveActions.isEmpty() || passActions;
    }

    private void getPlayerActions() {
        for (Player player : gameManager.getActivePlayers()) {
            try {
                List<String> outputs = player.getOutputs();
                Action action = parseAction(outputs.get(0));
                if (action.isLegalAction(turnType)) {
                    if (turnType.equals(Action.Type.PUSH)) {
                        if (pushActions.size() < 2)
                            pushActions.addAll(Arrays.asList(new HashMap<>(), new HashMap<>()));
                        //0 - for rows, 1 - for columns
                        if (((PushAction) action).isHorizontal())
                            pushActions.get(0).put(player, (PushAction) action);
                        else
                            pushActions.get(1).put(player, (PushAction) action);

                    } else {
                        if (!action.isPassAction())
                            moveActions.put(player, (MoveAction) action);
                        else {
                            passActions = true;
                            gameManager.addToGameSummary(String.format("%s passed", player.getNicknameToken()));
                        }
                    }
                } else
                    throw new InvalidAction(String.format("can't \"%s\" while expecting a %s action", action.getType(), turnType));
            } catch (InvalidAction e) {
                if (e.isFatal()) {
                    player.deactivate(String.format("%s: invalid input", player.getNicknameToken()));
                    forceGameEnd();
                }
                gameManager.addToGameSummary(GameManager.formatErrorMessage(String.format("%s: invalid input - %s", player.getNicknameToken(), e.getMessage())));
            } catch (AbstractPlayer.TimeoutException e) {
                gameManager.addToGameSummary(GameManager.formatErrorMessage(String.format("%s: timeout - no input provided", player.getNicknameToken())));
                player.deactivate(String.format("%s: timeout", player.getNicknameToken()));
                forceGameEnd();
            }
        }
        pushActions.removeIf(HashMap -> HashMap.isEmpty());
    }

    private Action parseAction(String action) throws InvalidAction {
        if (action.length() > Constants.MAX_INPUT_LENGTH) {
            throw new InvalidAction("exceeded number of allowed characters");
        }

        Matcher matchPush = Constants.PLAYER_INPUT_PUSH_PATTERN.matcher(action);
        Matcher matchMove = Constants.PLAYER_INPUT_MOVE_PATTERN.matcher(action);
        Matcher matchMaxSteps = Constants.PLAYER_INPUT_MOVE_MAX_STEPS_PATTERN.matcher(action);
        Matcher matchPass = Constants.PLAYER_INPUT_PASS_PATTERN.matcher(action);
        if (matchPush.matches()) {
            return new PushAction(Integer.parseInt(matchPush.group("id")),
                    Constants.Direction.valueOf(matchPush.group("direction")), Action.Type.PUSH);
        } else if (matchMove.matches()) {
            Matcher matchSteps = Constants.PLAYER_INPUT_MOVE_STEPS_PATTERN.matcher(action);
            MoveAction moveAction = new MoveAction(Action.Type.MOVE);
            while (matchSteps.find()) {
                moveAction.addStep(Constants.Direction.valueOf(matchSteps.group("direction")));
            }
            return moveAction;
        } else if (matchPass.matches()) {
            return new PassAction(Action.Type.PASS);
        } else if (matchMaxSteps.matches()) {
            throw new InvalidAction(String.format("max %d steps - %s", Constants.MAX_MOVE_STEPS, action));
        } else {
            throw new InvalidAction(action);
        }
    }

    private void doPlayerActions(){
        if (turnType.equals(Action.Type.PUSH)) {
            Map actions = pushActions.get(0);
            pushActions.remove(0);
            doPushAction(actions);
        } else {
            for (Map.Entry action : moveActions.entrySet()) {
                doMoveAction(action);
            }
            moveActions.values().removeIf(MoveAction::isEmpty);
            passActions = false;
        }
    }

    //Move actions
    private void doMoveAction(Map.Entry<Player, MoveAction> action) {
        Player player = action.getKey();
        PlayerModel playerModel = players.get(player.getIndex());
        MoveAction moveAction = action.getValue();
        Constants.Direction step = moveAction.getStep();
        try {
            movePlayer(playerModel, step);
            gameManager.addToGameSummary(String.format("%s moved %s", player.getNicknameToken(), step));
        }
        catch (InvalidAction e) {
            moveAction.setEmpty();
            gameManager.addToGameSummary(GameManager.formatErrorMessage(String.format("%s: invalid input - MOVE %s", player.getNicknameToken(), e.getMessage())));
        }
    }

    private void movePlayer(PlayerModel player, Constants.Direction step) throws InvalidAction {
        if (!gameBoard.isValidMove(player.getPos(), step))
            throw new InvalidAction(step.toString(), false);
        player.move(step);
    }

    //Push actions
    private void doPushAction(Map<Player, PushAction> actions) {
        for (Map.Entry<Player, PushAction> action : actions.entrySet())
            //push action update: action and player id
            updateObserver(new AbstractMap.SimpleEntry<>(action.getValue().toString(), action.getKey().getIndex()));

        if (!areValidPushActions(new ArrayList(actions.values()))) {
            gameManager.addToGameSummary(GameManager.formatErrorMessage("Both players tried to push the same line. Nothing happens!"));
            return;
        }
        for (Map.Entry<Player, PushAction> action : actions.entrySet()) {
            Player player = action.getKey();
            PlayerModel playerModel = players.get(player.getIndex());
            PushAction pushAction = action.getValue();
            int line = pushAction.getLine();
            Constants.Direction direction = pushAction.getDirection();
            TileModel poppedTile = gameBoard.pushLine(playerModel.getTile(), line, direction);
            playerModel.setTile(poppedTile);
            updatePlayerPos(pushAction, pushAction.isHorizontal());
            if (pushAction.isHorizontal()) {
                gameManager.addToGameSummary(String.format("%s pushed row %d to the %s", player.getNicknameToken(), line, direction));
            } else{
                gameManager.addToGameSummary(String.format("%s pushed column %d %s", player.getNicknameToken(), line, direction));
            }
        }
    }

    private boolean areValidPushActions(List<PushAction> actions) {
        if (actions.size() < 2)
            return true;
        return !PushAction.pushSameLine(actions);
    }

    private void updatePlayerPos(PushAction action, boolean horizontal){
        for (PlayerModel player : players) {
            int playerLine = horizontal ? player.getPos().getY() : player.getPos().getX();

            if (playerLine == action.getLine()) {
                Vector2 pos = new Vector2(player.getPos());
                pos.wrap(action.getDirection().asVector());
                player.move(pos);
            }
        }
    }

    //Game management
    private void forceAnimationFrame() {
        gameManager.getActivePlayers().stream().limit(1).forEach(player -> {
            player.setExpectedOutputLines(0);
            player.execute();
            try {
                player.getOutputs();
            } catch (Exception e) {
            }
        });
    }

    private void forceGameFrame() {
        for (Player player : gameManager.getActivePlayers()) {
            player.setExpectedOutputLines(1);
        }
    }

    private void forceGameEnd() {
        for (Player player : gameManager.getPlayers()) {
            if (player.isActive()) {
                player.deactivate();
            } else {
                player.setScore(-1);
            }
        }
        gameManager.endGame();
    }

    private void checkForFinishedItems() {
        for (Player player : gameManager.getPlayers()) {
            PlayerModel playerModel = players.get(player.getIndex());
            TileModel tile = gameBoard.getTile(playerModel.getPos());
            if (tile.hasItem() && playerModel.removeCard(tile.getItem())) {
                player.setScore(player.getScore() + POINTS_PER_ITEM);
                gameManager.addToGameSummary(GameManager.formatSuccessMessage(String.format("%s completed a quest", player.getNicknameToken())));
                gameManager.addTooltip(player,String.format("%s got the %s", player.getNicknameToken(),tile.getItem().getName()));
                gameBoard.removeItem(tile);
            }
        }
    }

    private void checkForWinner() {
        boolean win = gameManager.getActivePlayers().stream()
                .anyMatch(player -> player.getScore() == numCardsPerPlayer);
        if (win) {
            //allows both players to complete the action
            forceAnimationFrame();
            forceGameEnd();
        }
    }

    private void checkOutcome() {
        List<Integer> score = gameManager.getPlayers().stream()
                .map(player -> player.getScore())
                .collect(Collectors.toList());
        if (new HashSet(score).size() == 1) declareDraw();
        else {
            IntStream.range(0, score.size())
                    .boxed()
                    .max(Comparator.comparing(score::get))
                    .ifPresent(index -> declareWinner(gameManager.getPlayers().get(index)));
        }
    }

    private void declareWinner(Player player) {
        gameManager.addToGameSummary(GameManager.formatSuccessMessage(player.getNicknameToken() + " won!"));
    }

    private void declareDraw() {
        gameManager.addToGameSummary("It's a draw!");
    }

    public void onEnd() {
        endScreenModule.setScores(
                gameManager
                        .getPlayers()
                        .stream()
                        .mapToInt(player -> player.getScore())
                        .toArray());
        checkOutcome();
    }
}
