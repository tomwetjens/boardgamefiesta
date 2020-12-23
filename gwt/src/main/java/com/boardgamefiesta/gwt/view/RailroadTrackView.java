package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.gwt.logic.*;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class RailroadTrackView {

    Map<PlayerColor, String> players;
    List<StationView> stations;
    Map<City, List<PlayerColor>> cities;

    RailroadTrackView(RailroadTrack railroadTrack) {
        players = railroadTrack.getPlayers().stream()
                .collect(Collectors.toMap(Player::getColor, player -> railroadTrack.currentSpace(player).getName()));

        stations = railroadTrack.getStations().stream()
                .map(station -> new StationView(station,
                        railroadTrack.getStationMaster(station),
                        railroadTrack.getWorker(station),
                        railroadTrack.getUpgradedBy(station)))
                .collect(Collectors.toList());

        cities = railroadTrack.getDeliveries().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .map(Player::getColor)
                        .collect(Collectors.toList())));
    }

    @Value
    public class StationView {

        Worker worker;
        StationMaster stationMaster;
        Set<PlayerColor> players;

        StationView(Station station, Optional<StationMaster> stationMaster, Optional<Worker> worker, Set<Player> players) {
            this.worker = worker.orElse(null);
            this.stationMaster = stationMaster.orElse(null);
            this.players = players.stream().map(Player::getColor).collect(Collectors.toSet());
        }
    }
}

