package com.wetjens.gwt.server.rest.view.state;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.wetjens.gwt.City;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.RailroadTrack;
import com.wetjens.gwt.Station;
import com.wetjens.gwt.StationMaster;
import com.wetjens.gwt.Worker;
import lombok.Value;

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
        Type type;
        Integer index;

        SpaceView(RailroadTrack railroadTrack, RailroadTrack.Space space) {
            if (space instanceof RailroadTrack.Space.NumberedSpace) {
                type = Type.NORMAL;
                index = ((RailroadTrack.Space.NumberedSpace) space).getNumber();
            } else if (space instanceof RailroadTrack.Space.TurnoutSpace) {
                type = Type.TURNOUT;
                index = railroadTrack.getTurnouts().indexOf(space);
            } else {
                throw new IllegalArgumentException("Unsupported space: " + space);
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

