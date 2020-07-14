package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.Score;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.Getter;
import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Trail {

    @Getter
    private final Location.Start start;

    private final List<Location.TeepeeLocation> teepeeLocations;

    private final List<Location.BuildingLocation> neutralBuildingLocations;

    @Getter
    private final Location.KansasCity kansasCity;

    private final Map<Player, Location> playerLocations = new HashMap<>();

    private Trail() {
        kansasCity = new Location.KansasCity();

        Location.BuildingLocation g = new Location.BuildingLocation("G", false,
                new Location.BuildingLocation("G-1", false, kansasCity),
                new Location.BuildingLocation("G-2", false, kansasCity));

        Location.BuildingLocation f = new Location.BuildingLocation("F", false,
                new Location.BuildingLocation("F-1", false, g),
                new Location.BuildingLocation("F-2", true, g));

        Location.HazardLocation rockfallSection = new Location.HazardLocation(HazardType.ROCKFALL, 1,
                new Location.HazardLocation(HazardType.ROCKFALL, 2,
                        new Location.HazardLocation(HazardType.ROCKFALL, 3,
                                new Location.HazardLocation(HazardType.ROCKFALL, 4,
                                        new Location.BuildingLocation(HazardType.ROCKFALL + "-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, true,
                                                new Location.BuildingLocation(HazardType.ROCKFALL + "-RISK-2", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, true, f))))));

        Location.BuildingLocation e = new Location.BuildingLocation("E", false,
                new Location.BuildingLocation("E-1", true,
                        new Location.BuildingLocation("E-2", true, f)),
                rockfallSection);

        Location.BuildingLocation d = new Location.BuildingLocation("D", false, e);

        Location.TeepeeLocation teepeeLocation10 = new Location.TeepeeLocation(10,
                new Location.BuildingLocation("INDIAN-TRADE-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, false,
                        new Location.BuildingLocation("INDIAN-TRADE-RISK-2", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, false, e)));
        Location.TeepeeLocation teepeeLocation8 = new Location.TeepeeLocation(8, teepeeLocation10);
        Location.TeepeeLocation teepeeLocation6 = new Location.TeepeeLocation(6, teepeeLocation8);
        Location.TeepeeLocation teepeeLocation4 = new Location.TeepeeLocation(4, teepeeLocation6);
        Location.TeepeeLocation teepeeLocation2 = new Location.TeepeeLocation(2, teepeeLocation4);
        Location.TeepeeLocation teepeeLocation1 = new Location.TeepeeLocation(1, teepeeLocation2);

        teepeeLocations = Arrays.asList(
                new Location.TeepeeLocation(-3),
                new Location.TeepeeLocation(-2),
                new Location.TeepeeLocation(-1),
                teepeeLocation1,
                teepeeLocation2,
                teepeeLocation4,
                teepeeLocation6,
                teepeeLocation8,
                teepeeLocation10
        );

        Location.BuildingLocation crossRoadsIndianTrade = new Location.BuildingLocation("C-2", false, d, teepeeLocation1);

        Location.BuildingLocation c = new Location.BuildingLocation("C", false,
                new Location.BuildingLocation("C-1-1", true,
                        new Location.BuildingLocation("C-1-2", true, e)),
                crossRoadsIndianTrade);

        Location.HazardLocation droughtSection = new Location.HazardLocation(HazardType.DROUGHT, 1,
                new Location.HazardLocation(HazardType.DROUGHT, 2,
                        new Location.HazardLocation(HazardType.DROUGHT, 3,
                                new Location.HazardLocation(HazardType.DROUGHT, 4,
                                        new Location.BuildingLocation(HazardType.DROUGHT + "-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, false, c)))));

        Location.BuildingLocation b = new Location.BuildingLocation("B", false, droughtSection,
                new Location.BuildingLocation("B-1", true,
                        new Location.BuildingLocation("B-2", false,
                                new Location.BuildingLocation("B-3", false, c))));

        Location.HazardLocation floodSection = new Location.HazardLocation(HazardType.FLOOD, 1,
                new Location.HazardLocation(HazardType.FLOOD, 2,
                        new Location.HazardLocation(HazardType.FLOOD, 3,
                                new Location.HazardLocation(HazardType.FLOOD, 4,
                                        new Location.BuildingLocation(HazardType.FLOOD + "-RISK-1", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, false,
                                                new Location.BuildingLocation(HazardType.FLOOD + "-RISK-2", Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, true, b))))));
        Location.BuildingLocation a = new Location.BuildingLocation("A", false,
                new Location.BuildingLocation("A-1", false,
                        new Location.BuildingLocation("A-2", false,
                                new Location.BuildingLocation("A-3", false, b))),
                floodSection);

        neutralBuildingLocations = List.of(a, b, c, d, e, f, g);

        start = new Location.Start(a);
    }

    public Trail(boolean beginner, @NonNull Random random) {
        this();

        var neutralBuildings = new LinkedList<>(createNeutralBuildingSet());
        if (!beginner) {
            Collections.shuffle(neutralBuildings, random);
        }

        neutralBuildingLocations.forEach(buildingLocation ->
                buildingLocation.placeBuilding(neutralBuildings.poll()));
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var locations = factory.createObjectBuilder();

        getBuildingLocations().stream()
                .filter(buildingLocation -> buildingLocation.getBuilding().isPresent())
                .forEach(buildingLocation -> locations.add(buildingLocation.getName(), factory.createObjectBuilder()
                        .add("building", buildingLocation.getBuilding().map(b -> b.serialize(factory)).orElse(null))));

        getTeepeeLocations().stream()
                .filter(teepeeLocation -> teepeeLocation.getTeepee().isPresent())
                .forEach(teepeeLocation -> locations.add(teepeeLocation.getName(), factory.createObjectBuilder()
                        .add("teepee", teepeeLocation.getTeepee().map(Teepee::name).orElse(null))));

        getHazardLocations()
                .filter(hazardLocation -> hazardLocation.getHazard().isPresent())
                .forEach(hazardLocation -> locations.add(hazardLocation.getName(), factory.createObjectBuilder()
                        .add("hazard", hazardLocation.getHazard().map(h -> h.serialize(factory)).orElse(null))));

        return factory.createObjectBuilder()
                .add("playerLocations", JsonSerializer.forFactory(factory).fromStringMap(playerLocations, Player::getName, Location::getName))
                .add("locations", locations)
                .build();
    }

    static Trail deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        var trail = new Trail();

        jsonObject.getJsonObject("playerLocations").forEach((key, value) ->
                trail.playerLocations.put(playerMap.get(key), trail.getLocation(((JsonString) value).getString())));

        var locations = jsonObject.getJsonObject("locations");

        trail.getBuildingLocations().forEach(buildingLocation -> {
            var location = locations.getJsonObject(buildingLocation.getName());
            if (location != null) {
                var building = location.getJsonObject("building");
                if (building != null) {
                    buildingLocation.placeBuilding(Building.deserialize(playerMap, building));
                }
            }
        });

        trail.getTeepeeLocations().forEach(teepeeLocation -> {
            var location = locations.getJsonObject(teepeeLocation.getName());
            if (location != null) {
                var teepee = location.getString("teepee");
                if (teepee != null) {
                    teepeeLocation.placeTeepee(Teepee.valueOf(teepee));
                }
            }
        });

        trail.getHazardLocations().forEach(hazardLocation -> {
            var location = locations.getJsonObject(hazardLocation.getName());
            if (location != null) {
                var hazard = location.getJsonObject("hazard");
                if (hazard != null) {
                    hazardLocation.placeHazard(Hazard.deserialize(hazard));
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

    private Stream<Location.HazardLocation> getHazardLocations() {
        return getLocations().stream()
                .filter(location -> location instanceof Location.HazardLocation)
                .map(location -> (Location.HazardLocation) location);
    }

    public List<Location.HazardLocation> getHazardLocations(HazardType hazardType) {
        return getHazardLocations()
                .filter(hazardLocation -> hazardLocation.getType() == hazardType)
                .collect(Collectors.toList());
    }

    public List<Location.TeepeeLocation> getTeepeeLocations() {
        return Collections.unmodifiableList(teepeeLocations);
    }

    public Set<Location.BuildingLocation> getBuildingLocations() {
        return getLocations().stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .collect(Collectors.toSet());
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

    public Location.TeepeeLocation getTeepeeLocation(int reward) {
        return teepeeLocations.stream()
                .filter(teepeeLocation -> teepeeLocation.getReward() == reward).findAny()
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
                        .map(PossibleMove::firstMove))
                .collect(Collectors.toSet());
    }

    private Stream<List<Location>> reachableLocations(Location from, int stepLimit) {
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
        return new Score(Map.of(ScoreCategory.BUILDINGS.name(), getBuildings(player).stream()
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
}
