package com.tomsboardgames.gwt;

import com.tomsboardgames.api.EventListener;
import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.Score;
import com.tomsboardgames.api.State;
import com.tomsboardgames.gwt.view.ActionType;
import lombok.Getter;
import lombok.NonNull;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Game implements State, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Player> players;

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

    private final ActionStack actionStack;

    private transient Set<EventListener> eventListeners = new HashSet<>();

    @Getter
    private Player currentPlayer;

    @Getter
    private boolean ended;

    public Game(@NonNull Set<Player> players, boolean beginner, @NonNull Random random) {
        if (players.size() < 2) {
            throw new GWTException(GWTError.AT_LEAST_2_PLAYERS_REQUIRED);
        }

        if (players.size() > 4) {
            throw new GWTException(GWTError.AT_MOST_4_PLAYERS_SUPPORTED);
        }

        this.players = new LinkedList<>(players);
        Collections.shuffle(this.players, random);

        PlayerBuilding.BuildingSet buildings = beginner
                ? PlayerBuilding.BuildingSet.beginner()
                : PlayerBuilding.BuildingSet.random(random);

        Queue<ObjectiveCard> startingObjectiveCards = ObjectiveCards.createStartingObjectiveCardsDrawStack(random);

        this.playerStates = new HashMap<>();
        int startBalance = 6;
        for (Player player : this.players) {
            this.playerStates.put(player, new PlayerState(player, startBalance++, startingObjectiveCards.poll(), random, buildings));
        }

        this.currentPlayer = this.players.get(0);

        this.railroadTrack = new RailroadTrack(players, random);

        this.kansasCitySupply = new KansasCitySupply(random);
        this.trail = new Trail(this.players, beginner, random);
        this.jobMarket = new JobMarket(players.size());
        placeInitialTiles();

        this.foresights = new Foresights(kansasCitySupply);
        this.cattleMarket = new CattleMarket(players.size(), random);
        this.objectiveCards = new ObjectiveCards(random);

        this.actionStack = new ActionStack(Collections.singleton(PossibleAction.mandatory(Action.Move.class)));

        fireEvent(currentPlayer, GWTEvent.Type.BEGIN_TURN, Collections.emptyList());
    }

    private void placeInitialTiles() {
        IntStream.range(0, 7)
                .mapToObj(i -> kansasCitySupply.draw(0))
                .forEach(this::placeInitialTile);

        IntStream.range(0, players.size() == 2 ? 3 : jobMarket.getRowLimit() * 2 - 1)
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
    public void perform(@NonNull com.tomsboardgames.api.Action action, @NonNull Random random) {
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

            fireEvent(action);

            ImmediateActions immediateActions = action.perform(this, random);

            if (!immediateActions.isEmpty()) {
                actionStack.push(immediateActions.getActions());
            }
        } else {
            if (!actionStack.canPerform(action.getClass())) {
                throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
            }

            fireEvent(action);

            ImmediateActions immediateActions = action.perform(this, random);
            actionStack.perform(action.getClass());

            if (!immediateActions.isEmpty()) {
                actionStack.push(immediateActions.getActions());
            }
        }

        endTurnIfNoMoreActions(random);
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
        eventListeners.forEach(eventLogger -> eventLogger.event(new GWTEvent(player, type, values)));
    }

    private void fireEvent(Action action) {
        fireEvent(currentPlayer, GWTEvent.Type.ACTION, Stream.concat(Stream.of(ActionType.of(action.getClass()).name()),
                action.toEventParams(this).stream()).collect(Collectors.toList()));
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

        actionStack.skip();

        fireEvent(currentPlayer, GWTEvent.Type.SKIP, Collections.emptyList());

        endTurnIfNoMoreActions(random);
    }

    @Override
    public void endTurn(@NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        actionStack.skipAll();

        fireEvent(currentPlayer, GWTEvent.Type.END_TURN, Collections.emptyList());

        currentPlayerState().drawUpToHandLimit(random);

        afterEndTurn();
    }

    private void afterEndTurn() {
        if (currentPlayerState().hasJobMarketToken()) {
            // current player is ending the game, every other player can have one more turn
            fireEvent(currentPlayer, GWTEvent.Type.EVERY_OTHER_PLAYER_HAS_1_TURN, Collections.emptyList());
        }

        // next player
        currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());

        if (!currentPlayerState().hasJobMarketToken()) {
            actionStack.push(Collections.singleton(PossibleAction.mandatory(Action.Move.class)));

            fireEvent(currentPlayer, GWTEvent.Type.BEGIN_TURN, Collections.emptyList());
        } else {
            ended = true;
            fireEvent(currentPlayer, GWTEvent.Type.ENDS_GAME, Collections.emptyList());
        }
    }

    public PlayerState currentPlayerState() {
        return playerState(currentPlayer);
    }

    public PlayerState playerState(Player player) {
        return playerStates.get(player);
    }

    @Override
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Set<Class<? extends Action>> possibleActions() {
        if (isEnded()) {
            return Collections.emptySet();
        }

        Set<Class<? extends Action>> possibleActions = actionStack.getPossibleActions();

        if (canPlayObjectiveCard()) {
            possibleActions = new HashSet<>(possibleActions);
            possibleActions.add(Action.PlayObjectiveCard.class);
            return Collections.unmodifiableSet(possibleActions);
        }

        return possibleActions;
    }

    private boolean canPlayObjectiveCard() {
        return currentPlayerState().hasObjectiveCardInHand()
                && (actionStack.isEmpty() /* = after phase B */
                || (actionStack.size() == 1 && actionStack.canPerform(Action.Move.class))) /* before phase A */;
    }

    public Set<PossibleMove> possibleMoves(@NonNull Player player, int atMost) {
        var playerState = playerState(player);
        var stepLimit = playerState.getStepLimit(players.size());

        if (atMost > stepLimit) {
            throw new IllegalArgumentException("Can move at most " + stepLimit);
        }

        return trail.possibleMoves(player, playerState.getBalance(), atMost, players.size());
    }

    public Set<PossibleMove> possibleMoves(Player player, Location to) {
        if (isEnded()) {
            return Collections.emptySet();
        }

        Location from = trail.getCurrentLocation(player)
                .orElseThrow(() -> new GWTException(GWTError.NOT_AT_LOCATION, player));

        var playerState = playerState(player);

        return trail.possibleMoves(from, to, player, playerState.getBalance(), playerState.getStepLimit(players.size()), players.size());
    }

    public Score scoreDetails(Player player) {
        return playerState(player).score(this).add(trail.score(player)).add(railroadTrack.score(player));
    }

    @Override
    public int score(Player player) {
        return scoreDetails(player).getTotal();
    }

    @Override
    public void serialize(OutputStream outputStream) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Game deserialize(InputStream inputStream) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (Game) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<Player> winners() {
        Map<Player, Integer> scores = players.stream()
                .collect(Collectors.toMap(Function.identity(), this::score));

        int maxScore = scores.values().stream().max(Integer::compare).orElse(0);

        return scores.entrySet().stream()
                .filter(entry -> entry.getValue().equals(maxScore))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public void leave(Player player) {
        if (currentPlayer == player) {
            actionStack.clear();

            afterEndTurn();
        }

        players.remove(player);

        jobMarket.adjustRowLimit(players.size());
        // TODO Adjust cattle market?
    }

    ImmediateActions deliverToCity(City city) {
        return railroadTrack.deliverToCity(currentPlayer, city, this)
                .andThen(placeDisc(city.getDiscColors()));
    }

    ImmediateActions placeDisc(Collection<DiscColor> discColors) {
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

}
