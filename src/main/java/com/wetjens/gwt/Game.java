package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.Getter;
import lombok.NonNull;

public class Game {

    private final List<Player> players;

    private final EnumMap<Player, PlayerState> playerStates;

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

    private final Queue<ObjectiveCard> objectiveCards;

    private final ActionQueue actionQueue;

    @Getter
    private Player currentPlayer;

    private Phase phase;

    public Game(@NonNull Collection<Player> players, Random random) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("At least 2 players are required");
        }

        if (players.size() > 4) {
            throw new IllegalArgumentException("A maximum of 4 players is supported");
        }

        this.players = new ArrayList<>(players);
        Collections.shuffle(this.players, random);

        // TODO Random building variants
        PlayerBuilding.VariantSet buildings = PlayerBuilding.VariantSet.firstGame();

        this.playerStates = new EnumMap<>(Player.class);
        for (int i = 0; i < this.players.size(); i++) {
            Player player = this.players.get(0);
            this.playerStates.put(player, new PlayerState(player, 6 + i, random, buildings.createPlayerBuildings(player)));
        }

        this.currentPlayer = this.players.get(0);

        this.railroadTrack = new RailroadTrack(players, random);

        this.trail = new Trail();
        this.kansasCitySupply = new KansasCitySupply(random);

        placeInitialTiles();

        this.foresights = new Foresights(kansasCitySupply);
        this.jobMarket = new JobMarket(players.size());
        this.cattleMarket = new CattleMarket(players.size(), random);

        this.objectiveCards = ObjectiveCard.randomDeck(random);

        this.phase = Phase.MOVE;
        this.actionQueue = new ActionQueue(Move.class);
    }

    private void placeInitialTiles() {
        IntStream.range(0, 7)
                .mapToObj(i -> kansasCitySupply.draw(0))
                .forEach(this::placeInitialTile);
    }

    private void placeInitialTile(KansasCitySupply.Tile tile) {
        if (tile.getHazard() != null) {
            trail.getHazardLocations()
                    .filter(Location.HazardLocation::isEmpty)
                    .findFirst().ifPresent(hazardLocation -> hazardLocation.placeHazard(tile.getHazard()));
        } else {
            trail.getTeepeeLocations()
                    .filter(Location.TeepeeLocation::isEmpty)
                    .findFirst().ifPresent(teepeeLocation -> teepeeLocation.placeTeepee(tile.getTeepee()));
        }
    }

    public void perform(Action action) {
        if (action.isArbitrary()) {
            if (actionQueue.first().isImmediate()) {
                throw new IllegalStateException("Not allowed to perform arbitrary action when there is an immediate action to be performed first");
            }

            ImmediateActions immediateActions = action.perform(this);
            actionQueue.addFirst(immediateActions.getActions());
        } else {
            if (!actionQueue.canPerform(action.getClass())) {
                throw new IllegalStateException("Not allowed to perform action");
            }

            ImmediateActions immediateActions = action.perform(this);
            actionQueue.perform(action.getClass());
            actionQueue.addFirst(immediateActions.getActions());
        }

        if (phase == Phase.MOVE) {
            phase = Phase.ACTIONS;
        }
    }

    public void skip(Class<? extends Action> action) {
        actionQueue.skip(action);
    }

    PlayerState currentPlayerState() {
        return playerState(currentPlayer);
    }

    PlayerState playerState(Player player) {
        return playerStates.get(player);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Set<Class<? extends Action>> getPossibleActions() {
        Set<Class<? extends Action>> possibleActions = actionQueue.getPossibleActions();

        if (!actionQueue.first().isImmediate() && currentPlayerState().canPlayObjectiveCard()) {
            possibleActions = new HashSet<>(possibleActions);
            possibleActions.add(PlayObjectiveCard.class);
            return Collections.unmodifiableSet(possibleActions);
        }

        return possibleActions;
    }

    public ObjectiveCard takeObjectiveCard() {
        return objectiveCards.poll();
    }

    public RailroadTrack getRailroadTrack() {
        return railroadTrack;
    }
}
