package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

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
    private final Map<Player, Space> playerSpaces = new EnumMap<>(Player.class);

    public RailroadTrack(@NonNull Collection<Player> players, @NonNull Random random) {
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

        players.forEach(player -> playerSpaces.put(player, start));
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
        return Collections.unmodifiableSet(playerSpaces.keySet());
    }

    public Space current(Player player) {
        return playerSpaces.get(player);
    }

    public ImmediateActions moveEngineForward(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        if (atLeast < 0 || atLeast > 6) {
            throw new IllegalArgumentException("Must move at least 0..6, but was: " + atLeast);
        }

        if (atMost < 0 || atMost > 6) {
            throw new IllegalArgumentException("Must move at most 0..6, but was: " + atMost);
        }

        if (playerAt(to).isPresent()) {
            throw new IllegalStateException("Another player already on space");
        }

        Space from = current(player);
        Set<Space> reachable = reachableSpacesForward(player, from, atLeast, atMost);

        if (!reachable.contains(to)) {
            throw new IllegalArgumentException("Space not reachable within " + atLeast + "src/test" + atMost + " steps");
        }

        playerSpaces.put(player, to);

        return to.getStation()
                .filter(station -> !station.hasUpgraded(player))
                .map(station -> ImmediateActions.of(PossibleAction.optional(UpgradeStation.class)))
                .orElse(ImmediateActions.none());
    }

    public Set<Space> reachableSpacesForward(@NonNull Player player, @NonNull Space from, int atLeast, int atMost) {
        Set<Space> reachable = new HashSet<>();

        Optional<Player> playerOnSpace = playerAt(from);
        Optional<Player> otherPlayerOnSpace = playerOnSpace.filter(p -> p != player);

        if (atLeast == 0 && otherPlayerOnSpace.isEmpty()) {
            reachable.add(from);
        }

        if (otherPlayerOnSpace.isPresent()) {
            reachable.addAll(from.next.stream()
                    .flatMap(next -> reachableSpacesForward(player, next, atLeast, atMost).stream())
                    .collect(Collectors.toSet()));
        } else if (atMost > 0) {
            reachable.addAll(from.next.stream()
                    .flatMap(next -> reachableSpacesForward(player, next, Math.max(atLeast - 1, 0), atMost - 1).stream())
                    .collect(Collectors.toSet()));
        }

        return reachable;
    }

    public Set<Space> reachableSpacesBackwards(@NonNull Player player, @NonNull Space from, int atLeast, int atMost) {
        Set<Space> reachable = new HashSet<>();

        Optional<Player> playerOnSpace = playerAt(from);
        Optional<Player> otherPlayerOnSpace = playerOnSpace.filter(p -> p != player);

        if (atLeast == 0 && otherPlayerOnSpace.isEmpty()) {
            reachable.add(from);
        }

        if (otherPlayerOnSpace.isPresent()) {
            reachable.addAll(from.previous.stream()
                    .flatMap(previous -> reachableSpacesBackwards(player, previous, atLeast, atMost).stream())
                    .collect(Collectors.toSet()));
        } else if (atMost > 0) {
            reachable.addAll(from.previous.stream()
                    .flatMap(previous -> reachableSpacesBackwards(player, previous, Math.max(atLeast - 1, 0), atMost - 1).stream())
                    .collect(Collectors.toSet()));
        }

        return reachable;
    }

    private Optional<Player> playerAt(@NonNull Space space) {
        return playerSpaces.entrySet().stream()
                .filter(entry -> entry.getValue() == space)
                .map(Map.Entry::getKey)
                .findAny();
    }

    public ImmediateActions moveEngineBackwards(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        if (atLeast < 0 || atLeast > 6) {
            throw new IllegalArgumentException("Must move at least 0..6, but was: " + atLeast);
        }

        if (atMost < 0 || atMost > 6) {
            throw new IllegalArgumentException("Must move at most 0..6, but was: " + atMost);
        }

        // TODO Allow multiple players on start space
        if (playerAt(to).isPresent()) {
            throw new IllegalStateException("Another player already on space");
        }

        Space from = current(player);
        Set<Space> reachable = reachableSpacesBackwards(player, from, atLeast, atMost);

        if (!reachable.contains(to)) {
            throw new IllegalArgumentException("Space not reachable within " + atLeast + "src/test" + atMost + " steps");
        }

        playerSpaces.put(player, to);

        return to.getStation()
                .filter(station -> !station.hasUpgraded(player))
                .map(station -> ImmediateActions.of(PossibleAction.optional(UpgradeStation.class)))
                .orElse(ImmediateActions.none());
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

        public static class NumberedSpace extends Space {

            @Getter
            private final int number;

            private NumberedSpace(int number, @NonNull Collection<Space> next) {
                this(number, null, next);
            }

            private NumberedSpace(int number, @NonNull Station station, @NonNull Collection<Space> next) {
                super(station, next);
                this.number = number;
            }

            @Override
            public String toString() {
                return "Space{" + number + '}';
            }
        }

        public static final class StartSpace extends NumberedSpace {

            private StartSpace(@NonNull NumberedSpace next) {
                super(0, Collections.singleton(next));
            }
        }

        public static final class TurnoutSpace extends Space {

            public TurnoutSpace(@NonNull Space previous, @NonNull Space next, @NonNull Station station) {
                super(station, Collections.singleton(previous));
                this.previous.add(next);
            }
        }

        public static final class EndSpace extends NumberedSpace {
            public EndSpace(int number, @NonNull Station station) {
                super(number, station, Collections.emptySet());
            }
        }
    }

    public static final class UpgradeStation extends Action {

        @Override
        public ImmediateActions perform(@NonNull Game game) {
            Space current = game.getRailroadTrack().current(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new IllegalStateException("Not at station"));

            return station.upgrade(game);
        }
    }

    @AllArgsConstructor
    public static final class AppointStationMaster extends Action {
        Worker worker;

        @Override
        public ImmediateActions perform(@NonNull Game game) {
            Space current = game.getRailroadTrack().current(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new IllegalStateException("Not at station"));

            return station.appointStationMaster(game, worker);
        }
    }
}
