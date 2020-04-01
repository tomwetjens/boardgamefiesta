package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            // TODO Set signals
            Space.NumberedSpace current = new Space.NumberedSpace(false, number, Collections.singleton(last));

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

    EngineMove moveEngineForward(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        return moveEngine(player, to, atLeast, atMost, Space::getNext);
    }

    EngineMove moveEngineBackwards(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        return moveEngine(player, to, atLeast, atMost, Space::getPrevious);
    }

    private EngineMove moveEngine(@NonNull Player player, @NonNull RailroadTrack.@NonNull Space to, int atLeast, int atMost, Function<Space, Set<Space>> direction) {
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

        Set<ReachableSpace> reachableSpaces = reachableSpaces(from, from, atLeast, atMost, 0, direction);

        ReachableSpace reachableSpace = reachableSpaces.stream()
                .filter(rs -> rs.space == to)
                .min(Comparator.comparingInt(ReachableSpace::getSteps))
                .orElseThrow(() -> new IllegalArgumentException("Space not reachable within " + atLeast + ".." + atMost + " steps"));

        currentSpaces.put(player, to);

        ImmediateActions immediateActions = to.getStation()
                .filter(station -> !station.hasUpgraded(player))
                .map(station -> ImmediateActions.of(PossibleAction.optional(Action.UpgradeStation.class)))
                .orElse(ImmediateActions.none());

        if (to == end) {
            immediateActions = immediateActions.andThen(PossibleAction.mandatory(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class));
        }

        return new EngineMove(immediateActions, reachableSpace.getSteps());
    }

    public Space getSpace(Station station) {
        return getSpaces()
                .filter(space -> space.getStation().map(s -> s == station).orElse(false))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Station not on track"));
    }

    private Stream<Space> getSpaces() {
        return getSpacesAfter(start);
    }

    private Stream<Space> getSpacesAfter(Space from) {
        return Stream.concat(Stream.of(from), from.getNext().stream());
    }

    @Value
    public static final class EngineMove {
        ImmediateActions immediateActions;
        int steps;
    }

    private Set<ReachableSpace> reachableSpaces(@NonNull Space from, @NonNull Space current, int atLeast, int atMost, int steps, Function<Space, Set<Space>> direction) {
        Set<ReachableSpace> reachable = new HashSet<>();

        boolean available = current != from && (current == start || playerAt(current).isEmpty());
        boolean possible = available && atLeast <= 1;

        if (possible) {
            reachable.add(new ReachableSpace(current, steps));
        }

        if (!available) {
            // Space is not empty, jump over
            reachable.addAll(direction.apply(current).stream()
                    .flatMap(next -> reachableSpaces(from, next, atLeast, atMost, steps, direction).stream())
                    .collect(Collectors.toSet()));
        } else if (possible && atMost > 1) {
            // Space is possible so count as step
            reachable.addAll(direction.apply(current).stream()
                    .flatMap(next -> reachableSpaces(from, next, Math.max(atLeast - 1, 0), atMost - 1, steps + 1, direction).stream())
                    .collect(Collectors.toSet()));
        }

        return reachable;
    }

    @Value
    private static class ReachableSpace {
        Space space;
        int steps;
    }

    private Optional<Player> playerAt(@NonNull Space space) {
        return currentSpaces.entrySet().stream()
                .filter(entry -> entry.getValue() == space)
                .map(Map.Entry::getKey)
                .findAny();
    }

    public Set<PossibleDelivery> possibleDeliveries(Player player, int breedingValue, int certificates) {
        return Arrays.stream(City.values())
                .filter(city -> city.isMultipleDeliveries() || !hasMadeDelivery(player, city))
                .filter(city -> city.getValue() <= breedingValue + certificates)
                .map(city -> new PossibleDelivery(city, Math.max(0, city.getValue() - breedingValue)))
                .collect(Collectors.toSet());
    }

    public Map<City, List<Player>> getCities() {
        return Collections.unmodifiableMap(cities);
    }

    private boolean hasMadeDelivery(Player player, City city) {
        return cities.get(city).contains(player);
    }

    ImmediateActions deliverToCity(Player player, City city) {
        if (!city.isMultipleDeliveries() && hasMadeDelivery(player, city)) {
            throw new IllegalStateException("Already delivered to city");
        }

        cities.get(city).add(player);

        // TODO Immediate actions when delivering to city
        return ImmediateActions.none();
    }

    int signalsPassed(Player player) {
        Space current = currentSpace(player);
        return (int) spacesWithSignalsPassed(current).distinct().count();
    }

    private Stream<Space> spacesWithSignalsPassed(Space current) {
        return Stream.concat(Stream.of(current), current.getPrevious().stream().flatMap(this::spacesWithSignalsPassed))
                .filter(Space::hasSignal);
    }

    @Value
    public static final class PossibleDelivery {
        City city;
        int certificates;
    }

    public static abstract class Space {

        private final boolean signal;
        private final Station station;
        private final Set<Space> next;
        protected final Set<Space> previous = new HashSet<>();

        private Space(boolean signal, Station station, Collection<Space> next) {
            this.signal = signal;
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

        public boolean hasSignal() {
            return signal;
        }

        public boolean isBefore(Space space) {
            return previous.stream().anyMatch(prev -> prev == space || prev.isBefore(space));
        }

        @Getter
        @ToString
        public static class NumberedSpace extends Space {

            @Getter
            private final int number;

            private NumberedSpace(boolean signal, int number, @NonNull Collection<Space> next) {
                this(signal, number, null, next);
            }

            private NumberedSpace(boolean signal, int number, Station station, @NonNull Collection<Space> next) {
                super(signal, station, next);
                this.number = number;
            }
        }

        @ToString
        public static final class StartSpace extends NumberedSpace {

            private StartSpace(@NonNull NumberedSpace next) {
                super(false, 0, Collections.singleton(next));
            }
        }

        @ToString
        public static final class TurnoutSpace extends Space {

            public TurnoutSpace(@NonNull Space previous, @NonNull Space next, @NonNull Station station) {
                super(false, station, Collections.singleton(previous));
                this.previous.add(next);
            }
        }

        @ToString
        public static final class EndSpace extends NumberedSpace {
            public EndSpace(int number, @NonNull Station station) {
                super(false, number, station, Collections.emptySet());
            }
        }
    }
}
