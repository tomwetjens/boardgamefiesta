package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RailroadTrack {

    private static final int MAX_SPACE = 39;

    private static final List<Integer> TURNOUTS = Arrays.asList(4, 7, 10, 13, 16, 21, 25, 29, 33);

    /**
     * Numbers of the spaces that have a signal between it and the next space
     */
    private static final List<Integer> SIGNALS = List.of(3, 4, 5, 7, 9, 10, 11, 13, 15, 16, 17);

    static final int MAX_HAND_VALUE = 28;
    static final int MIN_HAND_VALUE = 5;
    static final int MAX_CERTIFICATES = 6;

    private final List<Station> stations;

    @Getter
    private final Space start;
    @Getter
    private final Space end;
    private final Map<String, Space> spaces = new HashMap<>();
    private final List<Space.StationSpace> turnouts = new ArrayList<>(TURNOUTS.size());
    private final Map<Player, Space> engines = new HashMap<>();
    private final Map<City, List<Player>> deliveries;

    @Builder(access = AccessLevel.PACKAGE)
    private RailroadTrack(@NonNull List<Station> stations, @NonNull Map<City, List<Player>> deliveries) {
        this.stations = stations;
        this.deliveries = deliveries;

        end = new Space.StationSpace(Integer.toString(MAX_SPACE), stations.get(stations.size() - 1), Collections.emptySet(), Collections.emptySet());
        spaces.put(end.getName(), end);

        Space last = end;
        for (int number = MAX_SPACE - 1; number > 0; number--) {
            var current = new Space(Integer.toString(number), Collections.singleton(last), new HashSet<>());

            last.previous.add(current);
            last = current;

            spaces.put(current.getName(), current);
        }

        start = new Space("0", Collections.singleton(last), Collections.emptySet());
        last.previous.add(start);
        spaces.put(start.getName(), start);

        // Turn outs
        for (int i = 0; i < TURNOUTS.size(); i++) {
            var number = TURNOUTS.get(i);

            var previous = spaces.get(Integer.toString(number));
            var next = spaces.get(Integer.toString(number + 1));

            var turnout = new Space.StationSpace(previous.getName() + ".5", stations.get(i), Collections.singleton(previous), Collections.singleton(next));

            previous.next.add(turnout);
            next.previous.add(turnout);

            turnouts.add(turnout);
            spaces.put(turnout.getName(), turnout);
        }
    }

    RailroadTrack(@NonNull Set<Player> players, @NonNull Game.Options options, @NonNull Random random) {
        this(createInitialStations(options, random), createInitialCities());

        players.forEach(player -> engines.put(player, start));
    }

    private static List<Station> createInitialStations(@NonNull Game.Options options, Random random) {
        List<StationMaster> stationMasters = options.isStationMasterPromos()
                ? Stream.concat(StationMaster.ORIGINAL.stream(), StationMaster.PROMOS.stream()).collect(Collectors.toList())
                : new ArrayList<>(StationMaster.ORIGINAL);
        Collections.shuffle(stationMasters, random);

        return Arrays.asList(
                Station.initial(2, 1, Collections.singleton(DiscColor.WHITE), stationMasters.get(0)),
                Station.initial(2, 1, Collections.singleton(DiscColor.WHITE), stationMasters.get(1)),
                Station.initial(4, 2, Collections.singleton(DiscColor.WHITE), stationMasters.get(2)),
                Station.initial(4, 2, Collections.singleton(DiscColor.WHITE), stationMasters.get(3)),
                Station.initial(6, 3, Arrays.asList(DiscColor.WHITE, DiscColor.BLACK), stationMasters.get(4)),
                Station.initial(8, 5, Arrays.asList(DiscColor.WHITE, DiscColor.BLACK), null),
                Station.initial(7, 6, Arrays.asList(DiscColor.WHITE, DiscColor.BLACK), null),
                Station.initial(6, 7, Arrays.asList(DiscColor.WHITE, DiscColor.BLACK), null),
                Station.initial(5, 8, Arrays.asList(DiscColor.WHITE, DiscColor.BLACK), null),
                Station.initial(3, 9, Arrays.asList(DiscColor.WHITE, DiscColor.BLACK), null));
    }

    private static Map<City, List<Player>> createInitialCities() {
        var cities = new EnumMap<City, List<Player>>(City.class);
        for (City city : City.values()) {
            cities.put(city, new LinkedList<>());
        }
        return cities;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("stations", serializer.fromCollection(stations, Station::serialize))
                .add("cities", serializer.fromMap(deliveries, City::name, players -> serializer.fromStrings(players, Player::getName)))
                .add("currentSpaces", serializer.fromStringMap(engines, Player::getName, Space::getName))
                .build();
    }

    static RailroadTrack deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        var railroadTrack = new RailroadTrack(
                jsonObject.getJsonArray("stations").stream()
                        .map(JsonValue::asJsonObject)
                        .map(obj -> Station.deserialize(playerMap, obj))
                        .collect(Collectors.toList()),
                JsonDeserializer.forObject(jsonObject.getJsonObject("cities"))
                        .asMap(City::valueOf, jsonValue -> jsonValue.asJsonArray().getValuesAs(JsonString::getString).stream()
                                .map(playerMap::get).collect(Collectors.toList())));

        railroadTrack.engines.putAll(JsonDeserializer.forObject(jsonObject.getJsonObject("currentSpaces"))
                .asStringMap(playerMap::get, railroadTrack::getSpace));

        return railroadTrack;
    }

    public List<Station> getStations() {
        return Collections.unmodifiableList(stations);
    }

    public Space getSpace(String name) {
        var space = spaces.get(name);
        if (space == null) {
            throw new GWTException(GWTError.NO_SUCH_SPACE);
        }
        return space;
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(engines.keySet());
    }

    public Space currentSpace(Player player) {
        return engines.getOrDefault(player, start);
    }

    public Station currentStation(Player player) {
        var space = currentSpace(player);
        if (!(space instanceof Space.StationSpace)) {
            throw new GWTException(GWTError.NOT_AT_STATION);
        }
        return ((Space.StationSpace) space).getStation();
    }

    EngineMove moveEngineForward(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        return moveEngine(player, to, atLeast, atMost, Space::getNext);
    }

    EngineMove moveEngineBackwards(@NonNull Player player, @NonNull Space to, int atLeast, int atMost) {
        return moveEngine(player, to, atLeast, atMost, Space::getPrevious);
    }

    private EngineMove moveEngine(@NonNull Player player, @NonNull Space to, int atLeast, int atMost, Function<Space, Set<Space>> direction) {
        if (atLeast < 0 || atLeast > 6) {
            throw new IllegalArgumentException("Must move at least 0..6, but was: " + atLeast);
        }

        if (atMost < 1) {
            throw new IllegalArgumentException("Must be able to move >=1");
        }

        if (to != start && playerAt(to).isPresent()) {
            throw new GWTException(GWTError.ALREADY_PLAYER_ON_SPACE);
        }

        var from = currentSpace(player);

        if (to == from) {
            throw new GWTException(GWTError.ALREADY_AT_SPACE);
        }

        Set<ReachableSpace> reachableSpaces = reachableSpaces(from, from, atLeast, atMost, 0, direction);

        ReachableSpace reachableSpace = reachableSpaces.stream()
                .filter(rs -> rs.space == to)
                .min(Comparator.comparingInt(ReachableSpace::getSteps))
                .orElseThrow(() -> new GWTException(GWTError.SPACE_NOT_REACHABLE));

        engines.put(player, to);

        var immediateActions = ImmediateActions.none();

        if (to instanceof Space.StationSpace) {
            var station = ((Space.StationSpace) to).getStation();

            if (!station.hasUpgraded(player)) {
                immediateActions = ImmediateActions.of(PossibleAction.optional(Action.UpgradeStation.class));
            }

            if (to == end) {
                immediateActions = immediateActions.andThen(PossibleAction.mandatory(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class));
            }
        }

        return new EngineMove(immediateActions, reachableSpace.getSteps());
    }

    public Space getSpace(Station station) {
        return turnouts.stream()
                .filter(space -> space.getStation() == station)
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.STATION_NOT_ON_TRACK));
    }

    int numberOfUpgradedStations(Player player) {
        return (int) stations.stream().filter(station -> station.hasUpgraded(player)).count();
    }

    @Value
    public static class EngineMove {
        ImmediateActions immediateActions;
        int steps;
    }

    public Set<Space> reachableSpacesForward(@NonNull Space from, int atLeast, int atMost) {
        return reachableSpaces(from, from, atLeast, atMost, 0, Space::getNext).stream()
                .map(ReachableSpace::getSpace)
                .collect(Collectors.toSet());
    }

    public Set<Space> reachableSpacesBackwards(@NonNull Space from, int atLeast, int atMost) {
        return reachableSpaces(from, from, atLeast, atMost, 0, Space::getPrevious).stream()
                .map(ReachableSpace::getSpace)
                .collect(Collectors.toSet());
    }

    private Set<ReachableSpace> reachableSpaces(@NonNull Space from, @NonNull Space current, int atLeast, int atMost, int steps, Function<Space, Set<Space>> direction) {
        Set<ReachableSpace> reachable = new HashSet<>();

        boolean available = current != from && (current == start || playerAt(current).isEmpty());
        boolean possible = available && atLeast <= 1;

        if (possible) {
            reachable.add(new ReachableSpace(current, steps + 1));
        }

        if (!available) {
            // Space is not empty, jump over
            reachable.addAll(direction.apply(current).stream()
                    .flatMap(next -> reachableSpaces(from, next, atLeast, atMost, steps, direction).stream())
                    .collect(Collectors.toSet()));
        } else if (atMost > 1) {
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
        return engines.entrySet().stream()
                .filter(entry -> entry.getValue() == space)
                .map(Map.Entry::getKey)
                .findAny();
    }

    Set<PossibleDelivery> possibleDeliveries(Player player, int handValue, int certificates) {
        int signalsPassed = signalsPassed(player);

        return Arrays.stream(City.values())
                .filter(city -> city.isMultipleDeliveries() || !hasMadeDelivery(player, city))
                .filter(city -> city.getValue() <= handValue + certificates)
                .map(city -> new PossibleDelivery(city, Math.max(0, city.getValue() - handValue), handValue - city.getSignals() + signalsPassed))
                .collect(Collectors.toSet());
    }

    Set<PossibleDelivery> possibleExtraordinaryDeliveries(Player player, int lastEngineMove) {
        return Arrays.stream(City.values())
                .filter(city -> city.isMultipleDeliveries() || !hasMadeDelivery(player, city))
                .filter(city -> city.getValue() <= lastEngineMove)
                .map(city -> new PossibleDelivery(city, 0, 0))
                .collect(Collectors.toSet());
    }

    public Map<City, List<Player>> getDeliveries() {
        return Collections.unmodifiableMap(deliveries);
    }

    private boolean hasMadeDelivery(Player player, City city) {
        return deliveries.computeIfAbsent(city, k -> new LinkedList<>()).contains(player);
    }

    ImmediateActions deliverToCity(Player player, City city, Game game) {
        if (!city.isMultipleDeliveries() && hasMadeDelivery(player, city)) {
            throw new GWTException(GWTError.ALREADY_DELIVERED_TO_CITY);
        }

        deliveries.computeIfAbsent(city, k -> new LinkedList<>()).add(player);

        switch (city) {
            case COLORADO_SPRINGS:
            case ALBUQUERQUE:
                if (hasMadeDelivery(player, City.SANTA_FE) && !game.getObjectiveCards().isEmpty()) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(City.SANTA_FE.name(), city.name()));
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
            case SANTA_FE:
                ImmediateActions immediateActions = ImmediateActions.none();

                if (hasMadeDelivery(player, City.COLORADO_SPRINGS) && !game.getObjectiveCards().isEmpty()) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(City.COLORADO_SPRINGS.name(), city.name()));
                    immediateActions = ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }

                if (hasMadeDelivery(player, City.ALBUQUERQUE) && game.getObjectiveCards().getAvailable().size() > 1) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(city.name(), City.ALBUQUERQUE.name()));
                    immediateActions = immediateActions.andThen(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }

                return immediateActions;
            case TOPEKA:
                if (hasMadeDelivery(player, City.WICHITA) && !game.getObjectiveCards().isEmpty()) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(city.name(), City.WICHITA.name()));
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
            case WICHITA:
                if (hasMadeDelivery(player, City.TOPEKA) && !game.getObjectiveCards().isEmpty()) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(City.TOPEKA.name(), city.name()));
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
        }

        return ImmediateActions.none();
    }

    int signalsPassed(Player player) {
        var current = Math.ceil(Float.parseFloat(currentSpace(player).getName()));
        return (int) SIGNALS.stream().takeWhile(signal -> signal < current).count();
    }

    Score score(Player player) {
        return new Score(Map.of(ScoreCategory.CITIES, scoreDeliveries(player),
                ScoreCategory.STATIONS, scoreStations(player)));
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

        if (hasMadeDelivery(player, City.WICHITA) && hasMadeDelivery(player, City.COLORADO_SPRINGS)) {
            result -= 1;
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
        return (int) deliveries.get(city).stream().filter(p -> p == player).count();
    }

    @Value
    public static class PossibleDelivery {
        City city;
        int certificates;
        int reward;
    }

    public static class Space {

        @Getter
        private final String name;
        final Set<Space> next;
        final Set<Space> previous;

        private Space(String name, Set<Space> next, Set<Space> previous) {
            this.name = name;
            this.next = new HashSet<>(next);
            this.previous = new HashSet<>(previous);
        }

        public Set<Space> getNext() {
            return Collections.unmodifiableSet(next);
        }

        public Set<Space> getPrevious() {
            return Collections.unmodifiableSet(previous);
        }

        public boolean isAfter(Space space) {
            return previous.stream().anyMatch(prev -> prev == space || prev.isAfter(space));
        }

        private static final class StationSpace extends Space {

            @Getter
            private final Station station;

            private StationSpace(String name, @NonNull Station station, @NonNull Set<Space> previous, @NonNull Set<Space> next) {
                super(name, next, previous);
                this.station = station;
            }

        }
    }
}
