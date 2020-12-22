package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.gwt.logic.*;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class RailroadTrackView {

    Map<PlayerColor, SpaceView> players;
    List<StationView> stations;
    Map<City, List<PlayerColor>> cities;

    RailroadTrackView(RailroadTrack railroadTrack) {
        players = railroadTrack.getPlayers().stream()
                .collect(Collectors.toMap(Player::getColor, player -> new SpaceView(railroadTrack, railroadTrack.currentSpace(player))));

        stations = railroadTrack.getStations().stream().map(StationView::new).collect(Collectors.toList());

        cities = railroadTrack.getCities().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .map(Player::getColor)
                        .collect(Collectors.toList())));
    }

    @Value
    public static class SpaceView {
        // TODO Flatten this class into just a String
        Integer number;
        Integer turnout;

        SpaceView(RailroadTrack railroadTrack, RailroadTrack.Space space) {
            if (space instanceof RailroadTrack.Space.TurnoutSpace) {
                turnout = railroadTrack.getTurnouts().indexOf(space);
                number = null;
            } else {
                number = Integer.valueOf(space.getName());
                turnout = null;
            }
        }

        public enum Type {
            NORMAL,
            TURNOUT;
        }
    }

    @Value
    public class StationView {

        Worker worker;
        StationMaster stationMaster;
        Set<PlayerColor> players;

        StationView(Station station) {
            worker = station.getWorker().orElse(null);
            stationMaster = station.getStationMaster().orElse(null);
            players = station.getPlayers().stream().map(Player::getColor).collect(Collectors.toSet());
        }
    }
}

