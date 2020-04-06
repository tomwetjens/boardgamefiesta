package com.wetjens.gwt;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
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

    @Value
    @Builder
    public static final class Options {
        boolean beginner;
    }

    public Game(@NonNull Collection<String> players, Options options, Random random) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("At least 2 players are required");
        }

        if (players.size() > 4) {
            throw new IllegalArgumentException("A maximum of 4 players is supported");
        }

        this.players = createPlayers(players, random);
        Collections.shuffle(this.players, random);

        PlayerBuilding.BuildingSet buildings = options.isBeginner()
                ? PlayerBuilding.BuildingSet.beginner()
                : PlayerBuilding.BuildingSet.random(random);

        this.playerStates = new HashMap<>();
        int startBalance = 6;
        for (Player player : this.players) {
            this.playerStates.put(player, new PlayerState(player, startBalance++, random, buildings));
        }

        this.currentPlayer = this.players.get(0);

        this.railroadTrack = new RailroadTrack(this.players, random);

        this.trail = new Trail(random);
        this.kansasCitySupply = new KansasCitySupply(random);

        placeInitialTiles();

        this.foresights = new Foresights(kansasCitySupply);
        this.jobMarket = new JobMarket(players.size());
        this.cattleMarket = new CattleMarket(players.size(), random);

        this.objectiveCards = new ObjectiveCards(random);

        this.actionStack = new ActionStack(Collections.singleton(PossibleAction.mandatory(Action.Move.class)));
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

        if (!actionStack.isEmpty() && !actionStack.peek().isImmediate() && currentPlayerState().canPlayObjectiveCard()) {
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
        return trail.possibleMoves(from, to, playerState(player).getStepLimit());
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
