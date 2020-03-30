package com.wetjens.gwt;

import lombok.ToString;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@ToString
public class Station {

    private final int cost;
    private final int points;
    private final DiscColor discColor;

    private StationMaster stationMaster;
    private Worker worker;

    private final Set<Player> players = new HashSet<>();

    public Station(int cost, int points, DiscColor discColor, StationMaster stationMaster) {
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

    public boolean hasUpgraded(Player player) {
        return players.contains(player);
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }
}
