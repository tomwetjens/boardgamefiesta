package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Getter;
import lombok.NonNull;

public class Game {

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

    private final Queue<ObjectiveCard> objectiveCards;

    private final ActionStack actionStack;

    @Getter
    private Player currentPlayer;

    public Game(@NonNull Collection<String> players, Random random) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("At least 2 players are required");
        }

        if (players.size() > 4) {
            throw new IllegalArgumentException("A maximum of 4 players is supported");
        }

        this.players = createPlayers(players, random);
        Collections.shuffle(this.players, random);

        // TODO Random building variants
        PlayerBuilding.VariantSet buildings = PlayerBuilding.VariantSet.firstGame();

        this.playerStates = new HashMap<>();
        int startBalance = 6;
        for (Player player : this.players) {
            this.playerStates.put(player, new PlayerState(player, startBalance++, random, buildings.createPlayerBuildings(player)));
        }

        this.currentPlayer = this.players.get(0);

        this.railroadTrack = new RailroadTrack(this.players, random);

        this.trail = new Trail();
        this.kansasCitySupply = new KansasCitySupply(random);

        placeInitialTiles();

        this.foresights = new Foresights(kansasCitySupply);
        this.jobMarket = new JobMarket(players.size());
        this.cattleMarket = new CattleMarket(players.size(), random);

        this.objectiveCards = ObjectiveCard.randomDeck(random);

        this.actionStack = new ActionStack(Collections.singleton(PossibleAction.mandatory(Move.class)));
    }

    private List<Player> createPlayers(@NonNull Collection<String> names, @NonNull Random random) {
        List<Player.Color> randomColors = new LinkedList<>(Arrays.asList(Player.Color.values()));
        Collections.shuffle(randomColors, random);

        return names.stream()
                .map(name -> new Player(name, randomColors.remove(0)))
                .collect(Collectors.toList());
    }

    private void placeInitialTiles() {
        IntStream.range(0, 7)
                .mapToObj(i -> kansasCitySupply.draw(0))
                .forEach(this::placeInitialTile);
    }

    private void placeInitialTile(KansasCitySupply.Tile tile) {
        if (tile.getHazard() != null) {
            trail.getHazardLocations(tile.getHazard().getType()).stream()
                    .filter(Location.HazardLocation::isEmpty)
                    .findFirst().ifPresent(hazardLocation -> hazardLocation.placeHazard(tile.getHazard()));
        } else {
            trail.getTeepeeLocations().stream()
                    .filter(Location.TeepeeLocation::isEmpty)
                    .findFirst().ifPresent(teepeeLocation -> teepeeLocation.placeTeepee(tile.getTeepee()));
        }
    }

    public void perform(@NonNull Action action) {
        if (action.canPlayAnyTime()) {
            if (!actionStack.isEmpty() && actionStack.peek().isImmediate()) {
                throw new IllegalStateException("Immediate action to be performed first");
            }

            ImmediateActions immediateActions = action.perform(this);
            actionStack.push(immediateActions.getActions());
        } else {
            if (!actionStack.canPerform(action.getClass())) {
                throw new IllegalStateException("Not allowed to perform action");
            }

            ImmediateActions immediateActions = action.perform(this);
            actionStack.perform(action.getClass());
            actionStack.push(immediateActions.getActions());
        }

        if (actionStack.isEmpty() && !currentPlayerState().canPlayObjectiveCard()) {
            // Can only automatically end turn when no actions remaining,
            // and player cannot (optionally) play an objective card
            endTurn();
        }
    }

    public void endTurn() {
        actionStack.skip();

        currentPlayerState().drawUpToHandLimit();

        currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());
    }

    PlayerState currentPlayerState() {
        return playerState(currentPlayer);
    }

    public PlayerState playerState(Player player) {
        return playerStates.get(player);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Set<Class<? extends Action>> possibleActions() {
        Set<Class<? extends Action>> possibleActions = actionStack.getPossibleActions();

        if (!actionStack.peek().isImmediate() && currentPlayerState().canPlayObjectiveCard()) {
            possibleActions = new HashSet<>(possibleActions);
            possibleActions.add(PlayObjectiveCard.class);
            return Collections.unmodifiableSet(possibleActions);
        }

        return possibleActions;
    }

    public ObjectiveCard takeObjectiveCard() {
        return objectiveCards.poll();
    }

    public Set<ObjectiveCard> getObjectiveCards() {
        return Collections.emptySet();
    }

    public RailroadTrack getRailroadTrack() {
        return railroadTrack;
    }
}
