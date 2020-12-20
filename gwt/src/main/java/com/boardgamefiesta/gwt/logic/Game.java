package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.EventListener;
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
public class Game implements State {

    @Getter
    private final Options.Mode mode;

    private final Set<Player> players;

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

    private transient Set<EventListener> eventListeners;

    @Getter
    private Player currentPlayer;

    private List<Player> playerOrder;

    @Getter
    private Status status;

    private boolean canUndo;

    @Getter
    private final List<ObjectiveCard> startingObjectiveCards;

    public static Game start(@NonNull Set<Player> players, @NonNull Options options, @NonNull Random random) {
        if (players.size() < 2) {
            throw new GWTException(GWTError.AT_LEAST_2_PLAYERS_REQUIRED);
        }

        if (players.size() > 4) {
            throw new GWTException(GWTError.AT_MOST_4_PLAYERS_SUPPORTED);
        }

        var playerOrder = new LinkedList<>(players);
        Collections.shuffle(playerOrder, random);

        PlayerBuilding.BuildingSet buildings = options.getBuildings() == Options.Buildings.BEGINNER
                ? PlayerBuilding.BuildingSet.beginner()
                : PlayerBuilding.BuildingSet.random(random);

        var playerStates = players.stream()
                .collect(Collectors.toMap(Function.identity(), player -> new PlayerState(player, 0, random, buildings)));

        var kansasCitySupply = options.getVariant() == Options.Variant.BALANCED
                ? KansasCitySupply.balanced(players.size(), random)
                : KansasCitySupply.original(random);

        var game = builder()
                .mode(options.getMode())
                .players(players)
                .playerOrder(playerOrder)
                .playerStates(playerStates)
                .currentPlayer(playerOrder.get(0))
                .railroadTrack(new RailroadTrack(players, random))
                .kansasCitySupply(kansasCitySupply)
                .trail(new Trail(options.getBuildings() == Options.Buildings.BEGINNER, random))
                .jobMarket(new JobMarket(players.size()))
                .foresights(new Foresights(kansasCitySupply))
                .cattleMarket(options.getVariant() == Options.Variant.BALANCED
                        ? CattleMarket.balanced(players.size(), random)
                        : CattleMarket.original(players.size(), random))
                .objectiveCards(new ObjectiveCards(random))
                .actionStack(ActionStack.initial(Collections.emptyList()))
                .canUndo(false)
                .status(Status.BIDDING)
                .startingObjectiveCards(ObjectiveCards.createStartingObjectiveCardsDrawStack(random, players.size()))
                .build();

        game.placeInitialTiles();

        if (options.getPlayerOrder() != Options.PlayerOrder.BIDDING) {
            game.start();
        } else {
            game.startBidding();
        }

        return game;
    }

    private void startBidding() {
        status = Status.BIDDING;

        beginFirstTurn();
    }

    void placeBid(Bid bid) {
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

        endBiddingIfCompleted();
    }

    private void endBiddingIfCompleted() {
        var uncontested = playerOrder.stream()
                .map(this::playerState)
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .mapToInt(Bid::getPosition)
                .distinct()
                .count();

        if (uncontested == playerOrder.size()) {
            actionStack.clear();
            fireEvent(currentPlayer, GWTEvent.Type.END_TURN, Collections.emptyList());

            playerOrder = playerOrderFromBids();

            start();
        }
    }

    private void start() {
        for (int i = 0; i < playerOrder.size(); i++) {
            var player = playerOrder.get(i);
            var playerState = playerStates.get(player);

            int startBalance = 6 + i;

            fireEvent(playerState.getPlayer(), GWTEvent.Type.PLAYER_ORDER, List.of(Integer.toString(i + 1), Integer.toString(startBalance)));

            playerState.gainDollars(startBalance);
            playerState.commitToObjectiveCard(startingObjectiveCards.remove(0));
        }

        status = Status.STARTED; // change before determining begin turn actions

        beginFirstTurn();
    }

    private void beginFirstTurn() {
        currentPlayer = playerOrder.get(0);
        actionStack.addActions(determineBeginTurnActions());

        fireEvent(currentPlayer, GWTEvent.Type.BEGIN_TURN, Collections.emptyList());
    }

    private void placeInitialTiles() {
        IntStream.range(0, 7)
                .mapToObj(i -> kansasCitySupply.draw(0))
                .forEach(this::placeInitialTile);

        IntStream.range(0, playerOrder.size() == 2 ? 3 : jobMarket.getRowLimit() * 2 - 1)
                .mapToObj(i -> kansasCitySupply.draw(1))
                .map(KansasCitySupply.Tile::getWorker)
                .forEach(this.jobMarket::addWorker);
    }

    private void placeInitialTile(KansasCitySupply.Tile tile) {
        if (tile.getHazard() != null) {
            trail.placeHazard(tile.getHazard());
        } else {
            trail.placeTeepee(tile.getTeepee());
        }
    }

    @Override
    public void perform(@NonNull com.boardgamefiesta.api.domain.Action action, @NonNull Random random) {
        perform((Action) action, random);
    }

    public void perform(@NonNull Action action, @NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (action instanceof Action.PlayObjectiveCard) {
            if (!canPlayObjectiveCard()) {
                throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
            }

            var actionResult = action.perform(this, random);

            actionStack.addImmediateActions(actionResult.getImmediateActions());
            actionStack.addActions(actionResult.getNewActions());

            canUndo = actionResult.canUndo();
        } else {
            if (!actionStack.canPerform(action.getClass())) {
                throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
            }

            // Action can possibly modify the action queue, so first get this action off the queue
            // before executing it
            actionStack.perform(action.getClass());

            var actionResult = action.perform(this, random);

            actionStack.addImmediateActions(actionResult.getImmediateActions());
            actionStack.addActions(actionResult.getNewActions());

            canUndo = actionResult.canUndo();
        }

        if (!canUndo) {
            endTurnIfNoMoreActions(random);
        }
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        if (eventListeners == null) {
            // Could be null after deserialization
            eventListeners = new HashSet<>();
        }
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    void fireEvent(Player player, GWTEvent.Type type, List<String> values) {
        if (eventListeners != null) {
            eventListeners.forEach(eventLogger -> eventLogger.event(new GWTEvent(player, type, values)));
        }
    }

    void fireActionEvent(Action action, List<String> params) {
        fireActionEvent(ActionType.of(action.getClass()).name(), params);
    }

    void fireActionEvent(String name, List<String> params) {
        fireEvent(currentPlayer, GWTEvent.Type.ACTION, Stream.concat(Stream.of(name), params.stream()).collect(Collectors.toList()));
    }

    private void endTurnIfNoMoreActions(@NonNull Random random) {
        if (actionStack.isEmpty() && !canPlayObjectiveCard()) {
            // Can only automatically end turn when no actions remaining,
            // and player cannot (optionally) play an objective card
            endTurn(random);
        }
    }

    @Override
    public void skip(@NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (!actionStack.isEmpty()) {
            actionStack.skip();

            fireEvent(currentPlayer, GWTEvent.Type.SKIP, Collections.emptyList());

            endTurnIfNoMoreActions(random);
        } else {
            endTurn(random);
        }
    }

    @Override
    public void endTurn(@NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        actionStack.skipAll();

        fireEvent(currentPlayer, GWTEvent.Type.END_TURN, Collections.emptyList());

        currentPlayerState().drawUpToHandLimit(random);
        canUndo = false;

        afterEndTurn();
    }

    private void afterEndTurn() {
        foresights.fillUp(!jobMarket.isClosed());

        if (currentPlayerState().hasJobMarketToken()) {
            // current player is ending the game, every other player can have one more turn
            fireEvent(currentPlayer, GWTEvent.Type.EVERY_OTHER_PLAYER_HAS_1_TURN, Collections.emptyList());
        }

        // next player
        currentPlayer = getNextPlayer();

        if (!currentPlayerState().hasJobMarketToken()) {
            actionStack.addActions(determineBeginTurnActions());

            currentPlayerState().beginTurn();

            fireEvent(currentPlayer, GWTEvent.Type.BEGIN_TURN, Collections.emptyList());
        } else {
            status = Status.ENDED;
            fireEvent(currentPlayer, GWTEvent.Type.ENDS_GAME, Collections.emptyList());
        }
    }

    private List<PossibleAction> determineBeginTurnActions() {
        if (mustPlaceBid(currentPlayer)) {
            return Collections.singletonList(PossibleAction.mandatory(Action.PlaceBid.class));
        } else {
            return Collections.singletonList(PossibleAction.mandatory(Action.Move.class));
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

    List<Player> getPlayerOrder() {
        return Collections.unmodifiableList(playerOrder);
    }

    @Override
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(playerOrder);
    }

    public Set<Class<? extends Action>> possibleActions() {
        if (isEnded()) {
            return Collections.emptySet();
        }

        var possibleActions = actionStack.getPossibleActions();

        if (canPlayObjectiveCard()) {
            possibleActions = new HashSet<>(possibleActions);
            possibleActions.add(Action.PlayObjectiveCard.class);
            return Collections.unmodifiableSet(possibleActions);
        }

        return possibleActions;
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

    public Set<PossibleMove> possibleMoves(@NonNull Player player, int atMost) {
        return trail.possibleMoves(player, playerState(player).getBalance(), atMost, playerOrder.size());
    }

    public Optional<Score> scoreDetails(Player player) {
        return isEnded() || mode == Options.Mode.STRATEGIC
                ? Optional.of(playerState(player).score(this).add(trail.score(player)).add(railroadTrack.score(player)))
                : Optional.empty();
    }

    @Override
    public Optional<Integer> score(Player player) {
        return scoreDetails(player).map(Score::getTotal);
    }

    @Override
    public Stats stats(Player player) {
        var playerState = playerState(player);

        var stats = Stats.builder()
                .value("players", playerOrder.size())
                .value("seat", playerOrder.indexOf(player) + 1)
                .value("cowboys", playerState.getNumberOfCowboys())
                .value("craftsmen", playerState.getNumberOfCraftsmen())
                .value("engineers", playerState.getNumberOfEngineers())
                .value("stepLimit", playerState.getStepLimit(playerOrder.size()))
                .value("handLimit", playerState.getHandLimit())
                .value("permCerts", playerState.permanentCertificates())
                .value("tempCerts", playerState.getTempCertificates())
                .value("tempCertLimit", playerState.getTempCertificateLimit());

        scoreDetails(player).ifPresent(score ->
                score.getCategories().forEach((category, value) ->
                        stats.value("score." + category.name(), value)));

        for (City city : City.values()) {
            stats.value("deliveries." + city.name(),
                    railroadTrack.getCities().get(city).stream()
                            .filter(delivery -> delivery == player)
                            .count());
        }

        for (String name : List.of("A", "B", "C", "D", "E", "F", "G")) {
            stats.value("building." + name, trail.getBuildingLocations().stream()
                    .filter(location -> name.equals(location.getBuilding().map(Building::getName).orElse(null)))
                    .findAny()
                    .map(Location::getName)
                    .orElse(""));
        }

        for (var number : PlayerBuilding.BuildingSet.ALL) {
            for (var side : PlayerBuilding.Side.values()) {
                var name = PlayerBuilding.Name.of(number, side).toString();

                stats.value("building." + name, trail.getBuildingLocations().stream()
                        .filter(location -> location.getBuilding()
                                .filter(building -> building.getName().equals(name))
                                .filter(building -> building instanceof PlayerBuilding)
                                .filter(building -> ((PlayerBuilding) building).getPlayer() == player)
                                .isPresent())
                        .findAny()
                        .map(Location::getName)
                        .orElse(""));
            }
        }

        return stats.build();
    }

    public JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("mode", mode.name())
                .add("players", serializer.fromCollection(players, Player::serialize))
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

    public static Game deserialize(JsonObject jsonObject) {
        var players = jsonObject.getJsonArray("players").stream()
                .map(JsonValue::asJsonObject)
                .map(Player::deserialize)
                .collect(Collectors.toSet());

        var playerMap = players.stream().collect(Collectors.toMap(Player::getName, Function.identity()));

        var playerOrder = jsonObject.getJsonArray("playerOrder").stream()
                .map(jsonValue -> (JsonString) jsonValue)
                .map(JsonString::getString)
                .map(playerMap::get)
                .collect(Collectors.toList());

        var kansasCitySupply = KansasCitySupply.deserialize(jsonObject.getJsonObject("kansasCitySupply"));

        var railroadTrack = RailroadTrack.deserialize(playerMap, jsonObject.getJsonObject("railroadTrack"));

        var trail = Trail.deserialize(playerMap, jsonObject.getJsonObject("trail"));

        return builder()
                .mode(Options.Mode.valueOf(jsonObject.getString("mode", Options.Mode.STRATEGIC.name())))
                .players(players)
                .playerOrder(playerOrder)
                .playerStates(deserializePlayerStates(playerMap, railroadTrack, trail, jsonObject.getJsonObject("playerStates")))
                .currentPlayer(playerMap.get(jsonObject.getString("currentPlayer")))
                .railroadTrack(railroadTrack)
                .kansasCitySupply(kansasCitySupply)
                .trail(trail)
                .jobMarket(JobMarket.deserialize(players.size(), jsonObject.getJsonObject("jobMarket")))
                .foresights(Foresights.deserialize(kansasCitySupply, jsonObject.getJsonObject("foresights")))
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

    private static Map<Player, PlayerState> deserializePlayerStates(Map<String, Player> playerMap, RailroadTrack railroadTrack, Trail trail, JsonObject jsonObject) {
        return jsonObject.keySet().stream()
                .collect(Collectors.toMap(playerMap::get, key -> PlayerState.deserialize(playerMap.get(key), railroadTrack, trail,
                        jsonObject.getJsonObject(key).asJsonObject())));
    }

    @Override
    public Set<Player> winners() {
        var scores = playerOrder.stream()
                .collect(Collectors.toMap(Function.identity(), player -> score(player)
                        .orElseThrow(() -> new GWTException(GWTError.GAME_NOT_ENDED))));

        int maxScore = scores.values().stream().max(Integer::compare).orElse(0);

        return scores.entrySet().stream()
                .filter(entry -> entry.getValue().equals(maxScore))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public void leave(Player player) {
        playerOrder.remove(player);
        // Do not remove player from "players" set since there may be buildings, railroad track etc. referring to the player still,
        // and these are deserialized back from that single set which must therefore not be modified after game has started

        if (status == Status.BIDDING) {
            endBiddingIfCompleted();
        } else if (status == Status.STARTED) {
            if (currentPlayer == player) {
                actionStack.clear();

                afterEndTurn();
            }
        } else {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        jobMarket.adjustRowLimit(playerOrder.size());
    }

    ImmediateActions deliverToCity(City city) {
        var immediateActions = railroadTrack.deliverToCity(currentPlayer, city, this);
        return removeDisc(city.getDiscColors()).andThen(immediateActions);
    }

    ImmediateActions removeDisc(Collection<DiscColor> discColors) {
        if (currentPlayerState().canRemoveDisc(discColors)) {
            return ImmediateActions.of(PossibleAction.mandatory(discColors.contains(DiscColor.BLACK) ? Action.UnlockBlackOrWhite.class : Action.UnlockWhite.class));
        } else {
            // If player MUST remove WHITE disc, but player only has BLACK discs left,
            // then by exception the player may remove a BLACK disc
            if (currentPlayerState().canRemoveDisc(Collections.singleton(DiscColor.BLACK))) {
                fireEvent(currentPlayer, GWTEvent.Type.MAY_REMOVE_BLACK_DISC_INSTEAD_OF_WHITE, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.mandatory(Action.UnlockBlackOrWhite.class));
            } else {
                // If player MUST remove a disc, but has no more discs to remove from player board,
                // then player MUST remove the disc from one of his stations
                if (railroadTrack.getStations().stream().anyMatch(station -> station.getPlayers().contains(currentPlayer))) {
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
            return possibleMoves(player, playerState(player).getStepLimit(players.size()));
        } else if (actionStack.canPerform(Action.Move1Forward.class)) {
            return possibleMoves(player, 1);
        } else if (actionStack.canPerform(Action.Move2Forward.class)) {
            return possibleMoves(player, 2);
        } else if (actionStack.canPerform(Action.Move3Forward.class)
                || actionStack.canPerform(Action.Move3ForwardWithoutFees.class)) {
            return possibleMoves(player, 3);
        } else if (actionStack.canPerform(Action.Move4Forward.class)) {
            return possibleMoves(player, 4);
        } else if (actionStack.canPerform(Action.Move5Forward.class)) {
            return possibleMoves(player, 5);
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
