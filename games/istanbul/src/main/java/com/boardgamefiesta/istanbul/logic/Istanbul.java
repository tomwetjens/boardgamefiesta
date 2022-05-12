
/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Istanbul implements State {

    public static final Set<PlayerColor> SUPPORTED_COLORS = Set.of(
            PlayerColor.WHITE,
            PlayerColor.YELLOW,
            PlayerColor.RED,
            PlayerColor.GREEN,
            PlayerColor.BLUE);

    @NonNull
    @Getter
    private final List<Player> players;

    @NonNull
    private final List<Player> playerOrder;

    @NonNull
    private final Map<Player, PlayerState> playerStates;

    @Getter
    @NonNull
    private final Layout layout;

    @NonNull
    private final LinkedList<BonusCard> bonusCards;

    @Getter(AccessLevel.PACKAGE)
    private final ActionQueue actionQueue;

    @Getter
    @NonNull
    private Player currentPlayer;

    @NonNull
    private Status status;

    private boolean canUndo;

    private List<InGameEventListener> eventListeners;

    public static Istanbul start(@NonNull Set<Player> players, @NonNull LayoutType layoutType, InGameEventListener eventListener, @NonNull Random random) {
        var playerOrder = new ArrayList<>(players);
        Collections.shuffle(playerOrder, random);

        int playerCount = players.size();

        var layout = layoutType.createLayout(playerCount, random);

        layout.randomPlace(random).placeGovernor();
        layout.randomPlace(random).placeSmuggler();

        var playerStates = IntStream.range(0, players.size())
                .boxed()
                .collect(Collectors.toMap(playerOrder::get, PlayerState::start));

        var bonusCards = new LinkedList<>(BonusCard.createDeck());
        Collections.shuffle(bonusCards, random);

        var actionQueue = new ActionQueue();
        actionQueue.addFollowUp(PossibleAction.mandatory(Action.Move.class));

        var game = new Istanbul(
                new ArrayList<>(playerOrder),
                playerOrder,
                playerStates,
                layout,
                bonusCards,
                actionQueue,
                playerOrder.get(0),
                Status.STARTED,
                false,
                new ArrayList<>());

        if (eventListener != null) {
            game.addEventListener(eventListener);
        }

        var fountain = layout.getFountain();
        var policeStation = layout.getPoliceStation();
        players.forEach(player -> {
            fountain.placeMerchant(Merchant.forPlayer(player), game);
            policeStation.placeFamilyMember(game, player);

            game.takeBonusCard(player, random);
        });

        if (playerCount == 2) {
            game.placeDummyMerchants();
        }

        return game;
    }

    private void placeDummyMerchants() {
        var smallMosque = layout.getSmallMosque();
        var greatMosque = layout.getGreatMosque();
        var gemstoneDealer = layout.getGemstoneDealer();

        var availableColors = SUPPORTED_COLORS.stream()
                .filter(color -> playerOrder.stream().noneMatch(player -> player.getColor() == color))
                .collect(Collectors.toCollection(LinkedList::new));
        Collections.shuffle(availableColors);

        var places = List.of(smallMosque, greatMosque, gemstoneDealer);
        places.forEach(place -> {
            var dummy = Merchant.dummy(availableColors.poll());

            place.placeMerchant(dummy, this);
        });
    }

    public JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("players", serializer.fromCollection(players, Player::serialize))
                .add("playerOrder", serializer.fromStrings(playerOrder.stream().map(Player::getName)))
                .add("playerStates", serializer.fromMap(playerStates, Player::getName, PlayerState::serialize))
                .add("layout", layout.serialize(factory))
                .add("bonusCards", serializer.fromStrings(bonusCards, BonusCard::name))
                .add("actionQueue", actionQueue.serialize(factory))
                .add("currentPlayer", currentPlayer.getName())
                .add("status", status.name())
                .add("canUndo", canUndo)
                .build();
    }

    public static Istanbul deserialize(JsonObject jsonObject) {
        var playersPossiblyNotInOriginalOrderIfOlderGame = jsonObject.getJsonArray("players").stream()
                .map(JsonValue::asJsonObject)
                .map(Player::deserialize)
                .collect(Collectors.toList());

        var playerMap = playersPossiblyNotInOriginalOrderIfOlderGame.stream()
                .collect(Collectors.toMap(Player::getName, Function.identity()));

        var playerOrder = jsonObject.getJsonArray("playerOrder").stream()
                .map(jsonValue -> (JsonString) jsonValue)
                .map(JsonString::getString)
                .map(playerMap::get)
                .collect(Collectors.toList());

        var players = tryToReconstructOriginalOrder(playersPossiblyNotInOriginalOrderIfOlderGame, playerOrder,
                jsonObject.containsKey("startPlayer") ? playerMap.get(jsonObject.getString("startPlayer")) : null);

        return new Istanbul(
                players,
                playerOrder,
                JsonDeserializer.forObject(jsonObject.getJsonObject("playerStates")).asObjectMap(playerMap::get, PlayerState::deserialize),
                Layout.deserialize(playerMap, jsonObject.getJsonObject("layout")),
                jsonObject.getJsonArray("bonusCards").stream()
                        .map(jsonValue -> (JsonString) jsonValue)
                        .map(JsonString::getString)
                        .map(BonusCard::valueOf)
                        .collect(Collectors.toCollection(LinkedList::new)),
                ActionQueue.deserialize(jsonObject.getJsonObject("actionQueue")),
                playerMap.get(jsonObject.getString("currentPlayer")),
                Status.valueOf(jsonObject.getString("status")),
                jsonObject.getBoolean("canUndo", false),
                null);
    }

    private static List<Player> tryToReconstructOriginalOrder(Collection<Player> players, List<Player> playerOrder, Player startPlayer) {
        if (startPlayer == null) {
            // Newer game, where the original players are always stored in order
            return new ArrayList<>(players);
        } else if (playerOrder.size() == players.size()) {
            // No players left during the game, so we can use the current player order
            return new ArrayList<>(playerOrder);
        } else {
            // One or more players left during the game

            var originalPlayerOrder = new ArrayList<>(playerOrder);
            // Because the original start player is known (older games only),
            // then make sure it is the first in the list
            if (!players.contains(startPlayer)) {
                originalPlayerOrder.add(0, startPlayer);
            }

            // No way to know what the order was before one or more players left,
            // so just add them at the end of the list in a non-deterministic way
            for (Player player : players) {
                if (!originalPlayerOrder.contains(player)) {
                    originalPlayerOrder.add(player);
                }
            }

            return originalPlayerOrder;
        }
    }

    @Override
    public void perform(Player player, com.boardgamefiesta.api.domain.Action action, Random random) {
        perform((Action) action, random);
    }

    @Override
    public void addEventListener(InGameEventListener eventListener) {
        if (eventListeners == null) {
            // Could be null after deserialization
            eventListeners = new LinkedList<>();
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(InGameEventListener eventListener) {
        if (eventListeners != null) {
            // Could be null after deserialization
            eventListeners.remove(eventListener);
        }
    }

    @Override
    public int getScore(Player player) {
        return getPlayerState(player).getRubies();
    }

    @Override
    public Stats getStats(Player player) {
        return playerStates.get(player).stats();
    }

    @Override
    public List<Player> getRanking() {
        return playerStates.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Player, PlayerState>>comparingInt(entry -> entry.getValue().getRubies())
                        .thenComparingInt(entry -> entry.getValue().getLira())
                        .thenComparingInt(entry -> entry.getValue().getTotalGoods())
                        .thenComparingInt(entry -> entry.getValue().getBonusCards().size())
                        .thenComparingInt(entry -> playerOrder.indexOf(entry.getKey()))
                        .reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void perform(@NonNull Action action, @NonNull Random random) {
        ActionResult actionResult;

        if (canPerformAnyTime(action) || canPerformBeforeOrAfter(action)) {
            actionResult = action.perform(this, random);
        } else if (actionQueue.canPerform(action.getClass())) {
            actionResult = action.perform(this, random);
            actionQueue.perform(action.getClass());
        } else {
            throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
        }

        actionQueue.addFollowUp(actionResult.getFollowUpActions());
        if (actionResult.isImmediate()) {
            actionQueue.stopCurrent();
        }

        canUndo = actionResult.canUndo();

        if (!canUndo && !canPerformAnotherAction()) {
            endTurn(currentPlayer, random);
        }
    }

    @Override
    public Set<Player> getCurrentPlayers() {
        return Collections.singleton(currentPlayer);
    }

    private boolean canPerformAnotherAction() {
        return !actionQueue.isEmpty() || !anyTimeActions().isEmpty() || !beforeOrAfterActions().isEmpty();
    }

    private boolean canPerformBeforeOrAfter(Action action) {
        return actionQueue.getCurrent().isEmpty() && beforeOrAfterActions().contains(action.getClass());
    }

    private boolean canPerformAnyTime(Action action) {
        return anyTimeActions().contains(action.getClass());
    }

    @Override
    public void skip(@NonNull Player player, @NonNull Random random) {
        skip(random);
    }

    public void skip(@NonNull Random random) {
        if (!actionQueue.isEmpty()) {
            actionQueue.skip();
        }

        if (actionQueue.isEmpty()) {
            endTurn(currentPlayer, random);
        }
    }

    @Override
    public void endTurn(Player player, @NonNull Random random) {
        endTurn(random);
    }

    public void endTurn(@NonNull Random random) {
        actionQueue.skipAll();

        nextPlayer();

        canUndo = false;
    }

    @Override
    public void forceEndTurn(Player player, @NonNull Random random) {
        if (isEnded()) {
            throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
        }

        if (!currentPlayer.equals(player)) {
            throw new IstanbulException(IstanbulError.CANNOT_PERFORM_ACTION);
        }

        var playerState = playerStates.get(player);

        if (!actionQueue.isEmpty()) {
            var possibleActions = actionQueue.getPossibleActions();

            if (possibleActions.contains(Action.Move.class)) {

            }
        }

        endTurn(player, random);
    }

    private void nextPlayer() {
        if (status == Status.STARTED && currentPlayerState().hasMaxRubies(playerOrder.size())) {
            // Triggers end of game, finish the round
            fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.LAST_ROUND));
            status = Status.LAST_ROUND;
        }

        var currentPlayerIndex = playerOrder.indexOf(this.currentPlayer);
        var nextPlayer = playerOrder.get((currentPlayerIndex + 1) % playerOrder.size());
        var nextPlayerState = getPlayerState(nextPlayer);

        this.currentPlayer = nextPlayer;

        playerStates.get(currentPlayer).beginTurn();

        // Status transitions
        if (this.currentPlayer.equals(playerOrder.get(0))) {
            if (status == Status.LAST_ROUND) {
                // Finished last round
                status = Status.PLAY_LEFTOVER_BONUS_CARDS;
                fireEvent(IstanbulEvent.create(currentPlayer, IstanbulEvent.Type.PLAY_LEFTOVER_BONUS_CARDS));
            } else if (status == Status.PLAY_LEFTOVER_BONUS_CARDS) {
                // All players have played last bonus cards (if applicable)
                status = Status.ENDED;
            }
        }

        if (status == Status.PLAY_LEFTOVER_BONUS_CARDS) {
            // If player does not have any bonus cards to play, go to the next player directly
            // This can go on to the next, etc. until the start player, if no players have bonus cards to play
            if (getPossibleActions().isEmpty()) {
                nextPlayer();
            }
        }

        if (!isEnded() && status != Status.PLAY_LEFTOVER_BONUS_CARDS) {
            this.actionQueue.addFollowUp(PossibleAction.mandatory(Action.Move.class));

            if (nextPlayerState.hasMosqueTile(MosqueTile.PAY_2_LIRA_TO_RETURN_ASSISTANT)) {
                // Once in a turn
                this.actionQueue.addAnyTime(PossibleAction.optional(Action.Pay2LiraToReturnAssistant.class));
            }
        }
    }

    @Override
    public boolean isEnded() {
        return status == Status.ENDED;
    }

    @Override
    public boolean canUndo() {
        return canUndo;
    }

    @Override
    public void leave(Player player, Random random) {
        if (playerOrder.contains(player)) {
            if (currentPlayer.equals(player)) {
                actionQueue.clear();
                nextPlayer();
            }

            playerOrder.remove(player);

            // Do not remove player from "players" set since there may be buildings, railroad track etc. referring to the player still,
            // and these are deserialized back from that single set which must therefore not be modified after game has started
        }
    }

    public Set<Class<? extends Action>> getPossibleActions() {
        var possibleActions = new HashSet<>(actionQueue.getPossibleActions());
        if (actionQueue.getCurrent().isEmpty()) {
            possibleActions.addAll(beforeOrAfterActions());
        }
        possibleActions.addAll(anyTimeActions());
        return Collections.unmodifiableSet(possibleActions);
    }

    /**
     * Actions that can be performed before or after an action during the current player's turn, but not during another action.
     */
    private Set<Class<? extends Action>> beforeOrAfterActions() {
        if (currentPlayerState().hasBonusCard(BonusCard.GAIN_1_GOOD)) {
            return Collections.singleton(Action.BonusCardGain1Good.class);
        }
        return Collections.emptySet();
    }

    /**
     * Actions that can be performed at any time during the current player's turn, also during other actions.
     */
    private Set<Class<? extends Action>> anyTimeActions() {
        if (!actionQueue.canPerform(Action.DiscardBonusCard.class) // Not during use of the Governor
                && !actionQueue.canPerform(Action.TakeBonusCardCaravansary.class)) { // Not during use of Caravansary

            var result = new HashSet<Class<? extends Action>>();

            if (currentPlayerState().hasBonusCard(BonusCard.TAKE_5_LIRA)) {
                result.add(Action.BonusCardTake5Lira.class);
            }
            if (status != Status.PLAY_LEFTOVER_BONUS_CARDS) {
                if (currentPlayerState().hasBonusCard(BonusCard.FAMILY_MEMBER_TO_POLICE_STATION)) {
                    result.add(Action.PlaceFamilyMemberOnPoliceStation.class);
                }
                if (isFirstPhase() && currentPlayerState().hasBonusCard(BonusCard.RETURN_1_ASSISTANT)) {
                    result.add(Action.BonusCardReturnAssistant.class);
                }
            }

            return result;
        }
        return Collections.emptySet();
    }

    private boolean isFirstPhase() {
        return actionQueue.canPerform(Action.LeaveAssistant.class) || actionQueue.canPerform(Action.Move.class);
    }

    PlayerState currentPlayerState() {
        return playerStates.get(currentPlayer);
    }


    Place getCurrentPlace() {
        return getCurrentPlace(currentPlayer.getColor());
    }

    Place getCurrentPlace(PlayerColor playerColor) {
        return layout.currentPlaceOfMerchant(playerColor);
    }

    Place getFamilyMemberCurrentPlace(Player player) {
        return layout.currentPlaceOfFamilyMember(player);
    }

    BonusCard drawBonusCard(Random random) {
        if (bonusCards.isEmpty()) {
            bonusCards.addAll(layout.getCaravansary().takeDiscardPile());
            Collections.shuffle(bonusCards, random);
        }
        return bonusCards.poll();
    }

    ActionResult moveMerchant(@NonNull Merchant merchant, @NonNull Place to, int atLeast, int atMost) {
        var from = getCurrentPlace(merchant.getColor());

        var dist = layout.distance(from, to);
        if (dist < atLeast && dist > atMost) {
            throw new IstanbulException(IstanbulError.PLACE_NOT_REACHABLE);
        }

        from.removeMerchant(merchant);
        var actionResult = to.placeMerchant(merchant, this);

        currentPlayerState().getStats().movedMerchant(dist);

        return actionResult;
    }

    void fireEvent(IstanbulEvent event) {
        if (eventListeners != null) {
            // Could be null after deserialization
            eventListeners.forEach(eventListener -> eventListener.event(event));
        }
    }

    public Place place(int x, int y) {
        return layout.place(x, y);
    }

    Place place(Predicate<Place> predicate) {
        return layout.place(predicate);
    }

    public PlayerState getPlayerState(Player player) {
        return playerStates.get(player);
    }

    public Place.GreatMosque getGreatMosque() {
        return layout.getGreatMosque();
    }

    public Place.SmallMosque getSmallMosque() {
        return layout.getSmallMosque();
    }

    public Place.LargeMarket getLargeMarket() {
        return layout.getLargeMarket();
    }

    public Place.SmallMarket getSmallMarket() {
        return layout.getSmallMarket();
    }

    public Place.SultansPalace getSultansPalace() {
        return layout.getSultansPalace();
    }

    public Place.GemstoneDealer getGemstoneDealer() {
        return layout.getGemstoneDealer();
    }

    public Place.TeaHouse getTeaHouse() {
        return layout.getTeaHouse();
    }

    public Place.BlackMarket getBlackMarket() {
        return layout.getBlackMarket();
    }

    public Place.Fountain getFountain() {
        return layout.getFountain();
    }

    public Place.Caravansary getCaravansary() {
        return layout.getCaravansary();
    }

    public Place.PostOffice getPostOffice() {
        return layout.getPostOffice();
    }

    public Place.Wainwright getWainwright() {
        return layout.getWainwright();
    }

    public Place.SpiceWarehouse getSpiceWarehouse() {
        return layout.getSpiceWarehouse();
    }

    public Place.FruitWarehouse getFruitWarehouse() {
        return layout.getFruitWarehouse();
    }

    public Place.FabricWarehouse getFabricWarehouse() {
        return layout.getFabricWarehouse();
    }

    public Place.PoliceStation getPoliceStation() {
        return layout.getPoliceStation();
    }

    /**
     * @return players in order (that are still playing, not including players that left)
     */
    public List<Player> getPlayerOrder() {
        return Collections.unmodifiableList(playerOrder);
    }

    Merchant getCurrentMerchant() {
        return getCurrentPlace().getMerchant(currentPlayer.getColor());
    }

    public List<Place> possiblePlaces() {
        var from = getCurrentPlace();
        return layout.getPlaces().stream()
                .filter(to -> {
                    var distance = layout.distance(from, to);
                    // TODO Take into account any bonus cards the player may have
                    return distance >= 1 && distance <= 2;
                })
                .collect(Collectors.toList());
    }

    public void takeBonusCard(Random random) {
        takeBonusCard(currentPlayer, random);
    }

    private void takeBonusCard(Player player, Random random) {
        var bonusCard = drawBonusCard(random);
        playerStates.get(player).addBonusCard(bonusCard);

        fireEvent(IstanbulEvent.create(player, IstanbulEvent.Type.TAKE_BONUS_CARD));
    }

    public int getBonusCardsSize() {
        return bonusCards.size();
    }

    public int getMaxRubies() {
        return PlayerState.maxRubies(players.size());
    }

    @Override
    public int getProgress() {
        return Math.min(100, Math.round((float)playerStates.values().stream()
                .mapToInt(PlayerState::getRubies)
                .max()
                .orElse(0) / (float)getMaxRubies()));
    }

    enum Status {
        STARTED,
        LAST_ROUND,
        PLAY_LEFTOVER_BONUS_CARDS,
        ENDED
    }

    @Override
    public State clone() {
        // TODO
        return null;
    }
}
