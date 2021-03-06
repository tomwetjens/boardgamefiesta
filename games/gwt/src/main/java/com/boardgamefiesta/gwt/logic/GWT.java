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

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.api.domain.Stats;
import com.boardgamefiesta.api.repository.JsonSerializer;
import com.boardgamefiesta.gwt.view.ActionType;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class GWT implements State {

    public enum Edition {
        FIRST,
        SECOND
    }

    @Getter
    private final Edition edition;

    @Getter
    private final Options.Mode mode;

    @Getter
    private final boolean railsToTheNorth;

    @Getter
    private List<Player> players;

    private final Map<Player, PlayerState> playerStates;

    @Getter
    private final Trail trail;

    @Getter
    private final RailroadTrack railroadTrack;

    @Getter
    private final KansasCitySupply kansasCitySupply;

    @Getter
    private final Foresights foresights;

    @Getter
    private final JobMarket jobMarket;

    @Getter
    private final CattleMarket cattleMarket;

    @Getter
    private final ObjectiveCards objectiveCards;

    @Getter(value = AccessLevel.PACKAGE)
    private final ActionStack actionStack;

    private transient Set<InGameEventListener> eventListeners;

    @Getter
    private Player currentPlayer;

    private List<Player> playerOrder;

    @Getter
    private Status status;

    private boolean canUndo;

    @Getter
    private final List<ObjectiveCard> startingObjectiveCards;

    /**
     * For backwards compatbility, starts 1nd edition.
     */
    public static GWT start(@NonNull Set<Player> players, @NonNull Options options, InGameEventListener eventListener, @NonNull Random random) {
        return start(Edition.FIRST, players, options, eventListener, random);
    }

    public static GWT start(@NonNull Edition edition, @NonNull Set<Player> players, @NonNull Options options, InGameEventListener eventListener, @NonNull Random random) {
        if (players.size() < 2) {
            throw new GWTException(GWTError.AT_LEAST_2_PLAYERS_REQUIRED);
        }

        if (players.size() > 4) {
            throw new GWTException(GWTError.AT_MOST_4_PLAYERS_SUPPORTED);
        }

        var playerOrder = new LinkedList<>(players);
        Collections.shuffle(playerOrder, random);

        PlayerBuilding.BuildingSet buildings = PlayerBuilding.BuildingSet.from(edition, options, random);

        var kansasCitySupply = options.getVariant() == Options.Variant.BALANCED
                ? KansasCitySupply.balanced(players.size(), random)
                : KansasCitySupply.original(random);

        var playerStates = players.stream()
                .collect(Collectors.toMap(Function.identity(), player -> new PlayerState(player, options, 0, random, buildings)));

        var game = builder()
                .edition(edition)
                .mode(options.getMode())
                .railsToTheNorth(options.isRailsToTheNorth())
                .players(new ArrayList<>(playerOrder))
                .playerOrder(playerOrder)
                .playerStates(playerStates)
                .currentPlayer(playerOrder.get(0))
                .railroadTrack(RailroadTrack.initial(edition, players, options, random))
                .kansasCitySupply(kansasCitySupply)
                .trail(new Trail(edition, options.getBuildings() == Options.Buildings.BEGINNER, random))
                .jobMarket(new JobMarket())
                .foresights(Foresights.initial(kansasCitySupply))
                .cattleMarket(options.getVariant() == Options.Variant.BALANCED
                        ? CattleMarket.balanced(players.size(), edition == Edition.SECOND && options.isSimmental(), random)
                        : CattleMarket.original(players.size(), edition == Edition.SECOND && options.isSimmental(), random))
                .objectiveCards(new ObjectiveCards(random))
                .actionStack(ActionStack.initial(Collections.emptyList()))
                .canUndo(false)
                .status(Status.BIDDING)
                .startingObjectiveCards(ObjectiveCards.createStartingObjectiveCardsDrawStack(random, players.size()))
                .build();

        if (eventListener != null) {
            game.addEventListener(eventListener);
        }

        game.placeInitialTiles();

        if (options.getPlayerOrder() != Options.PlayerOrder.BIDDING) {
            game.start(random);
        } else {
            game.startBidding();
        }

        return game;
    }

    private void startBidding() {
        status = Status.BIDDING;

        beginFirstTurn();
    }

    void placeBid(Bid bid, Random random) {
        if (bid.getPosition() < 0 || bid.getPosition() >= playerOrder.size()) {
            throw new GWTException(GWTError.BID_INVALID_POSITION);
        }

        var playerState = currentPlayerState();

        var highestForPosition = playerStates.values().stream()
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .filter(existing -> existing.getPosition() == bid.getPosition())
                .mapToInt(Bid::getPoints)
                .max()
                .orElse(-1); // less than 0, to allow bidding 0

        if (bid.getPoints() <= highestForPosition) {
            throw new GWTException(GWTError.BID_TOO_LOW);
        }

        playerState.placeBid(bid);
    }

    private void endBiddingIfCompleted(Random random) {
        var uncontested = playerOrder.stream()
                .map(this::playerState)
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .mapToInt(Bid::getPosition)
                .distinct()
                .count();

        if (uncontested == playerOrder.size()) {
            actionStack.clear();

            playerOrder = playerOrderFromBids();
            players = new ArrayList<>(playerOrder);

            start(random);
        }
    }

    private void start(Random random) {
        players = new ArrayList<>(playerOrder);

        for (int i = 0; i < playerOrder.size(); i++) {
            var player = playerOrder.get(i);
            var playerState = playerStates.get(player);

            int startBalance = 6 + i;

            fireEvent(playerState.getPlayer(), GWTEvent.Type.PLAYER_ORDER, List.of(Integer.toString(i + 1), Integer.toString(startBalance)));

            playerState.gainDollars(startBalance);
            playerState.commitToObjectiveCard(startingObjectiveCards.remove(0));

            if (edition == Edition.SECOND && player.getType() != Player.Type.COMPUTER) {
                // Draw extra cards that must be discarded at the beginning of the first turn
                playerState.drawCards(i, random);
            }

            playerState.getAutomaState().ifPresent(automaState -> automaState.start(this, random));
        }

        status = Status.STARTED; // change before determining begin turn actions

        beginFirstTurn();
    }

    private void beginFirstTurn() {
        currentPlayer = playerOrder.get(0);
        beginTurn();
    }

    private void beginTurn() {
        actionStack.addActions(determineBeginTurnActions());

        if (status != Status.BIDDING) {
            var playerState = currentPlayerState();
            playerState.beginTurn();
        }
    }

    private void placeInitialTiles() {
        int oneTilesToBePlaced = 7;
        while (oneTilesToBePlaced > 0 && kansasCitySupply.getTilesLeft(0) > 0) {
            var tile = kansasCitySupply
                    .draw(0)
                    .orElseThrow(() -> new GWTException(GWTError.NO_TILES_LEFT));
            if (placeInitialTile(tile)) {
                oneTilesToBePlaced--;
            }
        }

        IntStream.range(0, JobMarket.getInitialWorkerCount(playerOrder.size()))
                .mapToObj(i -> kansasCitySupply.draw(1))
                .flatMap(Optional::stream)
                .map(KansasCitySupply.Tile::getWorker)
                .forEach(worker -> jobMarket.addWorker(worker, playerOrder.size()));
    }

    private boolean placeInitialTile(KansasCitySupply.Tile tile) {
        if (tile.getHazard() != null) {
            return trail.placeHazard(tile.getHazard());
        } else {
            return trail.placeTeepee(tile.getTeepee());
        }
    }

    @Override
    public Set<Player> getCurrentPlayers() {
        return Collections.singleton(currentPlayer);
    }

    @Override
    public int getProgress() {
        return isEnded() ? 100 : jobMarket.isClosed() ? 99 : jobMarket.getProgress(playerOrder.size());
    }

    @Override
    public void perform(Player player, @NonNull com.boardgamefiesta.api.domain.Action action, @NonNull Random random) {
        if (!currentPlayer.equals(player)) {
            throw new GWTException(GWTError.NOT_CURRENT_PLAYER);
        }

        perform((Action) action, random);
    }

    public void perform(@NonNull Action action, @NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (action instanceof Action.PlayObjectiveCard && !canPlayObjectiveCard()) {
            throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
        }

        // If the action is not an action that can be performed at any time (i.e. that is not in the stack)
        // or we are not started yet, but still bidding, then we MUST check that the action is in the stack
        // and also update the stack
        if (!isAnytimeAction(action) || status == Status.BIDDING) {
            if (!actionStack.canPerform(action.getClass())) {
                throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
            }

            // Action can possibly modify the action queue, so first get this action off the queue
            // before executing it
            actionStack.perform(action.getClass());
        }

        var actionResult = action.perform(this, random);

        actionStack.addImmediateActions(actionResult.getImmediateActions());
        actionStack.addActions(actionResult.getNewActions());

        canUndo = actionResult.canUndo();
    }

    private static boolean isAnytimeAction(Action action) {
        return action instanceof Action.PlayObjectiveCard
                || action instanceof Action.UseExchangeToken;
    }

    @Override
    public void addEventListener(InGameEventListener eventListener) {
        if (eventListeners == null) {
            // Could be null after deserialization
            eventListeners = new HashSet<>();
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(InGameEventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    void fireEvent(Player player, GWTEvent.Type type, List<String> values) {
        if (eventListeners != null) {
            eventListeners.forEach(eventLogger -> eventLogger.event(new GWTEvent(player, type, values)));
        }
    }

    void fireActionEvent(Action action, List<String> params) {
        fireActionEvent(action.getClass(), params);
    }

    void fireActionEvent(Class<? extends Action> actionClass, List<String> params) {
        fireActionEvent(GWTEvent.Type.ACTION, Stream.concat(Stream.of(ActionType.of(actionClass).name()), params.stream()).collect(Collectors.toList()));
    }

    void fireActionEvent(GWTEvent.Type type, List<String> params) {
        fireEvent(currentPlayer, type, params);
    }

    public void skip(@NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (actionStack.canPerform(Action.UpgradeSimmental.class)) {
            currentPlayerState().discardHand(this);
        }

        actionStack.skip();
    }

    @Override
    public void skip(@NonNull Player player, @NonNull Random random) {
        if (!currentPlayer.equals(player)) {
            throw new GWTException(GWTError.NOT_CURRENT_PLAYER);
        }

        skip(random);
    }

    @Override
    public void endTurn(Player player, @NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (!currentPlayer.equals(player)) {
            throw new GWTException(GWTError.NOT_CURRENT_PLAYER);
        }

        var playerState = playerState(player);

        if (actionStack.canPerform(Action.UpgradeSimmental.class)) {
            playerState.discardHand(this);
        }

        actionStack.skipAll();

        playerState.endTurn(this, random);

        if (trail.atKansasCity(player)) {
            trail.moveToStart(player);
        }

        canUndo = false;

        afterEndTurn(random);
    }

    @Override
    public void forceEndTurn(Player player, @NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (!currentPlayer.equals(player)) {
            throw new GWTException(GWTError.NOT_CURRENT_PLAYER);
        }

        var playerState = playerState(player);

        while (currentPlayer.equals(player) && !actionStack.isEmpty() && !actionStack.canSkip()) {
            var possibleActions = actionStack.getPossibleActions();

            if (possibleActions.contains(Action.PlaceBid.class)) {
                perform(player, new Action.PlaceBid(lowestBidPossible()), random);
            } else if (possibleActions.contains(Action.Move.class)) {
                var steps = trail.calculateMoveToNearestUsingCheapestRoute(player, playerState.getBalance(), players.size());
                perform(player, new Action.Move(steps), random);
            } else if (possibleActions.contains(Action.SingleAuxiliaryAction.class)) {
                perform(player, new Action.SingleAuxiliaryAction(), random);
                perform(player, new Action.Gain1Dollar(), random);
            } else if (possibleActions.contains(Action.DeliverToCity.class)) {
                perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), random);
            } else if (possibleActions.contains(Action.ChooseForesight1.class)) {
                perform(new Action.ChooseForesight1(foresights.chooseAnyForesight(0, random)), random);
            } else if (possibleActions.contains(Action.ChooseForesight2.class)) {
                perform(new Action.ChooseForesight2(foresights.chooseAnyForesight(1, random)), random);
            } else if (possibleActions.contains(Action.ChooseForesight3.class)) {
                perform(new Action.ChooseForesight3(foresights.chooseAnyForesight(2, random)), random);
            } else if (possibleActions.contains(Action.UnlockWhite.class)) {
                perform(new Action.UnlockWhite(playerState.chooseAnyWhiteDisc(this)), random);
            } else if (possibleActions.contains(Action.UnlockBlackOrWhite.class)) {
                perform(new Action.UnlockBlackOrWhite(playerState.chooseAnyBlackOrWhiteDisc(this)), random);
            } else if (possibleActions.contains(Action.TakeObjectiveCard.class)) {
                var available = new ArrayList<>(objectiveCards.getAvailable());
                var objectiveCard = available.get(random.nextInt(available.size()));
                perform(new Action.TakeObjectiveCard(objectiveCard), random);
            } else if (possibleActions.contains(Action.GainExchangeToken.class)) {
                perform(new Action.GainExchangeToken(), random);
            } else if (possibleActions.contains(Action.DiscardCard.class)) {
                var hand = new ArrayList<>(playerState.getHand());
                var card = hand.get(random.nextInt(hand.size()));
                perform(new Action.DiscardCard(card), random);
            } else if (possibleActions.contains(Action.RemoveCard.class)) {
                var hand = new ArrayList<>(playerState.getHand());
                var card = hand.get(random.nextInt(hand.size()));
                perform(new Action.RemoveCard(card), random);
            } else if (possibleActions.contains(Action.DowngradeStation.class)) {
                var station = railroadTrack.getStations().stream()
                        .filter(s -> railroadTrack.getUpgradedBy(s).contains(player))
                        .min(Comparator.comparingInt(Station::getPoints))
                        .orElseThrow(() -> new GWTException(GWTError.STATION_NOT_UPGRADED_BY_PLAYER));
                perform(new Action.DowngradeStation(station), random);
            } else if (possibleActions.contains(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class)) {
                var to = railroadTrack.reachableSpacesBackwards(railroadTrack.currentSpace(player), 1, 1)
                        .stream().findFirst()
                        .orElseThrow(() -> new GWTException(GWTError.SPACE_NOT_REACHABLE));
                perform(new Action.MoveEngineAtLeast1BackwardsAndGain3Dollars(to), random);
            } else if (!actionStack.canSkip()) {
                throw new GWTException(GWTError.CANNOT_FORCE_END_TURN);
            }
        }

        if (currentPlayer.equals(player)) {
            endTurn(player, random);
        }
    }

    Bid lowestBidPossible() {
        var bids = players.stream()
                .map(this::playerState)
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        return IntStream.range(0, playerOrder.size())
                .filter(position -> bids.stream().noneMatch(bid -> bid.getPosition() == position))
                .mapToObj(position -> new Bid(position, 0))
                .findFirst()
                .orElseGet(() -> bids.stream()
                        .max(Comparator.comparingInt(Bid::getPoints))
                        .map(bid -> new Bid(bid.getPosition(), bid.getPoints() + 1))
                        .orElse(new Bid(0, 0)));
    }

    private void afterEndTurn(Random random) {
        if (status == Status.BIDDING) {
            endBiddingIfCompleted(random);

            if (status == Status.STARTED) {
                return;
            }
        }

        foresights.fillUp(kansasCitySupply, !jobMarket.isClosed());

        if (currentPlayerState().hasJobMarketToken()) {
            // current player is ending the game, every other player can have one more turn
            fireEvent(currentPlayer, GWTEvent.Type.EVERY_OTHER_PLAYER_HAS_1_TURN, Collections.emptyList());
        }

        // next player
        var playerThatEndedTurn = currentPlayer;
        currentPlayer = getNextPlayer();

        if (jobMarket.isClosed()) {
            // player ended his last turn, remove from player order
            playerOrder.remove(playerThatEndedTurn);
        }

        if (!playerOrder.isEmpty()) {
            beginTurn();
        } else {
            // all last turns have been taken
            end();
        }
    }

    private void end() {
        status = Status.ENDED;
    }

    private List<PossibleAction> determineBeginTurnActions() {
        if (mustPlaceBid(currentPlayer)) {
            return Collections.singletonList(PossibleAction.mandatory(Action.PlaceBid.class));
        } else {
            var currentPlayerState = currentPlayerState();
            var mustDiscardCards = currentPlayerState.getHand().size() - currentPlayerState.getHandLimit();
            if (mustDiscardCards > 0) {
                return List.of(PossibleAction.repeat(mustDiscardCards, mustDiscardCards, Action.DiscardCard.class),
                        PossibleAction.mandatory(Action.Move.class));
            } else {
                return Collections.singletonList(PossibleAction.mandatory(Action.Move.class));
            }
        }
    }

    private boolean mustPlaceBid(Player player) {
        return status == Status.BIDDING && isBidContested(player);
    }

    private boolean isBidContested(Player player) {
        return playerState(player).getBid()
                .map(Bid::getPosition)
                .map(this::isPositionContested)
                .orElse(true);
    }

    private boolean isPositionContested(int position) {
        return playerStates.values().stream()
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .mapToInt(Bid::getPosition)
                .filter(p -> p == position)
                .count() > 1;
    }

    public PlayerState currentPlayerState() {
        return playerState(currentPlayer);
    }

    public PlayerState playerState(Player player) {
        return playerStates.get(player);
    }

    /**
     * @return players in order (that are still playing, not including players that left)
     */
    public List<Player> getPlayerOrder() {
        return Collections.unmodifiableList(playerOrder);
    }

    public Set<Class<? extends Action>> possibleActions() {
        if (isEnded()) {
            return Collections.emptySet();
        }

        var possibleActions = new HashSet<>(actionStack.getPossibleActions());

        if (status == Status.STARTED) {
            if (canPlayObjectiveCard()) {
                possibleActions.add(Action.PlayObjectiveCard.class);
            }
            if (Action.UseExchangeToken.canPerform(this)) {
                possibleActions.add(Action.UseExchangeToken.class);
            }
        }

        return Collections.unmodifiableSet(possibleActions);
    }

    private boolean canPlayObjectiveCard() {
        // During your own turn, if you happen to have one or more objective cards in your hand, you can play any
        // of them, either:
        // - before performing phase A or
        // - before or after performing any one action in phase B.
        return currentPlayer != null
                && currentPlayerState().hasObjectiveCardInHand()
                && !trail.atKansasCity(currentPlayer)
                && ((actionStack.size() == 1 && actionStack.canPerform(Action.Move.class)) // before phase A
                || !actionStack.hasImmediate() // not during an action in phase B
                || actionStack.isEmpty()); // after phase B
    }

    public Set<PossibleMove> possibleMoves(@NonNull Player player, int atMost, boolean payFees) {
        return trail.possibleMoves(player, payFees ? playerState(player).getBalance() : 0, atMost, players.size());
    }

    public Score scoreDetails(Player player) {
        var playerState = playerState(player);

        return playerState.score(this)
                .add(trail.score(player))
                .add(railroadTrack.score(this, player, playerState));
    }

    @Override
    public int getScore(Player player) {
        return scoreDetails(player).getTotal();
    }

    @Override
    public Stats getStats(Player player) {
        var playerState = playerState(player);

        var stats = Stats.builder()
                .value("rttn", railsToTheNorth ? 'Y' : 'N')
                .value("players", players.size())
                .value("seat", players.indexOf(player) + 1)
                .value("cowboys", playerState.getNumberOfCowboys())
                .value("craftsmen", playerState.getNumberOfCraftsmen())
                .value("engineers", playerState.getNumberOfEngineers())
                .value("stepLimit", playerState.getStepLimit(players.size()))
                .value("handLimit", playerState.getHandLimit())
                .value("permCerts", playerState.permanentCertificates())
                .value("tempCerts", playerState.getTempCertificates())
                .value("tempCertLimit", playerState.getTempCertificateLimit())
                .value("bid", playerState.getBid().map(Bid::getPoints).map(Object::toString).orElse(""))
                .value("turns", playerState.getTurns());

        scoreDetails(player).getCategories().forEach((category, value) ->
                stats.value("score." + category.name(), value));

        for (City city : City.values()) {
            var players = railroadTrack.getDeliveries().get(city);
            stats.value("deliveries." + city.name(), players != null ?
                    players.stream()
                            .filter(player::equals)
                            .count() : 0);
        }

        for (String name : List.of("A", "B", "C", "D", "E", "F", "G")) {
            var buildingLocation = trail.getBuildingLocations().stream()
                    .filter(location -> name.equals(location.getBuilding().map(Building::getName).orElse(null)))
                    .findAny();

            stats.value("building." + name, buildingLocation
                    .map(Location::getName)
                    .orElse(""));
            stats.value("stops." + name, buildingLocation
                    .map(location -> playerState.getStops().getOrDefault(location, 0))
                    .map(i -> Integer.toString(i))
                    .orElse(""));
        }

        for (var number : PlayerBuilding.BuildingSet.ALL.stream().sorted().collect(Collectors.toList())) {
            for (var side : Arrays.stream(PlayerBuilding.Side.values()).sorted().collect(Collectors.toList())) {
                var name = PlayerBuilding.Name.of(number, side).toString();

                var buildingLocation = trail.getBuildingLocations().stream()
                        .filter(l -> l.getBuilding()
                                .filter(building -> building.getName().equals(name))
                                .filter(building -> building instanceof PlayerBuilding)
                                .filter(building -> player.equals(((PlayerBuilding) building).getPlayer()))
                                .isPresent())
                        .findAny();

                stats.value("building." + name, buildingLocation
                        .map(Location::getName)
                        .orElse(""));
                stats.value("stops." + name, buildingLocation
                        .map(location -> playerState.getStops().getOrDefault(location, 0))
                        .map(i -> Integer.toString(i))
                        .orElse(""));
            }
        }

        stats.value("stops.hazard", trail.getHazardLocations()
                .mapToInt(location -> playerState.getStops().getOrDefault(location, 0))
                .sum());

        stats.value("stops.teepee", trail.getTeepeeLocations().stream()
                .mapToInt(location -> playerState.getStops().getOrDefault(location, 0))
                .sum());

        return stats.build();
    }

    public JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("edition", edition.name())
                .add("mode", mode.name())
                .add("railsToTheNorth", railsToTheNorth)
                .add("players", serializer.fromCollection(players, Player::serialize))
                .add("originalPlayerOrder", serializer.fromStrings(players.stream().map(Player::getName)))
                .add("playerOrder", serializer.fromStrings(playerOrder.stream().map(Player::getName)))
                .add("playerStates", serializer.fromMap(playerStates, Player::getName, playerState -> playerState.serialize(factory, railroadTrack)))
                .add("currentPlayer", currentPlayer.getName())
                .add("railroadTrack", railroadTrack.serialize(factory))
                .add("kansasCitySupply", kansasCitySupply.serialize(factory))
                .add("trail", trail.serialize(factory))
                .add("jobMarket", jobMarket.serialize(factory))
                .add("foresights", foresights.serialize(factory))
                .add("cattleMarket", cattleMarket.serialize(factory))
                .add("objectiveCards", objectiveCards.serialize(factory))
                .add("actionStack", actionStack.serialize(factory))
                .add("status", status.name())
                .add("startingObjectiveCards", serializer.fromCollection(startingObjectiveCards, ObjectiveCard::serialize))
                .add("canUndo", canUndo)
                .build();
    }

    private static class Data {
        Edition edition; // Default FIRST
        Set<Player> players;
        List<String> playerOrder;
        List<String> originalPlayerOrder; // added later, if absent then tryToReconstructOriginalOrder
        KansasCitySupply kansasCitySupply;
        boolean railsToTheNorth;
        RailroadTrack railroadTrack;
        Trail trail;
        Options.Mode mode; // default STRATEGIC
        String currentPlayer;
        JobMarket jobMarket;
        Foresights foresights;
        CattleMarket cattleMarket;
        ObjectiveCards objectiveCards;
        ActionStack actionStack;
        boolean ended; // removed later in favor of "status"
        Status status; // added later, if absent then derive from "ended"
        ObjectiveCard startingObjectiveCards;
        boolean canUndo;
    }

    public static GWT deserialize(JsonObject jsonObject) {
        var edition = Edition.valueOf(jsonObject.getString("edition", Edition.FIRST.name()));

        var players = jsonObject.getJsonArray("players").stream()
                .map(JsonValue::asJsonObject)
                .map(Player::deserialize)
                .collect(Collectors.toSet());

        var playerMap = players.stream().collect(Collectors.toMap(Player::getName, Function.identity()));

        var playerOrder = jsonObject.getJsonArray("playerOrder").stream()
                .map(jsonValue -> (JsonString) jsonValue)
                .map(JsonString::getString)
                .map(playerMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        var originalPlayerOrder = jsonObject.containsKey("originalPlayerOrder")
                ? jsonObject.getJsonArray("originalPlayerOrder").stream()
                .map(jsonValue -> (JsonString) jsonValue)
                .map(JsonString::getString)
                .map(playerMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                : tryToReconstructOriginalOrder(players, playerOrder);

        var kansasCitySupply = KansasCitySupply.deserialize(jsonObject.getJsonObject("kansasCitySupply"));

        var railsToTheNorth = jsonObject.getBoolean("railsToTheNorth", false);

        var railroadTrack = RailroadTrack.deserialize(edition, railsToTheNorth, playerMap, jsonObject.getJsonObject("railroadTrack"));

        var trail = Trail.deserialize(edition, playerMap, jsonObject.getJsonObject("trail"));

        return builder()
                .edition(edition)
                .mode(Options.Mode.valueOf(jsonObject.getString("mode", Options.Mode.STRATEGIC.name())))
                .railsToTheNorth(railsToTheNorth)
                .players(originalPlayerOrder)
                .playerOrder(playerOrder)
                .playerStates(deserializePlayerStates(edition, playerMap, railroadTrack, trail, jsonObject.getJsonObject("playerStates")))
                .currentPlayer(playerMap.get(jsonObject.getString("currentPlayer")))
                .railroadTrack(railroadTrack)
                .kansasCitySupply(kansasCitySupply)
                .trail(trail)
                .jobMarket(JobMarket.deserialize(jsonObject.getJsonObject("jobMarket")))
                .foresights(Foresights.deserialize(jsonObject.getJsonObject("foresights")))
                .cattleMarket(CattleMarket.deserialize(jsonObject.getJsonObject("cattleMarket")))
                .objectiveCards(ObjectiveCards.deserialize(jsonObject.getJsonObject("objectiveCards")))
                .actionStack(ActionStack.deserialize(jsonObject.getJsonObject("actionStack")))
                .status(jsonObject.containsKey("status")
                        ? Status.valueOf(jsonObject.getString("status"))
                        : (jsonObject.getBoolean("ended", false) ? Status.ENDED : Status.STARTED))
                .startingObjectiveCards(jsonObject.containsKey("startingObjectiveCards")
                        ? jsonObject.getJsonArray("startingObjectiveCards").stream()
                        .map(ObjectiveCard::deserialize)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .canUndo(jsonObject.getBoolean("canUndo", false))
                .build();
    }

    private static Map<Player, PlayerState> deserializePlayerStates(GWT.Edition edition, Map<String, Player> playerMap, RailroadTrack railroadTrack, Trail trail, JsonObject jsonObject) {
        return jsonObject.keySet().stream()
                .filter(playerMap::containsKey)
                .collect(Collectors.toMap(playerMap::get, key -> PlayerState.deserialize(edition, playerMap.get(key), railroadTrack, trail,
                        jsonObject.getJsonObject(key).asJsonObject())));
    }

    private static List<Player> tryToReconstructOriginalOrder(Collection<Player> players, List<Player> playerOrder) {
        if (playerOrder.size() == players.size()) {
            // No players left during the game, so we can use the current player order
            return new ArrayList<>(playerOrder);
        } else {
            // One or more players left during the game
            var originalPlayerOrder = new ArrayList<>(playerOrder);

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
    public List<Player> getRanking() {
        var scores = players.stream()
                .collect(Collectors.toMap(Function.identity(), this::getScore));

        return scores.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Player, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparingInt(entry -> playerStates.get(entry.getKey()).getBalance())
                        .thenComparing(entry -> players.indexOf(entry.getKey()))
                        .reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void leave(@NonNull Player player, @NonNull Random random) {
        if (status == Status.BIDDING) {
            playerOrder.remove(player);

            if (currentPlayer.equals(player) && status != Status.STARTED) {
                afterEndTurn(random);
            }
        } else if (status == Status.STARTED) {
            if (currentPlayer.equals(player)) {
                actionStack.clear();

                afterEndTurn(random);
            }

            playerOrder.remove(player);

            if (playerOrder.size() == 1) {
                end();
            }
        } else {
            throw new GWTException(GWTError.GAME_ENDED);
        }
    }

    ImmediateActions deliverToCity(City city) {
        var immediateActions = railroadTrack.deliverToCity(currentPlayer, city, this);
        return removeDisc(city.getDiscColors()).andThen(immediateActions);
    }

    ImmediateActions removeDisc(Collection<DiscColor> discColors) {
        if (currentPlayerState().canRemoveDisc(discColors, this)) {
            return ImmediateActions.of(PossibleAction.mandatory(discColors.contains(DiscColor.BLACK) ? Action.UnlockBlackOrWhite.class : Action.UnlockWhite.class));
        } else {
            // If player MUST remove WHITE disc, but player only has BLACK discs left,
            // then by exception the player may remove a BLACK disc
            if (currentPlayerState().canRemoveDisc(Collections.singleton(DiscColor.BLACK), this)) {
                fireEvent(currentPlayer, GWTEvent.Type.MAY_REMOVE_BLACK_DISC_INSTEAD_OF_WHITE, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.mandatory(Action.UnlockBlackOrWhite.class));
            } else {
                // If player MUST remove a disc, but has no more discs to remove from player board,
                // then player MUST remove the disc from one of his stations
                if (railroadTrack.numberOfUpgradedStations(currentPlayer) > 0) {
                    fireEvent(currentPlayer, GWTEvent.Type.MUST_REMOVE_DISC_FROM_STATION, Collections.emptyList());
                    return ImmediateActions.of(PossibleAction.mandatory(Action.DowngradeStation.class));
                } else {
                    // EXCEPTIONAL CASE: If player only has discs on cities, then he cannot remove a disc anymore
                    return ImmediateActions.none();
                }
            }
        }
    }

    public Set<PossibleMove> possibleMoves(Player player) {
        if (actionStack.canPerform(Action.Move.class)) {
            return possibleMoves(player, playerState(player).getStepLimit(players.size()), true);
        } else if (actionStack.canPerform(Action.Move1Forward.class)) {
            return possibleMoves(player, 1, true);
        } else if (actionStack.canPerform(Action.Move2Forward.class)) {
            return possibleMoves(player, 2, true);
        } else if (actionStack.canPerform(Action.Move3Forward.class)) {
            return possibleMoves(player, 3, true);
        } else if (actionStack.canPerform(Action.Move3ForwardWithoutFees.class)) {
            return possibleMoves(player, 3, false);
        } else if (actionStack.canPerform(Action.Move4Forward.class)) {
            return possibleMoves(player, 4, true);
        } else if (actionStack.canPerform(Action.Move5Forward.class)) {
            return possibleMoves(player, 5, true);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean canUndo() {
        return canUndo;
    }

    @Override
    public boolean isEnded() {
        return status == Status.ENDED;
    }

    public Set<RailroadTrack.PossibleDelivery> possibleDeliveries(Player player) {
        var playerState = playerState(player);
        return trail.atKansasCity(player)
                ? railroadTrack.possibleDeliveries(player, playerState.handValue(), playerState.getTempCertificates() + playerState.permanentCertificates())
                : railroadTrack.possibleExtraordinaryDeliveries(player, playerState.getLastEngineMove());
    }

    public List<Player> playerOrderFromBids() {
        return playerOrder.stream()
                .map(this::playerState)
                .sorted(Comparator.comparingInt((PlayerState playerState) -> playerState.getBid().map(Bid::getPosition).orElse(Integer.MAX_VALUE))
                        // If equal positions, then by points
                        .thenComparing(Comparator.comparingInt((PlayerState playerState) -> playerState.getBid().map(Bid::getPoints).orElse(Integer.MIN_VALUE)).reversed())
                        // If equal bids, keep a consistent order
                        .thenComparingInt(playerState -> playerOrder.indexOf(playerState.getPlayer())))
                .map(PlayerState::getPlayer)
                .collect(Collectors.toList());
    }

    public boolean canSkip() {
        return actionStack.canSkip();
    }

    public Player getNextPlayer() {
        Player player = currentPlayer;

        do {
            player = playerOrder.get((playerOrder.indexOf(player) + 1) % playerOrder.size());
        } while (status == Status.BIDDING && !mustPlaceBid(player));

        return player;
    }

    public void executeAutoma(Player player, Random random) {
        playerStates.get(player)
                .getAutomaState()
                .orElseThrow(() -> new GWTException(GWTError.NO_AUTOMA_STATE))
                .execute(this, random);
    }

    public enum Status {
        BIDDING,
        STARTED,
        ENDED
    }

    @Value
    @Builder
    public static class Options {
        @NonNull
        @Builder.Default
        Buildings buildings = Buildings.RANDOMIZED;

        @NonNull
        @Builder.Default
        Mode mode = Mode.ORIGINAL;

        @NonNull
        @Builder.Default
        PlayerOrder playerOrder = PlayerOrder.RANDOMIZED;

        @NonNull
        @Builder.Default
        Variant variant = Variant.ORIGINAL;

        @Builder.Default
        boolean simmental = false;

        @Builder.Default
        boolean stationMasterPromos = false;

        @Builder.Default
        boolean building11 = false;

        @Builder.Default
        boolean building13 = false;

        @Builder.Default
        boolean railsToTheNorth = false;

        @Builder.Default
        Garth.Difficulty difficulty = Garth.Difficulty.EASY;

        public enum Buildings {
            BEGINNER,
            RANDOMIZED
        }

        public enum Mode {
            ORIGINAL,
            STRATEGIC
        }

        public enum PlayerOrder {
            RANDOMIZED,
            BIDDING
        }

        public enum Variant {
            ORIGINAL,
            BALANCED
        }

    }

}
