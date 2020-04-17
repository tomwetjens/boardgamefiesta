package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.City;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.RailroadTrack;
import com.wetjens.gwt.Station;
import com.wetjens.gwt.StationMaster;
import com.wetjens.gwt.Worker;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
public class RailroadTrackView {

    Map<Player, SpaceView> players;
    List<StationView> stations;
    Map<City, List<Player>> cities;

    RailroadTrackView(RailroadTrack railroadTrack) {
        players = railroadTrack.getPlayers().stream()
                .collect(Collectors.toMap(Function.identity(), player -> new SpaceView(railroadTrack, railroadTrack.currentSpace(player))));

        stations = railroadTrack.getStations().stream().map(StationView::new).collect(Collectors.toList());

        cities = railroadTrack.getCities();
    }

    @Value
    public static class SpaceView {
        Integer number;
        Integer turnout;

        SpaceView(RailroadTrack railroadTrack, RailroadTrack.Space space) {
            if (space instanceof RailroadTrack.Space.NumberedSpace) {
                number = ((RailroadTrack.Space.NumberedSpace) space).getNumber();
                turnout = null;
            } else {
                turnout = railroadTrack.getTurnouts().indexOf(space);
                number = null;
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
        Set<Player> players;

        StationView(Station station) {
            worker = station.getWorker().orElse(null);
            stationMaster = station.getStationMaster().orElse(null);
            players = station.getPlayers();
        }
    }
}

