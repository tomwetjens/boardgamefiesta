package com.wetjens.gwt;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
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
import java.util.stream.IntStream;

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

    @Getter
    private Player currentPlayer;

    ImmediateActions deliverToCity(City city) {
        ImmediateActions immediateActions = railroadTrack.deliverToCity(currentPlayer, city)
                .andThen(placeDisc(city.getDiscColors()));

        if (city == City.KANSAS_CITY) {
            currentPlayerState().gainDollars(6);
        }

        return immediateActions;
    }

    ImmediateActions placeDisc(Collection<DiscColor> discColors) {
        if (currentPlayerState().canUnlock(discColors)) {
            return ImmediateActions.of(PossibleAction.mandatory(discColors.contains(DiscColor.BLACK) ? Action.UnlockBlackOrWhite.class : Action.UnlockWhite.class));
        } else {
            // If player MUST remove WHITE disc, but player only has BLACK discs left,
            // then by exception the player may remove a BLACK disc
            if (currentPlayerState().canUnlock(Collections.singleton(DiscColor.BLACK))) {
                return ImmediateActions.of(PossibleAction.mandatory(Action.UnlockBlackOrWhite.class));
            } else {
                // If player MUST remove a disc, but has no more discs to remove,
                // then player MUST remove the disc from one of his stations
                return ImmediateActions.of(PossibleAction.mandatory(Action.DowngradeStation.class));
            }
        }
    }

    public Game(@NonNull Set<Player> players, boolean beginner, Random random) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("At least 2 players are required");
        }

        if (players.size() > 4) {
            throw new IllegalArgumentException("A maximum of 4 players is supported");
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

        this.trail = new Trail(this.players, beginner, random);
        this.kansasCitySupply = new KansasCitySupply(random);

        placeInitialTiles();

        this.foresights = new Foresights(kansasCitySupply);
        this.jobMarket = new JobMarket(players.size());
        this.cattleMarket = new CattleMarket(players.size(), random);

        this.objectiveCards = new ObjectiveCards(random);

        this.actionStack = new ActionStack(Collections.singleton(PossibleAction.mandatory(Action.Move.class)));
    }

    private void placeInitialTiles() {
        IntStream.range(0, 7)
                .mapToObj(i -> kansasCitySupply.draw(0))
                .forEach(this::placeInitialTile);
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
            throw new IllegalStateException("Game has ended");
        }

        if (action.canPlayAnyTime()) {
            if (!actionStack.isEmpty() && actionStack.peek().isImmediate()) {
                throw new IllegalStateException("Immediate action to be performed first");
            }

            ImmediateActions immediateActions = action.perform(this, random);
            actionStack.push(immediateActions.getActions());
        } else {
            if (!actionStack.canPerform(action.getClass())) {
                throw new IllegalStateException("Not allowed to perform action");
            }

            ImmediateActions immediateActions = action.perform(this, random);
            actionStack.perform(action.getClass());
            actionStack.push(immediateActions.getActions());
        }

        if (actionStack.isEmpty() && !currentPlayerState().canPlayObjectiveCard()) {
            // Can only automatically end turn when no actions remaining,
            // and player cannot (optionally) play an objective card
            endTurn(random);
        }
    }

    public boolean isEnded() {
        return jobMarket.isClosed() && currentPlayerState().hasJobMarketToken();
    }

    public void endTurn(@NonNull Random random) {
        if (isEnded()) {
            throw new IllegalStateException("Game has ended");
        }

        actionStack.skip();

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

    public void serialize(OutputStream outputStream) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(this);
        }
    }

    public static Game deserialize(InputStream inputStream) throws IOException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (Game) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
