package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

public class RailroadTrack {

    private static final int MAX_SPACE = 39;

    private static final List<Integer> TURNOUTS = Arrays.asList(4, 7, 10, 13, 16, 21, 25, 29, 33);

    private final List<Station> stations;

    @Getter
    private final Space.StartSpace start;
    @Getter
    private final Space.EndSpace end;
    private final Map<Integer, Space.NumberedSpace> normalSpaces = new HashMap<>();
    private final List<Space.TurnoutSpace> turnouts = new ArrayList<>(TURNOUTS.size());
    private final Map<Player, Space> currentSpaces = new HashMap<>();

    private final Map<City, List<Player>> cities = new HashMap<>();

    RailroadTrack(@NonNull Collection<Player> players, @NonNull Random random) {
        this.stations = createStations(random);

        end = new Space.EndSpace(MAX_SPACE, stations.get(stations.size() - 1));

        Space.NumberedSpace last = end;
        for (int number = MAX_SPACE - 1; number > 0; number--) {
            Space.NumberedSpace current = new Space.NumberedSpace(number, Collections.singleton(last));

            last.previous.add(current);
            last = current;

            normalSpaces.put(number, current);
        }

        start = new Space.StartSpace(last);
        last.previous.add(start);

        // Turn outs
        for (int i = 0; i < TURNOUTS.size(); i++) {
            int number = TURNOUTS.get(i);

            Space previous = normalSpaces.get(number);
            Space next = normalSpaces.get(number + 1);

            Space.TurnoutSpace turnout = new Space.TurnoutSpace(previous, next, stations.get(i));

            previous.next.add(turnout);
            next.previous.add(turnout);

            this.turnouts.add(turnout);
        }

        players.forEach(player -> currentSpaces.put(player, start));

        for (City city : City.values()) {
            cities.put(city, new LinkedList<>());
        }
    }

    private static List<Station> createStations(@NonNull Random random) {
        List<StationMaster> stationMasters = Arrays.asList(StationMaster.values());
        Collections.shuffle(stationMasters, random);

        return Arrays.asList(
                // TODO Correct cost and points of stations
                new Station(2, 2, DiscColor.WHITE, stationMasters.get(0)),
                new Station(2, 2, DiscColor.WHITE, stationMasters.get(1)),
                new Station(2, 2, DiscColor.WHITE, stationMasters.get(2)),
                new Station(2, 2, DiscColor.WHITE, stationMasters.get(3)),
                new Station(2, 2, DiscColor.BLACK, stationMasters.get(4)),
                new Station(2, 2, DiscColor.BLACK, null),
                new Station(2, 2, DiscColor.BLACK, null),
                new Station(2, 2, DiscColor.BLACK, null),
                new Station(2, 2, DiscColor.BLACK, null),
                new Station(2, 2, DiscColor.BLACK, null));
    }

    public List<Station> getStations() {
        return Collections.unmodifiableList(stations);
    }

    public Space.NumberedSpace getSpace(int number) {
        Space.NumberedSpace numberedSpace = normalSpaces.get(number);
        if (numberedSpace == null) {
            throw new IllegalArgumentException("No such space: " + number);
        }
        return numberedSpace;
    }

    public List<Space.TurnoutSpace> getTurnouts() {
        return Collections.unmodifiableList(turnouts);
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(currentSpaces.keySet());
    }

    public Space currentSpace(Player player) {
        return currentSpaces.get(player);
    }

    ImmediateActions moveEngineForward(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        if (atLeast < 0 || atLeast > 6) {
            throw new IllegalArgumentException("Must move at least 0..6, but was: " + atLeast);
        }

        if (atMost < 0 || atMost > 6) {
            throw new IllegalArgumentException("Must move at most 0..6, but was: " + atMost);
        }

        if (playerAt(to).isPresent()) {
            throw new IllegalStateException("Another player already on space");
        }

        Space from = currentSpace(player);

        if (to == from) {
            throw new IllegalArgumentException("Must specify different space that current");
        }

        Set<Space> reachable = reachableSpacesForward(player, from, from, atLeast, atMost);

        if (!reachable.contains(to)) {
            throw new IllegalArgumentException("Space not reachable within " + atLeast + ".." + atMost + " steps");
        }

        currentSpaces.put(player, to);

        return to.getStation()
                .filter(station -> !station.hasUpgraded(player))
                .map(station -> ImmediateActions.of(PossibleAction.optional(Action.UpgradeStation.class)))
                .orElse(ImmediateActions.none());
    }

    private Set<Space> reachableSpacesForward(@NonNull Player player, @NonNull Space from, @NonNull Space current, int atLeast, int atMost) {
        Set<Space> reachable = new HashSet<>();

        boolean available = current != from && playerAt(current).isEmpty();
        boolean possible = available && atLeast <= 1;

        if (possible) {
            reachable.add(current);
        }

        if (!available) {
            // Space is not empty, jump over
            reachable.addAll(current.next.stream()
                    .flatMap(next -> reachableSpacesForward(player, from, next, atLeast, atMost).stream())
                    .collect(Collectors.toSet()));
        } else if (possible && atMost > 1) {
            // Space is possible so count as step
            reachable.addAll(current.next.stream()
                    .flatMap(next -> reachableSpacesForward(player, from, next, Math.max(atLeast - 1, 0), atMost - 1).stream())
                    .collect(Collectors.toSet()));
        }

        return reachable;
    }

    private Set<Space> reachableSpacesBackwards(@NonNull Player player, @NonNull Space from, @NonNull Space current, int atLeast, int atMost) {
        Set<Space> reachable = new HashSet<>();

        boolean available = current != from && (current == start || playerAt(current).isEmpty());
        boolean possible = available && atLeast <= 1;

        if (possible) {
            reachable.add(current);
        }

        if (!available) {
            // Space is not empty, jump over
            reachable.addAll(current.previous.stream()
                    .flatMap(previous -> reachableSpacesBackwards(player, from, previous, atLeast, atMost).stream())
                    .collect(Collectors.toSet()));
        } else if (possible && atMost > 1) {
            // Space is possible so count as step
            reachable.addAll(current.previous.stream()
                    .flatMap(previous -> reachableSpacesBackwards(player, from, previous, Math.max(atLeast - 1, 0), atMost - 1).stream())
                    .collect(Collectors.toSet()));
        }

        return reachable;
    }

    private Optional<Player> playerAt(@NonNull Space space) {
        return currentSpaces.entrySet().stream()
                .filter(entry -> entry.getValue() == space)
                .map(Map.Entry::getKey)
                .findAny();
    }

    ImmediateActions moveEngineBackwards(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        if (atLeast < 0 || atLeast > 6) {
            throw new IllegalArgumentException("Must move at least 0..6, but was: " + atLeast);
        }

        if (atMost < 0 || atMost > 6) {
            throw new IllegalArgumentException("Must move at most 0..6, but was: " + atMost);
        }

        if (to != start && playerAt(to).isPresent()) {
            throw new IllegalStateException("Another player already on space");
        }

        Space from = currentSpace(player);

        if (to == from) {
            throw new IllegalArgumentException("Must specify different space than current");
        }

        Set<Space> reachable = reachableSpacesBackwards(player, from, from, atLeast, atMost);

        if (!reachable.contains(to)) {
            throw new IllegalArgumentException("Space not reachable within " + atLeast + ".." + atMost + " steps");
        }

        currentSpaces.put(player, to);

        return to.getStation()
                .filter(station -> !station.hasUpgraded(player))
                .map(station -> ImmediateActions.of(PossibleAction.optional(Action.UpgradeStation.class)))
                .orElse(ImmediateActions.none());
    }

    public Set<PossibleDelivery> possibleDeliveries(Player player, int breedingValue, int certificates) {
        return Arrays.stream(City.values())
                .filter(city -> city.isMultipleDeliveries() || !hasMadeDelivery(player, city))
                .filter(city -> city.getValue() <= breedingValue + certificates)
                .map(city -> new PossibleDelivery(city, Math.max(0, city.getValue() - breedingValue)))
                .collect(Collectors.toSet());
    }

    private boolean hasMadeDelivery(Player player, City city) {
        return cities.get(city).contains(player);
    }

    @Value
    public static class PossibleDelivery {
        City city;
        int certificates;
    }

    public static abstract class Space {

        final Station station;
        final Set<Space> next;
        final Set<Space> previous = new HashSet<>();

        private Space(Station station, Collection<Space> next) {
            this.station = station;
            this.next = new HashSet<>(next);
        }

        public Optional<Station> getStation() {
            return Optional.ofNullable(station);
        }

        public Set<Space> getNext() {
            return Collections.unmodifiableSet(next);
        }

        public Set<Space> getPrevious() {
            return Collections.unmodifiableSet(previous);
        }

        @Getter
        @ToString
        public static class NumberedSpace extends Space {

            @Getter
            private final int number;

            private NumberedSpace(int number, @NonNull Collection<Space> next) {
                this(number, null, next);
            }

            private NumberedSpace(int number, Station station, @NonNull Collection<Space> next) {
                super(station, next);
                this.number = number;
            }
        }

        @ToString
        public static final class StartSpace extends NumberedSpace {

            private StartSpace(@NonNull NumberedSpace next) {
                super(0, Collections.singleton(next));
            }
        }

        @ToString
        public static final class TurnoutSpace extends Space {

            public TurnoutSpace(@NonNull Space previous, @NonNull Space next, @NonNull Station station) {
                super(station, Collections.singleton(previous));
                this.previous.add(next);
            }
        }

        @ToString
        public static final class EndSpace extends NumberedSpace {
            public EndSpace(int number, @NonNull Station station) {
                super(number, station, Collections.emptySet());
            }
        }
    }
}
