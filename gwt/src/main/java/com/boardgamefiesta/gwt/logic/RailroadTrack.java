package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.function.BiFunction;
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
            new Station(3, 9, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)), // 39

            // Rails To The North expansion:
            new Station(7, 6, EnumSet.of(DiscColor.WHITE)), // green
            new Station(15, 8, EnumSet.of(DiscColor.WHITE, DiscColor.BLACK)) // red
    );

    private static final int MAX_SPACE = 39;
    private static final Map<String, Space> SPACES = new HashMap<>();
    private static final Map<String, Town> TOWNS = new HashMap<>();
    private static final Map<String, MediumTown> MEDIUM_TOWNS = new HashMap<>();
    private static final Map<City, BigTown> BIG_TOWNS = new HashMap<>();

    private static final BigTown MEMPHIS = Space.bigTown("MEM", Area.GREEN, City.MEMPHIS, RailroadTrack::firstPlayerGains2Dollars);
    private static final BigTown SAN_FRANCISCO = Space.bigTown("SFO", Area.BLUE, City.SAN_FRANCISCO);
    private static final BigTown DENVER = Space.bigTown("DEN", Area.BLUE, City.DENVER, RailroadTrack::pay1Or2Or3Dollars);
    private static final BigTown MILWAUKEE = Space.bigTown("MIL", Area.RED, City.MILWAUKEE);
    private static final BigTown GREEN_BAY = Space.bigTown("GBY", Area.RED, City.GREEN_BAY);
    private static final BigTown TORONTO = Space.bigTown("TOR", Area.RED, City.TORONTO);
    private static final BigTown MINNEAPOLIS = Space.bigTown("MIN", Area.RED, City.MINNEAPOLIS, RailroadTrack::pay1Or3Or4Dollars);
    private static final BigTown MONTREAL = Space.bigTown("MON", Area.RED, City.MONTREAL, RailroadTrack::pay1Or3Or4Dollars);

    private static final Space GREEN_STATION_TOWN = Space.stationTown("40", Area.GREEN, STATIONS.get(10));
    private static final Space RED_STATION_TOWN = Space.stationTown("41", Area.RED, STATIONS.get(11));

    private static final Space START = Space.numbered("0")
            .to(Space.smallTown("42", Area.GREEN)
                    .to(MEMPHIS)
                    .to(Space.mediumTown("43", Area.GREEN)));
    private static final Space END = START
            .next(Space.numbered("1")
                    .to(MEMPHIS
                            .to(GREEN_STATION_TOWN)
                            .to(Space.smallTown("44", Area.GREEN, RailroadTrack::gainExchangeToken))))
            .next(Space.numbered("2"))
            .next(Space.numbered("3"))
            .next(Space.numbered("4")
                    .to(MEMPHIS)
                    .to(Space.smallTown("45", Area.PURPLE, RailroadTrack::firstPlayerGains2Dollars)
                            .to(Space.mediumTown("46", Area.PURPLE))))
            .turnout("4.5", STATIONS.get(0), Space.numbered("5"))
            .next(Space.numbered("6")
                    .to(Space.smallTown("47", Area.BLUE)
                            .to(Space.smallTown("48", Area.BLUE, RailroadTrack::gainExchangeToken))
                            .to(DENVER)
                            .to(Space.smallTown("49", Area.BLUE)
                                    .to(SAN_FRANCISCO)))
                    .to(MILWAUKEE
                            .connect(Space.smallTown("50", Area.RED, RailroadTrack::firstPlayerGains2Dollars)
                                    .connect(GREEN_BAY
                                            .to(RED_STATION_TOWN)
                                            .connect(Space.smallTown("51", Area.RED, RailroadTrack::firstPlayerGains2Dollars)
                                                    .to(Space.mediumTown("52", Area.RED))
                                                    .connect(Space.smallTown("53", Area.RED, RailroadTrack::firstPlayerGains1Certificate)
                                                            .to(MINNEAPOLIS))
                                                    .connect(Space.smallTown("54", Area.RED, RailroadTrack::firstPlayerGains1Certificate)
                                                            .connect(TORONTO)
                                                            .to(MONTREAL)
                                                            .to(Space.mediumTown("55", Area.RED)))))
                                    .to(RED_STATION_TOWN))))
            .next(Space.numbered("7"))
            .turnout("7.5", STATIONS.get(1), Space.numbered("8"))
            .next(Space.numbered("9"))
            .next(Space.numbered("10")
                    .to(Space.smallTown("56", Area.TEAL, RailroadTrack::firstPlayerGainsExchangeToken)
                            .to(Space.mediumTown("57", Area.TEAL))))
            .turnout("10.5", STATIONS.get(2), Space.numbered("11"))
            .next(Space.numbered("12")
                    .to(TORONTO))
            .next(Space.numbered("13"))
            .turnout("13.5", STATIONS.get(3), Space.numbered("14"))
            .next(Space.numbered("15")
                    .to(Space.smallTown("58", Area.VIOLET, RailroadTrack::firstPlayerTakeObjectiveCard)
                            .to(Space.mediumTown("59", Area.VIOLET))))
            .next(Space.numbered("16"))
            .turnout("16.5", STATIONS.get(4), Space.numbered("17"))
            .next(Space.numbered("18"))
            .next(Space.numbered("19"))
            .next(Space.numbered("20"))
            .next(Space.numbered("21"))
            .turnout("21.5", STATIONS.get(5), Space.numbered("22"))
            .next(Space.numbered("23"))
            .next(Space.numbered("24"))
            .next(Space.numbered("25"))
            .turnout("25.5", STATIONS.get(6), Space.numbered("26"))
            .next(Space.numbered("27"))
            .next(Space.numbered("28"))
            .next(Space.numbered("29"))
            .turnout("29.5", STATIONS.get(7), Space.numbered("30"))
            .next(Space.numbered("31"))
            .next(Space.numbered("32"))
            .next(Space.numbered("33"))
            .turnout("33.5", STATIONS.get(8), Space.numbered("34"))
            .next(Space.numbered("35"))
            .next(Space.numbered("36"))
            .next(Space.numbered("37"))
            .next(Space.numbered("38"))
            .next(Space.turnout("39", STATIONS.get(9)));

    private static final EnumSet<City> ORIGINAL_CITY_STRIP = EnumSet.of(
            City.KANSAS_CITY,
            City.TOPEKA,
            City.WICHITA,
            City.COLORADO_SPRINGS,
            City.SANTA_FE,
            City.ALBUQUERQUE,
            City.EL_PASO,
            City.SAN_DIEGO,
            City.SACRAMENTO,
            City.SAN_FRANCISCO);

    private static final EnumSet<City> RTTN_CITY_STRIP = EnumSet.of(
            City.KANSAS_CITY,
            City.COLUMBIA,
            City.ST_LOUIS,
            City.CHICAGO,
            City.DETROIT,
            City.CLEVELAND,
            City.PITTSBURGH,
            City.NEW_YORK_CITY);

    @Builder.Default
    private final Set<City> cityStrip = ORIGINAL_CITY_STRIP;
    @Builder.Default
    private final Map<Player, Space> engines = new HashMap<>();
    @Builder.Default
    private final Map<City, List<Player>> deliveries = new EnumMap<>(City.class);
    @Builder.Default
    private final Map<Station, StationMaster> stationMasters = new HashMap<>();
    @Getter
    @Builder.Default
    private final Set<StationMaster> bonusStationMasters = new HashSet<>();
    @Builder.Default
    private final Map<Station, Worker> workers = new HashMap<>();
    @Builder.Default
    private final Map<Station, List<Player>> upgrades = new HashMap<>();
    @Builder.Default
    private final Map<MediumTown, MediumTownTile> mediumTownTiles = new HashMap<>();
    @Builder.Default
    private final Map<Town, List<Player>> branchlets = new HashMap<>();

    static RailroadTrack initial(@NonNull Set<Player> players, @NonNull Game.Options options, @NonNull Random random) {
        var engines = players.stream().collect(Collectors.toMap(Function.identity(), player -> START));

        var stationMastersPile = createStationMastersPile(options, random);
        var stationMasters = new HashMap<Station, StationMaster>();

        stationMasters.put(STATIONS.get(0), stationMastersPile.poll());
        stationMasters.put(STATIONS.get(1), stationMastersPile.poll());
        stationMasters.put(STATIONS.get(2), stationMastersPile.poll());
        stationMasters.put(STATIONS.get(3), stationMastersPile.poll());
        stationMasters.put(STATIONS.get(4), stationMastersPile.poll());

        if (options.isRailsToTheNorth()) {
            stationMasters.put(STATIONS.get(10), stationMastersPile.poll());
            stationMasters.put(STATIONS.get(11), stationMastersPile.poll());
        }

        var mediumTownTilesPile = MediumTownTile.shuffledPile(random);

        var mediumTownTiles = MEDIUM_TOWNS.values().stream()
                .collect(Collectors.toMap(Function.identity(), mediumTown -> mediumTownTilesPile.poll()));

        return new RailroadTrack(
                options.isRailsToTheNorth() ? RTTN_CITY_STRIP : ORIGINAL_CITY_STRIP,
                engines,
                new EnumMap<>(City.class),
                stationMasters,
                new HashSet<>(stationMastersPile),
                new HashMap<>(),
                new HashMap<>(),
                mediumTownTiles,
                new HashMap<>());
    }

    private static Queue<StationMaster> createStationMastersPile(@NonNull Game.Options options, Random random) {
        var stationMasters = options.isRailsToTheNorth() ? new LinkedList<>(StationMaster.RAILS_TO_THE_NORTH)
                : options.isStationMasterPromos() ? new LinkedList<>(StationMaster.WITH_PROMOS)
                : new LinkedList<>(StationMaster.ORIGINAL);
        Collections.shuffle(stationMasters, random);
        return stationMasters;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("stations", serializer.fromCollection(STATIONS, station ->
                        factory.createObjectBuilder()
                                .add("players", JsonSerializer.forFactory(factory).fromStrings(upgrades.getOrDefault(station, Collections.emptyList()), Player::getName))
                                .add("stationMaster", Optional.ofNullable(stationMasters.get(station)).map(StationMaster::name).orElse(null))
                                .add("worker", Optional.ofNullable(workers.get(station)).map(Worker::name).orElse(null))
                                .build()))
                .add("cities", serializer.fromMap(deliveries, City::name, players -> serializer.fromStrings(players, Player::getName)))
                .add("branchlets", serializer.fromMap(branchlets, Space::getName, players -> serializer.fromStrings(players, Player::getName)))
                .add("mediumTownTiles", serializer.fromStringMap(mediumTownTiles, MediumTown::getName, MediumTownTile::name))
                .add("currentSpaces", serializer.fromStringMap(engines, Player::getName, Space::getName))
                .add("bonusStationMasters", serializer.fromStrings(bonusStationMasters, StationMaster::name))
                .build();
    }

    static RailroadTrack deserialize(boolean railsToTheNorth, Map<String, Player> playerMap, JsonObject jsonObject) {
        var engines = JsonDeserializer.forObject(jsonObject.getJsonObject("currentSpaces"))
                .asStringMap(playerMap::get, SPACES::get);

        var deliveries = JsonDeserializer.forObject(jsonObject.getJsonObject("cities"))
                .asMap(City::valueOf, jsonValue -> jsonValue.asJsonArray().getValuesAs(JsonString::getString).stream()
                        .map(playerMap::get).collect(Collectors.toList()));

        Map<Town, List<Player>> branchlets = new HashMap<>();
        if (jsonObject.containsKey("branchlets")) {
            branchlets = JsonDeserializer.forObject(jsonObject.getJsonObject("branchlets"))
                    .asMap(TOWNS::get, jsonValue -> jsonValue.asJsonArray().getValuesAs(JsonString::getString).stream()
                            .map(playerMap::get).collect(Collectors.toList()));
        }

        var upgrades = new HashMap<Station, List<Player>>();
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
                    .collect(Collectors.toCollection(LinkedList::new)));

            if (obj.getString("stationMaster") != null) {
                stationMasters.put(station, StationMaster.valueOf(obj.getString("stationMaster")));
            }
            if (obj.getString("worker") != null) {
                workers.put(station, Worker.valueOf(obj.getString("worker")));
            }
        }

        var bonusStationMasters = new HashSet<StationMaster>();
        if (jsonObject.containsKey("bonusStationMasters")) {
            bonusStationMasters = jsonObject.getJsonArray("bonusStationMasters").stream()
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .map(StationMaster::valueOf)
                    .collect(Collectors.toCollection(HashSet::new));
        }

        Map<MediumTown, MediumTownTile> mediumTownTiles = new HashMap<>();
        if (jsonObject.containsKey("mediumTownTiles")) {
            mediumTownTiles = JsonDeserializer.forObject(jsonObject.getJsonObject("mediumTownTiles"))
                    .asStringMap(MEDIUM_TOWNS::get, MediumTownTile::valueOf);
        }

        return new RailroadTrack(
                railsToTheNorth ? RTTN_CITY_STRIP : ORIGINAL_CITY_STRIP,
                engines,
                deliveries,
                stationMasters,
                bonusStationMasters,
                workers,
                upgrades,
                mediumTownTiles,
                branchlets);
    }

    Optional<Station> getStation(Town town) {
        if (town instanceof StationTown) {
            return Optional.of(((StationTown) town).getStation());
        }
        return Optional.empty();
    }

    public Space getSpace(String name) {
        var space = SPACES.get(name);
        if (space == null) {
            throw new GWTException(GWTError.NO_SUCH_SPACE);
        }
        return space;
    }

    public Town getTown(String name) {
        var town = TOWNS.get(name);
        if (town == null) {
            throw new GWTException(GWTError.NO_SUCH_TOWN);
        }
        return town;
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
        if (!(space instanceof Space.Turnout)) {
            throw new GWTException(GWTError.NOT_AT_STATION);
        }
        return ((Space.Turnout) space).getStation();
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

        Set<ReachableSpace> reachableSpaces = reachableSpacesEngine(from, from, atLeast, atMost, 0, direction);

        ReachableSpace reachableSpace = reachableSpaces.stream()
                .filter(rs -> rs.space == to)
                .min(Comparator.comparingInt(ReachableSpace::getSteps))
                .orElseThrow(() -> new GWTException(GWTError.SPACE_NOT_REACHABLE));

        engines.put(player, to);

        var immediateActions = ImmediateActions.none();

        if (to instanceof Space.Turnout) {
            var station = ((Space.Turnout) to).getStation();

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
                .filter(space -> space instanceof Space.Turnout && ((Space.Turnout) space).getStation() == station)
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

    public Optional<MediumTownTile> getMediumTownTile(Town town) {
        if (town instanceof MediumTown) {
            return Optional.of(mediumTownTiles.get(town));
        }
        return Optional.empty();
    }

    int transportCosts(Player player, City city) {
        return transportCosts(city, signalsPassed(player));
    }

    private int transportCosts(City city, int passed) {
        // Rails To The North: find the space of the railroad track whose printed number equals the city value of the
        // city crest you just delivered to. Then calculate number of signals as usual.
        return Math.max(0, numberOfSignals(city.getValue()) - passed);
    }

    int numberOfAreas(Player player) {
        return (int) branchlets.entrySet().stream()
                .filter(entry -> entry.getValue().contains(player))
                .map(Map.Entry::getKey)
                .map(Town::getArea)
                .distinct()
                .count();
    }

    public Stream<Town> possibleTowns(Player player) {
        if (!isRailsToTheNorth()) {
            return Stream.empty();
        }
        return accessibleTowns(START, player, new HashSet<>(Set.of(START)), "")
                .filter(town -> !hasBranchlet(town, player));
    }

    private Stream<Town> accessibleTowns(Space from, Player player, Set<Space> visited, String indent) {
        return from.next.stream()
                .filter(next -> !visited.contains(next))
                .flatMap(next -> {
                    if (next instanceof Town) {
                        if (from instanceof Town || from == START || getCity(from).map(city -> hasMadeDelivery(player, city)).orElse(true)) {
                            var branchlet = hasBranchlet((Town) next, player);
                            if (branchlet) {
                                visited.add(next);
                                return Stream.concat(Stream.of((Town) next), accessibleTowns(next, player, visited, indent + "  "));
                            } else {
                                visited.add(next);
                                return Stream.of((Town) next);
                            }
                        } else {
                            return Stream.empty();
                        }
                    } else {
                        if (next.getName().equals("16")) {
                            // Early exit, no need to search after space 15
                            return Stream.empty();
                        }
                        visited.add(next);
                        return accessibleTowns(next, player, visited, indent + "  ");
                    }
                });
    }

    void takeBonusStationMaster(StationMaster stationMaster) {
        if (!bonusStationMasters.remove(stationMaster)) {
            throw new GWTException(GWTError.STATION_MASTER_NOT_AVAILABLE);
        }
    }

    public Collection<Town> getTowns() {
        return TOWNS.values();
    }

    @Value
    public static class EngineMove {
        ImmediateActions immediateActions;
        int steps;
    }

    public Set<Space> reachableSpacesForward(@NonNull Space from, int atLeast, int atMost) {
        return reachableSpacesEngine(from, from, atLeast, atMost, 0, Space::getNext).stream()
                .map(ReachableSpace::getSpace)
                .collect(Collectors.toSet());
    }

    public Set<Space> reachableSpacesBackwards(@NonNull Space from, int atLeast, int atMost) {
        return reachableSpacesEngine(from, from, atLeast, atMost, 0, Space::getPrevious).stream()
                .map(ReachableSpace::getSpace)
                .collect(Collectors.toSet());
    }

    private Set<ReachableSpace> reachableSpacesEngine(@NonNull Space from, @NonNull Space current, int atLeast, int atMost, int steps, Function<Space, Set<Space>> direction) {
        Set<ReachableSpace> reachable = new HashSet<>();

        boolean available = current != from && (current == START || playerAt(current).isEmpty());
        boolean possible = available && atLeast <= 1;

        if (possible) {
            reachable.add(new ReachableSpace(current, steps + 1));
        }

        if (!available) {
            // Space is not empty, jump over
            reachable.addAll(direction.apply(current).stream()
                    .filter(next -> !(next instanceof Town)) // Engine cannot move to the north
                    .flatMap(next -> reachableSpacesEngine(from, next, atLeast, atMost, steps, direction).stream())
                    .collect(Collectors.toSet()));
        } else if (atMost > 1) {
            // Space is possible so count as step
            reachable.addAll(direction.apply(current).stream()
                    .filter(next -> !(next instanceof Town)) // Engine cannot move to the north
                    .flatMap(next -> reachableSpacesEngine(from, next, Math.max(atLeast - 1, 0), atMost - 1, steps + 1, direction).stream())
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

        var cities = Arrays.stream(City.values())
                .filter(city -> canDeliver(player, city))
                .filter(city -> city.getValue() <= handValue + certificates)
                .collect(Collectors.toSet());

        return cities.stream()
                .map(city -> new PossibleDelivery(city, Math.max(0, city.getValue() - handValue), handValue - transportCosts(city, signalsPassed)))
                .collect(Collectors.toSet());
    }

    Set<PossibleDelivery> possibleExtraordinaryDeliveries(Player player, int lastEngineMove) {
        return Arrays.stream(City.values())
                .filter(city -> canDeliver(player, city))
                .filter(city -> city.getValue() <= lastEngineMove)
                .map(city -> new PossibleDelivery(city, 0, 0))
                .collect(Collectors.toSet());
    }

    private boolean canDeliver(Player player, City city) {
        return isAccessible(player, city)
                && (!hasMadeDelivery(player, city) || city.isMultipleDeliveries());
    }

    private boolean isAccessible(Player player, City city) {
        return cityStrip.contains(city) || hasBranchlet(BIG_TOWNS.get(city), player);
    }

    public Map<City, List<Player>> getDeliveries() {
        return Collections.unmodifiableMap(deliveries);
    }

    private Optional<City> getCity(Space space) {
        return cityStrip.stream()
                .filter(city -> Integer.toString(city.getValue()).equals(space.getName()))
                .findAny();
    }

    boolean hasMadeDelivery(Player player, City city) {
        return deliveries.computeIfAbsent(city, k -> new LinkedList<>()).contains(player);
    }

    ImmediateActions deliverToCity(Player player, City city, Game game) {
        if (!canDeliver(player, city)) {
            if (!isAccessible(player, city)) {
                throw new GWTException(GWTError.CITY_NOT_ACCESSIBLE);
            } else {
                throw new GWTException(GWTError.ALREADY_DELIVERED_TO_CITY);
            }
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
            case COLUMBIA:
                if (hasMadeDelivery(player, City.ST_LOUIS)) {
                    return ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class));
                }
                break;
            case ST_LOUIS:
                if (hasMadeDelivery(player, City.COLUMBIA)) {
                    return ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class));
                }
                break;
            case CHICAGO:
                if (hasMadeDelivery(player, City.DETROIT)) {
                    return ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class));
                }
                break;
            case DETROIT:
                if (hasMadeDelivery(player, City.CHICAGO)) {
                    return ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class));
                }
                break;
            case CLEVELAND:
                if (hasMadeDelivery(player, City.PITTSBURGH) && !game.getObjectiveCards().isEmpty()) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(City.PITTSBURGH.name(), city.name()));
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
            case PITTSBURGH:
                if (hasMadeDelivery(player, City.CLEVELAND) && !game.getObjectiveCards().isEmpty()) {
                    game.fireEvent(player, GWTEvent.Type.MUST_TAKE_OBJECTIVE_CARD, List.of(City.CLEVELAND.name(), city.name()));
                    return ImmediateActions.of(PossibleAction.mandatory(Action.TakeObjectiveCard.class));
                }
                break;
            case NEW_YORK_CITY:
                return ImmediateActions.of(PossibleAction.optional(Action.TakeBonusStationMaster.class));
            case MEMPHIS:
                return ImmediateActions.of(PossibleAction.any(Action.TakeObjectiveCard.class, Action.Gain2Dollars.class));
            case MILWAUKEE:
                return ImmediateActions.of(PossibleAction.any(Action.TakeObjectiveCard.class, Action.Gain3Dollars.class));
        }

        return ImmediateActions.none();
    }

    private boolean hasBranchlet(Town town, Player player) {
        return getBranchlets(town).contains(player);
    }

    int signalsPassed(Player player) {
        var current = (int) Math.ceil(Float.parseFloat(currentSpace(player).getName()));
        return numberOfSignals(current);
    }

    private int numberOfSignals(int number) {
        // Count signals between nose of engine and number of city delivering to
        return (int) SIGNALS.stream().takeWhile(signal -> signal < number).count();
    }

    Score score(Player player, PlayerState playerState) {
        return new Score(Map.of(ScoreCategory.CITIES, scoreDeliveries(player, playerState),
                ScoreCategory.STATIONS, scoreStations(player)));
    }

    private int scoreStations(Player player) {
        return upgrades.entrySet().stream()
                .filter(entry -> entry.getValue().contains(player))
                .map(Map.Entry::getKey)
                .mapToInt(Station::getPoints)
                .sum();
    }

    private int scoreDeliveries(Player player, PlayerState playerState) {
        int result = 0;

        result -= numberOfDeliveries(player, City.KANSAS_CITY) * (isRailsToTheNorth() ? 8 : 6);

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

        result += numberOfDeliveries(player, City.SAN_FRANCISCO) *
                (isRailsToTheNorth() ? playerState.numberOfBells() * 2 : 9);

        if (hasMadeDelivery(player, City.COLUMBIA) && hasMadeDelivery(player, City.ST_LOUIS)) {
            result -= 5;
        }

        if (hasMadeDelivery(player, City.CHICAGO) && hasMadeDelivery(player, City.DETROIT)) {
            result -= 5;
        }

        if (hasMadeDelivery(player, City.CLEVELAND) && hasMadeDelivery(player, City.PITTSBURGH)) {
            result += 4;
        }

        if (hasMadeDelivery(player, City.PITTSBURGH) && hasMadeDelivery(player, City.NEW_YORK_CITY)) {
            result += 6;
        }

        if (hasMadeDelivery(player, City.NEW_YORK_CITY)) {
            result += 3;
        }

        if (hasMadeDelivery(player, City.GREEN_BAY)) {
            result += 4;
        }

        if (hasMadeDelivery(player, City.TORONTO)) {
            result += 5;
        }

        if (hasMadeDelivery(player, City.MINNEAPOLIS)) {
            result += 10;
        }

        if (hasMadeDelivery(player, City.MONTREAL)) {
            result += 15;
        }

        return result;
    }

    public boolean isRailsToTheNorth() {
        return cityStrip == RTTN_CITY_STRIP;
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

    public List<Player> getUpgradedBy(@NonNull Station station) {
        return upgrades.computeIfAbsent(station, k -> new LinkedList<>());
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

    @ToString(of = "name")
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

            SPACES.put(name, this);
        }

        static Space numbered(String name) {
            return new Space(name, new HashSet<>(3), new HashSet<>(3));
        }

        static Space turnout(String name, Station station) {
            return new Turnout(name, station, new HashSet<>(2), new HashSet<>(2));
        }

        static MediumTown mediumTown(String name, Area area) {
            return new MediumTown(name, area);
        }

        static Space smallTown(String name, Area area) {
            return new SmallTown(name, area);
        }

        static Space smallTown(String name, Area area, BiFunction<Game, Town, ImmediateActions> activate) {
            return new SmallTown(name, area, activate);
        }

        static Space stationTown(String name, Area area, Station station) {
            return new StationTown(name, area, station);
        }

        static BigTown bigTown(String name, Area area, City city) {
            return new BigTown(name, area, city);
        }

        static BigTown bigTown(String name, Area area, City city, BiFunction<Game, Town, ImmediateActions> activate) {
            return new BigTown(name, area, city, activate);
        }

        boolean isAfter(Space space) {
            return previous.stream()
                    .filter(prev -> !(prev instanceof Town))
                    .anyMatch(prev -> prev == space || prev.isAfter(space));
        }

        Space next(Space space) {
            next.add(space);
            space.previous.add(this);
            return space;
        }

        Space turnout(String name, Station station, Space next) {
            next(Space.turnout(name, station)).next(next);
            return next(next);
        }

        Space connect(Space other) {
            other.to(this);
            return to(other);
        }

        Space to(Space other) {
            next.add(other);
            return this;
        }

        private static final class Turnout extends Space {

            @Getter
            private final Station station;

            private Turnout(String name, @NonNull Station station, @NonNull Set<Space> previous, @NonNull Set<Space> next) {
                super(name, next, previous);
                this.station = station;
            }

        }
    }

    ImmediateActions placeBranchlet(Game game, Town town) {
        if (!isRailsToTheNorth()) {
            throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
        }

        if (!isAccessible(START, town, game.getCurrentPlayer())) {
            throw new GWTException(GWTError.TOWN_NOT_ACCESSIBLE);
        }

        var immediateActions = town.placeBranchlet(game);

        var branchlets = this.branchlets.computeIfAbsent(town, k -> new LinkedList<>());
        if (branchlets.contains(game.getCurrentPlayer())) {
            throw new GWTException(GWTError.ALREADY_PLACED_BRANCHLET);
        }

        branchlets.add(game.getCurrentPlayer());

        game.currentPlayerState().rememberLastPlacedBranchlet(town);

        return immediateActions;
    }

    private boolean isAccessible(Space from, Town to, Player player) {
        if (from == to) {
            return true;
        }
        return accessibleTowns(from, player, new HashSet<>(Set.of(from)), "").anyMatch(accessible -> accessible == to);
    }

    public static abstract class Town extends Space {

        @Getter
        private final Area area;
        private final BiFunction<Game, Town, ImmediateActions> activate;

        Town(String name, Area area, BiFunction<Game, Town, ImmediateActions> activate) {
            super(name, Collections.emptySet(), Collections.emptySet());
            this.area = area;
            this.activate = activate;

            TOWNS.put(name, this);
        }

        public ImmediateActions placeBranchlet(Game game) {
            return activate.apply(game, this);
        }
    }

    private static class SmallTown extends Town {

        SmallTown(String name, Area area) {
            this(name, area, (game, town) -> ImmediateActions.none());
        }

        SmallTown(String name, Area area, BiFunction<Game, Town, ImmediateActions> activate) {
            super(name, area, activate);
        }
    }

    private static class MediumTown extends Town {

        MediumTown(String name, Area area) {
            super(name, area, (game, town) -> game.getRailroadTrack().getMediumTownTile(town)
                    .map(mediumTownTile -> mediumTownTile.activate(game))
                    .orElse(ImmediateActions.none()));
            MEDIUM_TOWNS.put(name, this);
        }

    }

    private static class BigTown extends Town {

        @Getter
        private final City city;

        BigTown(String name, Area area, City city) {
            this(name, area, city, (game, town) -> ImmediateActions.none());
        }

        BigTown(String name, Area area, City city, BiFunction<Game, Town, ImmediateActions> activate) {
            super(name, area, activate);
            this.city = city;

            BIG_TOWNS.put(city, this);
        }
    }

    private static class StationTown extends Town {

        @Getter
        private final Station station;

        StationTown(String name, Area area, Station station) {
            super(name, area, (game, town) -> {
                if (!game.getRailroadTrack().hasUpgraded(station, game.getCurrentPlayer())) {
                    return ImmediateActions.of(PossibleAction.optional(Action.UpgradeStationTown.class));
                }
                return ImmediateActions.none();
            });
            this.station = station;
        }

    }

    private static ImmediateActions gainExchangeToken(Game game, Town town) {
        return ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class));
    }

    private static ImmediateActions firstPlayerGainsExchangeToken(Game game, Town town) {
        return game.getRailroadTrack().getBranchlets(town).isEmpty()
                ? ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class))
                : ImmediateActions.none();
    }

    private static ImmediateActions firstPlayerTakeObjectiveCard(Game game, Town town) {
        return game.getRailroadTrack().getBranchlets(town).isEmpty()
                ? ImmediateActions.of(PossibleAction.optional(Action.TakeObjectiveCard.class))
                : ImmediateActions.none();
    }

    private static ImmediateActions firstPlayerGains2Dollars(Game game, Town town) {
        return game.getRailroadTrack().getBranchlets(town).isEmpty()
                ? ImmediateActions.of(PossibleAction.optional(Action.Gain2Dollars.class))
                : ImmediateActions.none();
    }

    private static ImmediateActions firstPlayerGains1Certificate(Game game, Town town) {
        return game.getRailroadTrack().getBranchlets(town).isEmpty()
                ? ImmediateActions.of(PossibleAction.optional(Action.Gain1Certificate.class))
                : ImmediateActions.none();
    }

    private static ImmediateActions pay1Or2Or3Dollars(Game game, Town town) {
        int amount;
        switch (game.getRailroadTrack().getBranchlets(town).size()) {
            case 0:
                amount = 1;
                break;
            case 3:
                amount = 3;
                break;
            default:
                amount = 2;
                break;
        }

        game.currentPlayerState().payDollars(amount);

        return ImmediateActions.none();
    }

    private static ImmediateActions pay1Or3Or4Dollars(Game game, Town town) {
        int amount;
        switch (game.getRailroadTrack().getBranchlets(town).size()) {
            case 0:
                amount = 1;
                break;
            case 3:
                amount = 4;
                break;
            default:
                amount = 3;
                break;
        }

        game.currentPlayerState().payDollars(amount);

        return ImmediateActions.none();
    }

    public List<Player> getBranchlets(Town town) {
        return Collections.unmodifiableList(branchlets.getOrDefault(town, Collections.emptyList()));
    }

    private enum Area {
        GREEN,
        BLUE,
        PURPLE,
        TEAL,
        RED,
        VIOLET
    }

}

