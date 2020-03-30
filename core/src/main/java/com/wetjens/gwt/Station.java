package com.wetjens.gwt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import lombok.NonNull;
import lombok.ToString;

@ToString
public class Station {

    private final int cost;
    private final int points;
    private final DiscColor discColor;

    private StationMaster stationMaster;
    private Worker worker;

    private final Set<Player> players = new HashSet<>();

    public Station(int cost, int points, @NonNull DiscColor discColor, StationMaster stationMaster) {
        this.cost = cost;
        this.points = points;
        this.discColor = discColor;
        this.stationMaster = stationMaster;
    }

    public Optional<StationMaster> getStationMaster() {
        return Optional.ofNullable(stationMaster);
    }

    public Optional<Worker> getWorker() {
        return Optional.ofNullable(worker);
    }

    public boolean hasUpgraded(@NonNull Player player) {
        return players.contains(player);
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public ImmediateActions upgrade(@NonNull Game game) {
        if (players.contains(game.getCurrentPlayer())) {
            throw new IllegalStateException("Already upgraded station");
        }

        game.currentPlayerState().payDollars(cost);

        players.add(game.getCurrentPlayer());

        return stationMaster != null
                ? ImmediateActions.of(PossibleAction.optional(RailroadTrack.AppointStationMaster.class))
                : ImmediateActions.none();
    }

    public ImmediateActions appointStationMaster(@NonNull Game game, @NonNull Worker worker) {
        game.currentPlayerState().removeWorker(worker);
        this.worker = worker;

        StationMaster reward = this.stationMaster;

        game.currentPlayerState().addStationMaster(reward);
        this.stationMaster = null;

        return reward.getImmediateActions();
    }
}
