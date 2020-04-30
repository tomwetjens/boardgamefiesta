package com.wetjens.gwt;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.NonNull;

public class Game implements Serializable {

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

    private final Set<GWTEventListener> eventListeners = new HashSet<>();

    @Getter
    private Player currentPlayer;

    ImmediateActions deliverToCity(City city) {
        return railroadTrack.deliverToCity(currentPlayer, city, this)
                .andThen(placeDisc(city.getDiscColors()));
    }

    ImmediateActions placeDisc(Collection<DiscColor> discColors) {
        if (currentPlayerState().canUnlock(discColors)) {
            return ImmediateActions.of(PossibleAction.mandatory(discColors.contains(DiscColor.BLACK) ? Action.UnlockBlackOrWhite.class : Action.UnlockWhite.class));
        } else {
            // If player MUST remove WHITE disc, but player only has BLACK discs left,
            // then by exception the player may remove a BLACK disc
            if (currentPlayerState().canUnlock(Collections.singleton(DiscColor.BLACK))) {
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

    public Game(@NonNull Set<Player> players, boolean beginner, Random random) {
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

        this.railroadTrack = new RailroadTrack(this.players, random);

        this.kansasCitySupply = new KansasCitySupply(random);
        this.trail = new Trail(this.players, beginner, random);
        this.jobMarket = new JobMarket(players.size());
        placeInitialTiles();

        this.foresights = new Foresights(kansasCitySupply);
        this.cattleMarket = new CattleMarket(players.size(), random);
        this.objectiveCards = new ObjectiveCards(random);

        this.actionStack = new ActionStack(Collections.singleton(PossibleAction.mandatory(Action.Move.class)));
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

    public void perform(@NonNull Action action, @NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        if (action.canPlayAnyTime()) {
            if (!actionStack.isEmpty() && actionStack.peek().isImmediate()) {
                throw new GWTException(GWTError.IMMEDIATE_ACTION_MUST_BE_PERFORMED_FIRST);
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

    public void addEventListener(GWTEventListener eventLogger) {
        eventListeners.add(eventLogger);
    }

    void fireEvent(Player player, GWTEvent.Type type, List<Object> values) {
        eventListeners.forEach(eventLogger -> eventLogger.event(new GWTEvent(player, type, values)));
    }

    private void fireEvent(Action action) {
        fireEvent(currentPlayer, GWTEvent.Type.ACTION, Stream.concat(Stream.of(action), action.toEventParams(this).stream()).collect(Collectors.toList()));
    }

    private void endTurnIfNoMoreActions(@NonNull Random random) {
        if (actionStack.isEmpty() && !currentPlayerState().canPlayObjectiveCard()) {
            // Can only automatically end turn when no actions remaining,
            // and player cannot (optionally) play an objective card
            endTurn(random);
        }
    }

    public boolean isEnded() {
        return jobMarket.isClosed() && currentPlayerState().hasJobMarketToken();
    }

    public void skip(@NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        actionStack.skip();

        endTurnIfNoMoreActions(random);
    }

    public void endTurn(@NonNull Random random) {
        if (isEnded()) {
            throw new GWTException(GWTError.GAME_ENDED);
        }

        actionStack.skipAll();

        if (!isEnded()) {
            currentPlayerState().drawUpToHandLimit(random);

            currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());

            actionStack.push(Collections.singleton(PossibleAction.mandatory(Action.Move.class)));
        }
    }

    public PlayerState currentPlayerState() {
        return playerState(currentPlayer);
    }

    public PlayerState playerState(Player player) {
        return playerStates.get(player);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Set<Class<? extends Action>> possibleActions() {
        if (isEnded()) {
            return Collections.emptySet();
        }

        Set<Class<? extends Action>> possibleActions = actionStack.getPossibleActions();

        if ((actionStack.isEmpty() || !actionStack.peek().isImmediate()) && currentPlayerState().canPlayObjectiveCard()) {
            possibleActions = new HashSet<>(possibleActions);
            possibleActions.add(Action.PlayObjectiveCard.class);
            return Collections.unmodifiableSet(possibleActions);
        }

        return possibleActions;
    }

    public Set<List<Location>> possibleMoves(Player player, Location to) {
        if (isEnded()) {
            return Collections.emptySet();
        }
        Location from = trail.getCurrentLocation(player);
        return trail.possibleMoves(from, to, playerState(player).getStepLimit(players.size()));
    }

    public int score(Player player) {
        return playerState(player).score(this) + trail.score(player) + railroadTrack.score(player);
    }

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

    public Set<Player> winners() {
        Map<Player, Integer> scores = players.stream()
                .collect(Collectors.toMap(Function.identity(), this::score));

        int maxScore = scores.values().stream().max(Integer::compare).orElse(0);

        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
