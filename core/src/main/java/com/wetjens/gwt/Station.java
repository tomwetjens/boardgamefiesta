package com.wetjens.gwt;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

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
            throw new IllegalStateException("Already upgraded station");
        }

        game.currentPlayerState().payDollars(cost);

        players.add(game.getCurrentPlayer());

        game.placeDisc(discColors);

        return stationMaster != null
                ? ImmediateActions.of(PossibleAction.optional(Action.AppointStationMaster.class))
                : ImmediateActions.none();
    }

    ImmediateActions appointStationMaster(@NonNull Game game, @NonNull Worker worker) {
        game.currentPlayerState().removeWorker(worker);
        this.worker = worker;

        StationMaster reward = this.stationMaster;

        game.currentPlayerState().addStationMaster(reward);
        this.stationMaster = null;

        return reward.getImmediateActions();
    }

    ImmediateActions downgrade(Game game) {
        if (!players.remove(game.getCurrentPlayer())) {
            throw new IllegalStateException("Player did not upgrade station");
        }
        return ImmediateActions.none();
    }
}
