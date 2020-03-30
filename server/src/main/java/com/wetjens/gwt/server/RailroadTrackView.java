package com.wetjens.gwt.server;

import com.wetjens.gwt.*;
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

    RailroadTrackView(RailroadTrack railroadTrack) {
        players = railroadTrack.getPlayers().stream()
                .collect(Collectors.toMap(Function.identity(), player -> new SpaceView(railroadTrack, railroadTrack.current(player))));

        stations = railroadTrack.getStations().stream().map(StationView::new).collect(Collectors.toList());
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

