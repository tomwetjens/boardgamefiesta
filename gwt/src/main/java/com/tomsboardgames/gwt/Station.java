package com.tomsboardgames.gwt;

import com.tomsboardgames.api.Player;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;
import java.util.*;

@ToString
public class Station implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int cost;
    @Getter
    private final int points;
    private final Set<DiscColor> discColors;

    private StationMaster stationMaster;
    private Worker worker;

    private final Set<Player> players = new HashSet<>();

    Station(int cost, int points, @NonNull Collection<DiscColor> discColors, StationMaster stationMaster) {
        this.cost = cost;
        this.points = points;
        this.discColors = new HashSet<>(discColors);
        this.stationMaster = stationMaster;
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
}
