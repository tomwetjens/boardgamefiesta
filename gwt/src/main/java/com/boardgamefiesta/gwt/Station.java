package com.boardgamefiesta.gwt;

import com.boardgamefiesta.api.Player;
import com.boardgamefiesta.json.JsonSerializer;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.stream.Collectors;

@ToString
public class Station {

    private final int cost;
    @Getter
    private final int points;
    private final Set<DiscColor> discColors;

    private StationMaster stationMaster;
    private Worker worker;

    private final Set<Player> players;

    private Station(int cost, int points, @NonNull Collection<DiscColor> discColors, StationMaster stationMaster, Set<Player> players) {
        this.cost = cost;
        this.points = points;
        this.discColors = new HashSet<>(discColors);
        this.stationMaster = stationMaster;
        this.players = players;
    }

    static Station initial(int cost, int points, @NonNull Collection<DiscColor> discColors, StationMaster stationMaster) {
        return new Station(cost, points, discColors, stationMaster, new HashSet<>());
    }

    public Optional<StationMaster> getStationMaster() {
        return Optional.ofNullable(stationMaster);
    }

    public Optional<Worker> getWorker() {
        return Optional.ofNullable(worker);
    }

    boolean hasUpgraded(@NonNull Player player) {
        return players.contains(player);
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public Set<DiscColor> getDiscColors() {
        return Collections.unmodifiableSet(discColors);
    }

    ImmediateActions upgrade(@NonNull Game game) {
        if (players.contains(game.getCurrentPlayer())) {
            throw new GWTException(GWTError.ALREADY_UPGRADED_STATION);
        }

        game.currentPlayerState().payDollars(cost);

        players.add(game.getCurrentPlayer());

        ImmediateActions placeDiscActions = game.placeDisc(discColors);

        if (stationMaster != null) {
            game.fireEvent(game.getCurrentPlayer(), GWTEvent.Type.MAY_APPOINT_STATION_MASTER, List.of(stationMaster.name()));
            return placeDiscActions.andThen(PossibleAction.optional(Action.AppointStationMaster.class));
        }
        return placeDiscActions;
    }

    ImmediateActions appointStationMaster(@NonNull Game game, @NonNull Worker worker) {
        game.currentPlayerState().removeWorker(worker);
        this.worker = worker;

        StationMaster reward = this.stationMaster;

        game.currentPlayerState().addStationMaster(reward);
        this.stationMaster = null;

        return reward.activate(game);
    }

    ImmediateActions downgrade(Game state) {
        if (!players.remove(state.getCurrentPlayer())) {
            throw new GWTException(GWTError.STATION_NOT_UPGRADED_BY_PLAYER);
        }
        return ImmediateActions.none();
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("cost", cost)
                .add("points", points)
                .add("discColors", JsonSerializer.forFactory(factory).fromStrings(discColors, DiscColor::name))
                .add("stationMaster", stationMaster != null ? stationMaster.name() : null)
                .add("players", JsonSerializer.forFactory(factory).fromStrings(players, Player::getName))
                .build();
    }

    static Station deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        return new Station(
                jsonObject.getInt("cost"),
                jsonObject.getInt("points"),
                jsonObject.getJsonArray("discColors").getValuesAs(JsonString::getString).stream()
                        .map(DiscColor::valueOf).collect(Collectors.toSet()),
                jsonObject.getString("stationMaster") != null ? StationMaster.valueOf(jsonObject.getString("stationMaster")) : null,
                jsonObject.getJsonArray("players").getValuesAs(JsonString::getString).stream().
                        map(playerMap::get).collect(Collectors.toSet()));
    }

}
