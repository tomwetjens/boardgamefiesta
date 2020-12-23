package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RailroadTrack {

    /**
     * Numbers of the spaces that have a signal between it and the next space
     */
    private static final List<Integer> SIGNALS = List.of(3, 4, 5, 7, 9, 10, 11, 13, 15, 16, 17);

    private static final List<Station> STATIONS = List.of(
            new Station(2, 1, EnumSet.of(DiscColor.WHITE)), // 4.5
            new Station(2, 1, EnumSet.of(DiscColor.WHITE)), // 7.5
            new Station(4, 2, EnumSet.of(DiscColor.WHITE)), // 10.5
            new Station(4, 2, EnumSet.of(DiscColor.WHITE)), // 13.5
            new Station(6, 3, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)), // 16.5
            new Station(8, 5, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)), // 21.5
            new Station(7, 6, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)), // 25.5
            new Station(6, 7, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)), // 29.5
            new Station(5, 8, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)), // 33.5
            new Station(3, 9, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)) // 39
    );

    private static final int MAX_SPACE = 39;
    private static final Space START;
    private static final Space END;
    private static final Map<String, Space> SPACES = new HashMap<>();

    static {
        END = new Space.StationSpace(Integer.toString(MAX_SPACE), STATIONS.get(9), Collections.emptySet(), Collections.emptySet());
        SPACES.put(END.getName(), END);

        Space last = END;
        for (int number = MAX_SPACE - 1; number > 0; number--) {
            var current = new Space(Integer.toString(number), Collections.singleton(last), new HashSet<>());

            last.previous.add(current);
            last = current;

            SPACES.put(current.getName(), current);
        }

        START = new Space("0", Collections.singleton(last), Collections.emptySet());
        last.previous.add(START);
        SPACES.put(START.getName(), START);

        Stream.of(
                turnout(STATIONS.get(0), SPACES.get("4"), SPACES.get("5")),
                turnout(STATIONS.get(1), SPACES.get("7"), SPACES.get("8")),
                turnout(STATIONS.get(2), SPACES.get("10"), SPACES.get("11")),
                turnout(STATIONS.get(3), SPACES.get("13"), SPACES.get("14")),
                turnout(STATIONS.get(4), SPACES.get("16"), SPACES.get("17")),
                turnout(STATIONS.get(5), SPACES.get("21"), SPACES.get("22")),
                turnout(STATIONS.get(6), SPACES.get("25"), SPACES.get("26")),
                turnout(STATIONS.get(7), SPACES.get("29"), SPACES.get("30")),
                turnout(STATIONS.get(8), SPACES.get("33"), SPACES.get("34"))
        ).forEach(turnout -> {
            turnout.previous.forEach(previous -> previous.next.add(turnout));
            turnout.next.forEach(next -> next.previous.add(turnout));

            SPACES.put(turnout.getName(), turnout);
        });
    }

    @Builder.Default
    private final Map<Player, Space> engines = new HashMap<>();

    @Builder.Default
    private final Map<City, List<Player>> deliveries = new EnumMap<>(City.class);

    @Builder.Default
    private final Map<Station, StationMaster> stationMasters = new HashMap<>();
    @Builder.Default
    private final Map<Station, Worker> workers = new HashMap<>();
    @Builder.Default
    private final Map<Station, Set<Player>> upgrades = new HashMap<>();

    private static Space.StationSpace turnout(Station station, Space previous, Space next) {
        return new Space.StationSpace(previous.getName() + ".5", station, Set.of(previous), Set.of(next));
    }

    static RailroadTrack initial(@NonNull Set<Player> players, @NonNull Game.Options options, @NonNull Random random) {
        var engines = players.stream().collect(Collectors.toMap(Function.identity(), player -> START));

        var stationMastersSet = createStationMastersSet(options, random);
        var stationMasters = new HashMap<Station, StationMaster>();

        STATIONS.forEach(station -> {
            var stationMaster = stationMastersSet.poll();
            if (stationMaster != null) {
                stationMasters.put(station, stationMaster);
            }
        });

        return new RailroadTrack(engines, new EnumMap<>(City.class), stationMasters, new HashMap<>(), new HashMap<>());
    }

    private static Queue<StationMaster> createStationMastersSet(@NonNull Game.Options options, Random random) {
        var stationMasters = options.isStationMasterPromos()
                ? Stream.concat(StationMaster.ORIGINAL.stream(), StationMaster.PROMOS.stream()).collect(Collectors.toCollection(LinkedList::new))
                : new LinkedList<>(StationMaster.ORIGINAL);
        Collections.shuffle(stationMasters, random);
        return stationMasters;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("stations", serializer.fromCollection(STATIONS, station ->
                        factory.createObjectBuilder()
                                .add("players", JsonSerializer.forFactory(factory).fromStrings(upgrades.getOrDefault(station, Collections.emptySet()), Player::getName))
                                .add("stationMaster", Optional.ofNullable(stationMasters.get(station)).map(StationMaster::name).orElse(null))
                                .add("worker", Optional.ofNullable(workers.get(station)).map(Worker::name).orElse(null))
                                .build()))
                .add("cities", serializer.fromMap(deliveries, City::name, players -> serializer.fromStrings(players, Player::getName)))
                .add("currentSpaces", serializer.fromStringMap(engines, Player::getName, Space::getName))
                .build();
    }

    static RailroadTrack deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        var engines = JsonDeserializer.forObject(jsonObject.getJsonObject("currentSpaces"))
                .asStringMap(playerMap::get, SPACES::get);

        var deliveries = JsonDeserializer.forObject(jsonObject.getJsonObject("cities"))
                .asMap(City::valueOf, jsonValue -> jsonValue.asJsonArray().getValuesAs(JsonString::getString).stream()
                        .map(playerMap::get).collect(Collectors.toList()));

        var upgrades = new HashMap<Station, Set<Player>>();
        var stationMasters = new HashMap<Station, StationMaster>();
        var workers = new HashMap<Station, Worker>();

        var stationsArray = jsonObject.getJsonArray("stations");
        for (var i = 0; i < stationsArray.size(); i++) {
            var station = STATIONS.get(i);
            var obj = stationsArray.get(i).asJsonObject();

            upgrades.put(station, obj.getJsonArray("players").stream()
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .map(playerMap::get)
                    .collect(Collectors.toCollection(HashSet::new)));

            if (obj.getString("stationMaster") != null) {
                stationMasters.put(station, StationMaster.valueOf(obj.getString("stationMaster")));
            }
            if (obj.getString("worker") != null) {
                workers.put(station, Worker.valueOf(obj.getString("worker")));
            }
        }

        return new RailroadTrack(engines, deliveries, stationMasters, workers, upgrades);
    }

    public Station getStation(Space space) {
        if (!(space instanceof Space.StationSpace)) {
            throw new GWTException(GWTError.STATION_NOT_ON_TRACK);
        }
        return ((Space.StationSpace) space).getStation();
    }

    public Space getSpace(String name) {
        var space = SPACES.get(name);
        if (space == null) {
            throw new GWTException(GWTError.NO_SUCH_SPACE);
        }
        return space;
    }

    public Space getStart() {
        return START;
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(engines.keySet());
    }

    public Space currentSpace(Player player) {
        return engines.getOrDefault(player, START);
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

        if (to != START && playerAt(to).isPresent()) {
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

            if (!hasUpgraded(station, player)) {
                immediateActions = ImmediateActions.of(PossibleAction.optional(Action.UpgradeStation.class));
            }

            if (to == END) {
                immediateActions = immediateActions.andThen(PossibleAction.mandatory(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class));
            }
        }

        return new EngineMove(immediateActions, reachableSpace.getSteps());
    }

    public Space getSpace(Station station) {
        return SPACES.values().stream()
                .filter(space -> space instanceof Space.StationSpace && ((Space.StationSpace) space).getStation() == station)
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.STATION_NOT_ON_TRACK));
    }

    int numberOfUpgradedStations(Player player) {
        return (int) upgrades.values().stream().filter(players -> players.contains(player)).count();
    }

    public Optional<StationMaster> getStationMaster(Station station) {
        return Optional.ofNullable(stationMasters.get(station));
    }

    public Optional<Worker> getWorker(Station station) {
        return Optional.ofNullable(workers.get(station));
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

        boolean available = current != from && (current == START || playerAt(current).isEmpty());
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
        return upgrades.entrySet().stream()
                .filter(entry -> entry.getValue().contains(player))
                .map(Map.Entry::getKey)
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
        return (int) deliveries.computeIfAbsent(city, k -> new LinkedList<>()).stream().filter(p -> p == player).count();
    }

    /**
     * @return the index of the station can be used as identifier
     */
    public List<Station> getStations() {
        return STATIONS;
    }

    boolean hasUpgraded(@NonNull Station station, @NonNull Player player) {
        return getUpgradedBy(station).contains(player);
    }

    ImmediateActions upgradeStation(@NonNull Game game, @NonNull Station station) {
        var player = game.getCurrentPlayer();
        var upgradedBy = getUpgradedBy(station);

        if (upgradedBy.contains(player)) {
            throw new GWTException(GWTError.ALREADY_UPGRADED_STATION);
        }

        var playerState = game.currentPlayerState();
        playerState.payDollars(station.getCost());
        playerState.rememberLastUpgradedStation(station);

        upgradedBy.add(player);

        ImmediateActions placeDiscActions = game.removeDisc(station.getDiscColors());

        var stationMaster = stationMasters.get(station);

        if (stationMaster != null) {
            game.fireEvent(player, GWTEvent.Type.MAY_APPOINT_STATION_MASTER, List.of(stationMaster.name()));
            return placeDiscActions.andThen(PossibleAction.optional(Action.AppointStationMaster.class));
        }
        return placeDiscActions;
    }

    public Set<Player> getUpgradedBy(@NonNull Station station) {
        return upgrades.computeIfAbsent(station, k -> new HashSet<>());
    }

    ImmediateActions appointStationMaster(@NonNull Game game, @NonNull Station station, @NonNull Worker worker) {
        game.currentPlayerState().removeWorker(worker);

        workers.put(station, worker);

        StationMaster reward = stationMasters.get(station);

        game.currentPlayerState().addStationMaster(reward);
        stationMasters.remove(station);

        return reward.activate(game);
    }

    void downgradeStation(@NonNull Game game, @NonNull Station station) {
        var player = game.getCurrentPlayer();
        var upgradedBy = getUpgradedBy(station);

        if (!upgradedBy.remove(player)) {
            throw new GWTException(GWTError.STATION_NOT_UPGRADED_BY_PLAYER);
        }
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

        @Getter(AccessLevel.PRIVATE)
        protected final Set<Space> next;

        @Getter(AccessLevel.PRIVATE)
        protected final Set<Space> previous;

        private Space(String name, Set<Space> next, Set<Space> previous) {
            this.name = name;
            this.next = new HashSet<>(next);
            this.previous = new HashSet<>(previous);
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
