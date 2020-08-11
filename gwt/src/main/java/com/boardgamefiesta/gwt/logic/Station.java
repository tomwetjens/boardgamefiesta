package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.stream.Collectors;

@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Station {

    private final int cost;

    @Getter
    private final int points;

    @NonNull
    private final Set<DiscColor> discColors;

    @NonNull
    private final Set<Player> players;

    private StationMaster stationMaster;
    private Worker worker;

    static Station initial(int cost, int points, @NonNull Collection<DiscColor> discColors, StationMaster stationMaster) {
        return new Station(cost, points, new HashSet<>(discColors), new HashSet<>(), stationMaster, null);
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

        ImmediateActions placeDiscActions = game.removeDisc(discColors);

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

    void downgrade(Game state) {
        if (!players.remove(state.getCurrentPlayer())) {
            throw new GWTException(GWTError.STATION_NOT_UPGRADED_BY_PLAYER);
        }
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var builder = factory.createObjectBuilder()
                .add("cost", cost)
                .add("points", points)
                .add("discColors", JsonSerializer.forFactory(factory).fromStrings(discColors, DiscColor::name))
                .add("players", JsonSerializer.forFactory(factory).fromStrings(players, Player::getName));

        if (stationMaster != null) {
            builder.add("stationMaster", stationMaster.name());
        }
        if (worker != null) {
            builder.add("worker", worker.name());
        }

        return builder.build();
    }

    static Station deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        return new Station(
                jsonObject.getInt("cost"),
                jsonObject.getInt("points"),
                jsonObject.getJsonArray("discColors").getValuesAs(JsonString::getString).stream()
                        .map(DiscColor::valueOf).collect(Collectors.toSet()),
                jsonObject.getJsonArray("players").getValuesAs(JsonString::getString).stream().
                        map(playerMap::get).collect(Collectors.toSet()),
                jsonObject.containsKey("stationMaster") ? StationMaster.valueOf( jsonObject.getString("stationMaster")) : null,
                jsonObject.containsKey("worker") ? Worker.valueOf(jsonObject.getString("worker")) : null);
    }

}
