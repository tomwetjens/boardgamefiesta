package com.wetjens.gwt;

import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Trail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private final Location.Start start;

    private final List<Location.TeepeeLocation> teepeeLocations;

    @Getter
    private final Location.KansasCity kansasCity;

    private final Map<Player, Location> playerLocations = new HashMap<>();

    public Trail(@NonNull Collection<Player> players, boolean beginner, @NonNull Random random) {
        var neutralBuildings = new LinkedList<>(createNeutralBuildingSet());
        if (!beginner) {
            Collections.shuffle(neutralBuildings, random);
        }

        kansasCity = new Location.KansasCity();

        Location.BuildingLocation g = new Location.BuildingLocation("G", false,
                new Location.BuildingLocation("G-1", false, kansasCity),
                new Location.BuildingLocation("G-2", false, kansasCity));
        g.placeBuilding(neutralBuildings.poll());

        Location.BuildingLocation f = new Location.BuildingLocation("F", false,
                new Location.BuildingLocation("F-1", false, g),
                new Location.BuildingLocation("F-2", true, g));
        f.placeBuilding(neutralBuildings.poll());

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
        e.placeBuilding(neutralBuildings.poll());

        Location.BuildingLocation d = new Location.BuildingLocation("D", false, e);
        d.placeBuilding(neutralBuildings.poll());

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
        c.placeBuilding(neutralBuildings.poll());

        Location.HazardLocation droughtSection = new Location.HazardLocation(HazardType.DROUGHT, 1,
                new Location.HazardLocation(HazardType.DROUGHT, 2,
                        new Location.HazardLocation(HazardType.DROUGHT, 3,
                                new Location.HazardLocation(HazardType.DROUGHT, 4,
                                        new Location.BuildingLocation(HazardType.DROUGHT + "-RISK-1", Action.Discard1CattleCardToGain1Certificate.class, false, c)))));

        Location.BuildingLocation b = new Location.BuildingLocation("B", false, droughtSection,
                new Location.BuildingLocation("B-1", true,
                        new Location.BuildingLocation("B-2", false,
                                new Location.BuildingLocation("B-3", false, c))));
        b.placeBuilding(neutralBuildings.poll());

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
        a.placeBuilding(neutralBuildings.poll());

        start = new Location.Start(a);
    }

    private static List<NeutralBuilding> createNeutralBuildingSet() {
        return Arrays.asList(
                new NeutralBuilding.G(),
                new NeutralBuilding.F(),
                new NeutralBuilding.E(),
                new NeutralBuilding.D(),
                new NeutralBuilding.C(),
                new NeutralBuilding.B(),
                new NeutralBuilding.A()
        );
    }

    public Set<Location> getLocations() {
        return getLocations(start).collect(Collectors.toSet());
    }

    private Stream<Location> getLocations(Location from) {
        return Stream.concat(Stream.of(from), from.getNext().stream().flatMap(this::getLocations));
    }

    public List<Location.HazardLocation> getHazardLocations(HazardType hazardType) {
        return getLocations().stream()
                .filter(location -> location instanceof Location.HazardLocation)
                .map(location -> (Location.HazardLocation) location)
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

    public void removeHazard(Hazard hazard) {
        Location.HazardLocation hazardLocation = getHazardLocations(hazard.getType()).stream()
                .filter(location -> hazard == location.getHazard().orElse(null))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.HAZARD_NOT_ON_TRAIL, hazard.getType(), hazard.getHand(), hazard.getPoints()));

        hazardLocation.removeHazard();
    }

    public Location getLocation(String name) {
        return getLocations().stream()
                .filter(location -> location.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_SUCH_LOCATION, name));
    }

    public Location.TeepeeLocation getTeepeeLocation(int reward) {
        return teepeeLocations.stream()
                .filter(teepeeLocation -> teepeeLocation.getReward() == reward).findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_SUCH_LOCATION, reward));
    }

    Set<PossibleMove> possibleMoves(Location from, Location to, Player player, int balance, int stepLimit, int playerCount) {
        if (from.isEmpty()) {
            throw new GWTException(GWTError.LOCATION_EMPTY, from.getName());
        }

        if (to.isEmpty()) {
            throw new GWTException(GWTError.LOCATION_EMPTY, to.getName());
        }

        return reachableLocations(from, to, stepLimit)
                .filter(steps -> steps.get(steps.size() - 1) == to)
                .map(steps -> new PossibleMove(from, to, steps, player, balance, playerCount))
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
        return new Score(Map.of(Score.Category.BUILDINGS, getBuildings(player).stream()
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
