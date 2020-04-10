package com.wetjens.gwt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
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

@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RailroadTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_SPACE = 39;

    private static final List<Integer> TURNOUTS = Arrays.asList(4, 7, 10, 13, 16, 21, 25, 29, 33);
    private static final List<Integer> SIGNALS = Arrays.asList(4, 5, 6, 8, 10, 11, 12, 14, 16, 17, 18);

    static final int MAX_HAND_VALUE = 28;
    static final int MIN_HAND_VALUE = 5;
    static final int MAX_CERTIFICATES = 6;

    private final List<Station> stations;

    @Getter
    private final Space.StartSpace start;
    @Getter
    private final Space.EndSpace end;
    private final Map<Integer, Space.NumberedSpace> normalSpaces = new HashMap<>();
    private final List<Space.TurnoutSpace> turnouts = new ArrayList<>(TURNOUTS.size());
    private final Map<Player, Space> currentSpaces = new HashMap<>();

    private final Map<City, List<Player>> cities;

    RailroadTrack(@NonNull Collection<Player> players, @NonNull Random random) {
        this.stations = createStations(random);
        this.cities = createEmptyCities();

        this.end = new Space.EndSpace(MAX_SPACE, stations.get(stations.size() - 1));

        Space.NumberedSpace last = end;
        for (int number = MAX_SPACE - 1; number > 0; number--) {
            Space.NumberedSpace current = new Space.NumberedSpace(SIGNALS.contains(number), number, Collections.singleton(last));

            last.previous.add(current);
            last = current;

            normalSpaces.put(number, current);
        }

        this.start = new Space.StartSpace(last);
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
    }

    private static List<Station> createStations(@NonNull Random random) {
        List<StationMaster> stationMasters = Arrays.asList(StationMaster.values());
        Collections.shuffle(stationMasters, random);

        return Arrays.asList(
                new Station(2, 1, DiscColor.WHITE, stationMasters.get(0)),
                new Station(2, 1, DiscColor.WHITE, stationMasters.get(1)),
                new Station(4, 2, DiscColor.WHITE, stationMasters.get(2)),
                new Station(4, 2, DiscColor.WHITE, stationMasters.get(3)),
                new Station(6, 3, DiscColor.BLACK, stationMasters.get(4)),
                new Station(8, 5, DiscColor.BLACK, null),
                new Station(7, 6, DiscColor.BLACK, null),
                new Station(6, 7, DiscColor.BLACK, null),
                new Station(5, 8, DiscColor.BLACK, null),
                new Station(3, 9, DiscColor.BLACK, null));
    }

    private static Map<City, List<Player>> createEmptyCities() {
        var cities = new EnumMap<City, List<Player>>(City.class);
        for (City city : City.values()) {
            cities.put(city, new LinkedList<>());
        }
        return cities;
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

    int numberOfUpgradedStations(Player player) {
        return (int) stations.stream().filter(station -> station.hasUpgraded(player)).count();
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
        return cities.computeIfAbsent(city, k -> new LinkedList<>()).contains(player);
    }

    ImmediateActions deliverToCity(Player player, City city) {
        if (!city.isMultipleDeliveries() && hasMadeDelivery(player, city)) {
            throw new IllegalStateException("Already delivered to city");
        }

        cities.computeIfAbsent(city, k -> new LinkedList<>()).add(player);

        switch (city) {
            case COLORADO_SPRINGS:
            case ALBUQUERQUE:
                if (hasMadeDelivery(player, City.SANTA_FE)) {
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
            case SANTA_FE:
                ImmediateActions immediateActions = ImmediateActions.none();

                if (hasMadeDelivery(player, City.COLORADO_SPRINGS)) {
                    immediateActions = ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }

                if (hasMadeDelivery(player, City.ALBUQUERQUE)) {
                    immediateActions = immediateActions.andThen(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }

                return immediateActions;
            case TOPEKA:
                if (hasMadeDelivery(player, City.WICHITA)) {
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
            case WICHITA:
                if (hasMadeDelivery(player, City.TOPEKA)) {
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
        }

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

    int score(Player player) {
        return scoreDeliveries(player) + scoreStations(player);
    }

    private int scoreStations(Player player) {
        return stations.stream()
                .filter(station -> station.hasUpgraded(player))
                .mapToInt(Station::getPoints)
                .sum();
    }

    private int scoreDeliveries(Player player) {
        int result = 0;

        result -= numberOfDeliveries(player, City.KANSAS_CITY) * 6;

        if (hasMadeDelivery(player, City.TOPEKA) && hasMadeDelivery(player, City.WICHITA)) {
            result -= 3;
        }

        if (hasMadeDelivery(player, City.ALBUQUERQUE) && hasMadeDelivery(player, City.EL_PASO)) {
            result += 6;
        }

        if (hasMadeDelivery(player, City.EL_PASO) && hasMadeDelivery(player, City.SAN_DIEGO)) {
            result += 8;
        }

        if (hasMadeDelivery(player, City.SAN_DIEGO) && hasMadeDelivery(player, City.SACRAMENTO)) {
            result += 4;
        }

        if (hasMadeDelivery(player, City.SACRAMENTO)) {
            result += 6;
        }

        result += numberOfDeliveries(player, City.SAN_FRANCISCO) * 9;

        return result;
    }

    int numberOfDeliveries(Player player, City city) {
        return (int) cities.get(city).stream().filter(p -> p == player).count();
    }

    @Value
    public static final class PossibleDelivery {
        City city;
        int certificates;
    }

    public static abstract class Space implements Serializable {

        private static final long serialVersionUID = 1L;

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

            private static final long serialVersionUID = 1L;

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
