package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.gwt.logic.*;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class RailroadTrackView {

    Map<PlayerColor, String> players;
    List<StationView> stations;
    Map<City, List<PlayerColor>> cities;
    Map<String, TownView> towns;
    List<StationMaster> bonusStationMasters;

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

        if (railroadTrack.isRailsToTheNorth()) {
            towns = railroadTrack.getTowns().stream()
                    .collect(Collectors.toMap(RailroadTrack.Town::getName, town -> new TownView(railroadTrack, town)));
        } else {
            towns = null;
        }

        bonusStationMasters = railroadTrack.getBonusStationMasters().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Value
    public static class StationView {

        Worker worker;
        StationMaster stationMaster;
        List<PlayerColor> players;

        StationView(Station station, Optional<StationMaster> stationMaster, Optional<Worker> worker, List<Player> players) {
            this.worker = worker.orElse(null);
            this.stationMaster = stationMaster.orElse(null);
            this.players = players.stream().map(Player::getColor).collect(Collectors.toList());
        }
    }

    @Value
    public static class TownView {
        MediumTownTile mediumTownTile;
        List<PlayerColor> branchlets;

        public TownView(RailroadTrack railroadTrack, RailroadTrack.Town town) {
            branchlets = railroadTrack.getBranchlets(town).stream().map(Player::getColor).collect(Collectors.toList());
            mediumTownTile = railroadTrack.getMediumTownTile(town).orElse(null);
        }
    }
}

