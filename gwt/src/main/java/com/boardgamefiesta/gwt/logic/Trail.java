package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.Getter;
import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Trail {

    @Getter
    private final Location.Start start;

    private final Map<HazardType, List<Location.HazardLocation>> hazardLocations;
    private final Map<String, Location.TeepeeLocation> teepeeLocations;
    private final Map<String, Location.BuildingLocation> buildingLocations;
    private final List<Location.BuildingLocation> neutralBuildingLocations;

    @Getter
    private final Location.KansasCity kansasCity;

    private final Map<Player, Location> playerLocations = new HashMap<>();

    Trail(@NonNull GWT.Edition edition) {
        kansasCity = new Location.KansasCity();

        var g1 = new Location.BuildingLocation("G-1", false, kansasCity);
        var g2 = new Location.BuildingLocation("G-2", false, kansasCity);
        var g = new Location.BuildingLocation("G", false, g1, g2);

        var f1 = new Location.BuildingLocation("F-1", false, g);
        var f2 = new Location.BuildingLocation("F-2", true, g);
        var f = new Location.BuildingLocation("F", false, f1, f2);

        var rockfallRisk2 = new Location.BuildingLocation(HazardType.ROCKFALL + "-RISK-2", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, true, f);
        var rockfallRisk1 = new Location.BuildingLocation(HazardType.ROCKFALL + "-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, false, rockfallRisk2);
        var rockfall4 = new Location.HazardLocation(HazardType.ROCKFALL, 4, rockfallRisk1);
        var rockfall3 = new Location.HazardLocation(HazardType.ROCKFALL, 3, rockfall4);
        var rockfall2 = new Location.HazardLocation(HazardType.ROCKFALL, 2, rockfall3);
        var rockfall1 = new Location.HazardLocation(HazardType.ROCKFALL, 1, rockfall2);

        var e2 = new Location.BuildingLocation("E-2", true, f);
        var e1 = new Location.BuildingLocation("E-1", true, e2);
        var e = new Location.BuildingLocation("E", false, e1, rockfall1);

        var indianTradeRisk2 = new Location.BuildingLocation("INDIAN-TRADE-RISK-2", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, false, e);
        var indianTradeRisk1 = new Location.BuildingLocation("INDIAN-TRADE-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, false, indianTradeRisk2);
        var teepeeLocation10 = new Location.TeepeeLocation("TEEPEE-10", 10, indianTradeRisk1);
        var teepeeLocation8 = new Location.TeepeeLocation("TEEPEE-8", 8, teepeeLocation10);
        var teepeeLocation6 = new Location.TeepeeLocation("TEEPEE-6", 6, teepeeLocation8);
        var teepeeLocation4 = new Location.TeepeeLocation("TEEPEE-4", edition == GWT.Edition.FIRST ? 4 : 5, teepeeLocation6);
        var teepeeLocation2 = new Location.TeepeeLocation("TEEPEE-2", edition == GWT.Edition.FIRST ? 2 : 4, teepeeLocation4);
        var teepeeLocation1 = new Location.TeepeeLocation("TEEPEE-1", edition == GWT.Edition.FIRST ? 1 : 3, teepeeLocation2);

        var teepeeMin3 = new Location.TeepeeLocation("TEEPEE--3", edition == GWT.Edition.FIRST ? -3 : 2);
        var teepeeMin2 = new Location.TeepeeLocation("TEEPEE--2", edition == GWT.Edition.FIRST ? -2 : 1);
        var teepeeMin1 = new Location.TeepeeLocation("TEEPEE--1", edition == GWT.Edition.FIRST ? -1 : 0);
        teepeeLocations = Stream.of(
                teepeeMin3,
                teepeeMin2,
                teepeeMin1,
                teepeeLocation1,
                teepeeLocation2,
                teepeeLocation4,
                teepeeLocation6,
                teepeeLocation8,
                teepeeLocation10
        ).collect(Collectors.toMap(Location.TeepeeLocation::getName, Function.identity()));

        var c12 = new Location.BuildingLocation("C-1-2", true, e);
        var c11 = new Location.BuildingLocation("C-1-1", true, c12);

        Location.BuildingLocation d;
        Location.BuildingLocation d1 = null;
        Location.BuildingLocation c;
        Location.BuildingLocation c2 = null;
        if (edition == GWT.Edition.FIRST) {
            d = new Location.BuildingLocation("D", false, e);
            c2 = new Location.BuildingLocation("C-2", false, d, teepeeLocation1);
            c = new Location.BuildingLocation("C", false, c11, c2);
        } else {
            d1 = new Location.BuildingLocation("D-1", false, e);
            d = new Location.BuildingLocation("D", false, d1, teepeeLocation1);
            c = new Location.BuildingLocation("C", false, c11, d);
        }

        var droughtRisk1 = new Location.BuildingLocation(HazardType.DROUGHT + "-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, false, c);
        var drought4 = new Location.HazardLocation(HazardType.DROUGHT, 4, droughtRisk1);
        var drought3 = new Location.HazardLocation(HazardType.DROUGHT, 3, drought4);
        var drought2 = new Location.HazardLocation(HazardType.DROUGHT, 2, drought3);
        var drought1 = new Location.HazardLocation(HazardType.DROUGHT, 1, drought2);

        var b3 = new Location.BuildingLocation("B-3", false, c);
        var b2 = new Location.BuildingLocation("B-2", false, b3);
        var b1 = new Location.BuildingLocation("B-1", true, b2);
        var b = new Location.BuildingLocation("B", false, drought1, b1);

        var floodRisk2 = new Location.BuildingLocation(HazardType.FLOOD + "-RISK-2", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, true, b);
        var floodRisk1 = new Location.BuildingLocation(HazardType.FLOOD + "-RISK-1",
                edition == GWT.Edition.FIRST ? Action.Discard1JerseyToGain1CertificateAnd2Dollars.class
                        : Action.Discard1CattleCardToGain1Certificate.class, false, floodRisk2);
        var flood4 = new Location.HazardLocation(HazardType.FLOOD, 4, floodRisk1);
        var flood3 = new Location.HazardLocation(HazardType.FLOOD, 3, flood4);
        var flood2 = new Location.HazardLocation(HazardType.FLOOD, 2, flood3);
        var flood1 = new Location.HazardLocation(HazardType.FLOOD, 1, flood2);

        var a3 = new Location.BuildingLocation("A-3", false, b);
        var a2 = new Location.BuildingLocation("A-2", false, a3);
        var a1 = new Location.BuildingLocation("A-1", false, a2);
        var a = new Location.BuildingLocation("A", false, a1, flood1);

        neutralBuildingLocations = List.of(a, b, c, d, e, f, g);
        buildingLocations = Stream.of(a, a1, a2, a3, floodRisk1, floodRisk2, b, b1, b2, b3, droughtRisk1, c, c11, c12, c2, d, d1, e, e1, e2, indianTradeRisk1, indianTradeRisk2, rockfallRisk1, rockfallRisk2, f, f1, f2, g, g1, g2)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Location::getName, Function.identity()));
        hazardLocations = Stream.of(flood1, flood2, flood3, flood4, drought1, drought2, drought3, drought4, rockfall1, rockfall2, rockfall3, rockfall4)
                .collect(Collectors.groupingBy(Location.HazardLocation::getType));

        start = new Location.Start(a);
    }

    public Trail(GWT.Edition edition, boolean beginner, @NonNull Random random) {
        this(edition);

        var neutralBuildings = new LinkedList<>(createNeutralBuildingSet());
        if (!beginner) {
            Collections.shuffle(neutralBuildings, random);
        }

        neutralBuildingLocations.forEach(buildingLocation ->
                buildingLocation.placeBuilding(neutralBuildings.poll()));
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var locations = factory.createObjectBuilder();

        buildingLocations.values().stream()
                .filter(buildingLocation -> buildingLocation.getBuilding().isPresent())
                .forEach(buildingLocation -> locations.add(buildingLocation.getName(), factory.createObjectBuilder()
                        .add("building", buildingLocation.getBuilding().map(b -> b.serialize(factory)).orElse(null))));

        teepeeLocations.values().stream()
                .filter(teepeeLocation -> teepeeLocation.getTeepee().isPresent())
                .forEach(teepeeLocation -> locations.add(teepeeLocation.getName(), factory.createObjectBuilder()
                        .add("teepee", teepeeLocation.getTeepee().map(Teepee::name).orElse(null))));

        hazardLocations.values().stream()
                .flatMap(List::stream)
                .filter(hazardLocation -> hazardLocation.getHazard().isPresent())
                .forEach(hazardLocation -> locations.add(hazardLocation.getName(), factory.createObjectBuilder()
                        .add("hazard", hazardLocation.getHazard().map(h -> h.serialize(factory)).orElse(null))));

        return factory.createObjectBuilder()
                .add("playerLocations", JsonSerializer.forFactory(factory).fromStringMap(playerLocations, Player::getName, Location::getName))
                .add("locations", locations)
                .build();
    }

    static Trail deserialize(GWT.Edition edition, Map<String, Player> playerMap, JsonObject jsonObject) {
        var trail = new Trail(edition);

        jsonObject.getJsonObject("playerLocations").forEach((key, value) ->
                trail.playerLocations.put(playerMap.get(key), trail.getLocation(((JsonString) value).getString())));

        var locations = jsonObject.getJsonObject("locations");

        trail.buildingLocations.values().forEach(buildingLocation -> {
            var location = locations.get(buildingLocation.getName());
            if (location != null && location != JsonValue.NULL) {
                var building = location.asJsonObject().get("building");
                if (building != null && building != JsonValue.NULL) {
                    buildingLocation.placeBuilding(Building.deserialize(edition, playerMap, building.asJsonObject()));
                }
            }
        });

        trail.teepeeLocations.values().forEach(teepeeLocation -> {
            var location = locations.get(teepeeLocation.getName());
            if (location != null && location != JsonValue.NULL) {
                var teepee = location.asJsonObject().getString("teepee");
                if (teepee != null) {
                    teepeeLocation.placeTeepee(Teepee.valueOf(teepee));
                }
            }
        });

        trail.hazardLocations.values().stream()
                .flatMap(List::stream)
                .forEach(hazardLocation -> {
                    var location = locations.get(hazardLocation.getName());
                    if (location != null && location != JsonValue.NULL) {
                        var hazard = location.asJsonObject().get("hazard");
                        if (hazard != null && hazard != JsonValue.NULL) {
                            hazardLocation.placeHazard(Hazard.deserialize(hazard.asJsonObject()));
                        }
                    }
                });

        return trail;
    }

    private static List<NeutralBuilding> createNeutralBuildingSet() {
        return Arrays.asList(
                new NeutralBuilding.A(),
                new NeutralBuilding.B(),
                new NeutralBuilding.C(),
                new NeutralBuilding.D(),
                new NeutralBuilding.E(),
                new NeutralBuilding.F(),
                new NeutralBuilding.G()
        );
    }

    public Set<Location> getLocations() {
        return getLocations(start).collect(Collectors.toSet());
    }

    private Stream<Location> getLocations(Location from) {
        return Stream.concat(Stream.of(from), from.getNext().stream().flatMap(this::getLocations));
    }

    public List<Location.HazardLocation> getHazardLocations(HazardType hazardType) {
        return Collections.unmodifiableList(hazardLocations.get(hazardType));
    }

    public Stream<Location.HazardLocation> getHazardLocations() {
        return hazardLocations.values().stream().flatMap(List::stream);
    }

    public Collection<Location.TeepeeLocation> getTeepeeLocations() {
        return Collections.unmodifiableCollection(teepeeLocations.values());
    }

    public Collection<Location.BuildingLocation> getBuildingLocations() {
        return Collections.unmodifiableCollection(buildingLocations.values());
    }

    public Optional<Location> getCurrentLocation(Player player) {
        return Optional.ofNullable(playerLocations.get(player));
    }

    void movePlayer(Player player, Location to) {
        playerLocations.put(player, to);
    }

    public Location getLocation(String name) {
        return getLocations().stream()
                .filter(location -> location.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_SUCH_LOCATION));
    }

    public Optional<Location.BuildingLocation> getBuildingLocation(String name) {
        return Optional.ofNullable(buildingLocations.get(name));
    }

    public Location.TeepeeLocation getTeepeeLocation(int reward) {
        return teepeeLocations.values().stream()
                .filter(teepeeLocation -> teepeeLocation.getReward() == reward)
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_SUCH_LOCATION));
    }

    public Location.TeepeeLocation getTeepeeLocation(String name) {
        return Optional.ofNullable(teepeeLocations.get(name))
                .orElseThrow(() -> new GWTException(GWTError.NO_SUCH_LOCATION));
    }

    Set<PossibleMove> possibleMoves(Player player, int balance, int stepLimit, int playerCount) {
        if (stepLimit <= 0) {
            return Collections.emptySet();
        }

        return getCurrentLocation(player)
                .map(from -> reachableLocations(from, stepLimit)
                        .map(steps -> PossibleMove.fromTo(from, steps, player, balance, playerCount)))
                .orElseGet(() -> getLocations().stream()
                        .filter(location -> location != start)
                        .filter(location -> !location.isEmpty())
                        .filter(location -> location instanceof Location.BuildingLocation)
                        .map(PossibleMove::firstMove))
                .collect(Collectors.toSet());
    }

    private static Stream<List<Location>> reachableLocations(Location from, int stepLimit) {
        if (stepLimit <= 0) {
            return Stream.empty();
        }

        return from.getNext().stream()
                .flatMap(next -> {
                    if (next.isEmpty()) {
                        // Empty, so does not count towards step limit
                        return reachableLocations(next, stepLimit);
                    } else {
                        // Not empty, so counts as a step towards the step limit
                        return Stream.concat(Stream.of(List.of(next)), reachableLocations(next, stepLimit - 1)
                                .map(steps -> Stream.concat(Stream.of(next), steps.stream()).collect(Collectors.toList())));
                    }
                });
    }

    Set<PossibleMove> possibleMoves(Location from, Location to, Player player, int balance, int stepLimit, int playerCount) {
        if (to.isEmpty()) {
            throw new GWTException(GWTError.LOCATION_EMPTY);
        }

        return reachableLocations(from, to, stepLimit)
                .filter(steps -> steps.get(steps.size() - 1) == to)
                .map(steps -> PossibleMove.fromTo(from, steps, player, balance, playerCount))
                .collect(Collectors.toSet());
    }

    private Stream<List<Location>> reachableLocations(Location from, Location to, int stepLimit) {
        if (from == to || stepLimit == 0) {
            return Stream.empty();
        }

        return from.getNext().stream()
                .flatMap(next -> Stream.concat(
                        Stream.of(Collections.singletonList(next)),
                        next.isEmpty()
                                ? reachableLocations(next, to, stepLimit)
                                : reachableLocations(next, to, stepLimit - 1)
                                .map(nextSteps -> Stream.concat(Stream.of(next), nextSteps.stream()).collect(Collectors.toList()))));
    }

    public int buildingsInWoods(Player player) {
        return (int) getBuildingLocations().stream()
                .filter(Location.BuildingLocation::isInWoods)
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .filter(playerBuilding -> playerBuilding.getPlayer() == player)
                .count();
    }

    public Set<Location> getAdjacentLocations(Location location) {
        return Stream.concat(location.getNext().stream(), getLocations().stream()
                .filter(l -> l.getNext().contains(location))).collect(Collectors.toSet());
    }

    int numberOfBuildings(Player player) {
        return getBuildings(player).size();
    }

    Set<PlayerBuilding> getBuildings(Player player) {
        return getBuildingLocations().stream()
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .filter(playerBuilding -> playerBuilding.getPlayer() == player)
                .collect(Collectors.toSet());
    }

    Score score(Player player) {
        return new Score(Map.of(ScoreCategory.BUILDINGS, getBuildings(player).stream()
                .mapToInt(PlayerBuilding::getPoints)
                .sum()));
    }

    public Map<Player, Location> getPlayerLocations() {
        return Collections.unmodifiableMap(playerLocations);
    }

    void placeHazard(Hazard hazard) {
        getHazardLocations(hazard.getType()).stream()
                .filter(Location.HazardLocation::isEmpty)
                .min(Comparator.comparingInt(Location.HazardLocation::getNumber))
                .ifPresent(hazardLocation -> hazardLocation.placeHazard(hazard));
    }

    void placeTeepee(Teepee teepee) {
        getTeepeeLocations().stream()
                .filter(Location.TeepeeLocation::isEmpty)
                .min(Comparator.comparingInt(Location.TeepeeLocation::getReward))
                .ifPresent(teepeeLocation -> teepeeLocation.placeTeepee(teepee));
    }

    void moveToStart(Player player) {
        playerLocations.put(player, start);
    }

    public boolean atKansasCity(Player player) {
        return playerLocations.get(player) == kansasCity;
    }

}
