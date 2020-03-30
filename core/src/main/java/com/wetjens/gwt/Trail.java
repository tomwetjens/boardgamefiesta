package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;

public class Trail {

    @Getter
    private final Location.Start start;

    private final List<Location.TeepeeLocation> teepeeLocations;

    @Getter
    private final Location.KansasCity kansasCity;

    private final EnumMap<Player, Location> playerLocations = new EnumMap<>(Player.class);

    public Trail() {
        // TODO Randomize building placement if not beginner
        Queue<Building> buildingsToPlace = new LinkedList<>(Arrays.asList(
                new NeutralBuilding.G(),
                new NeutralBuilding.F(),
                new NeutralBuilding.E(),
                new NeutralBuilding.D(),
                new NeutralBuilding.C(),
                new NeutralBuilding.B(),
                new NeutralBuilding.A()
        ));

        kansasCity = new Location.KansasCity();

        Location.BuildingLocation g = new Location.BuildingLocation("G",
                new Location.BuildingLocation("G-1", kansasCity),
                new Location.BuildingLocation("G-2", kansasCity));
        g.placeBuilding(buildingsToPlace.poll());

        Location.BuildingLocation f = new Location.BuildingLocation("F",
                new Location.BuildingLocation("F-1", g),
                new Location.BuildingLocation("F-2", g));
        f.placeBuilding(buildingsToPlace.poll());

        Location.HazardLocation rockfallSection = new Location.HazardLocation(HazardType.ROCKFALL, 1,
                new Location.HazardLocation(HazardType.ROCKFALL, 2,
                        new Location.HazardLocation(HazardType.ROCKFALL, 3,
                                new Location.HazardLocation(HazardType.ROCKFALL, 4,
                                        new Location.BuildingLocation(HazardType.ROCKFALL + "-RISK-1",
                                                new Location.BuildingLocation(HazardType.ROCKFALL + "-RISK-2", f))))));

        Location.BuildingLocation e = new Location.BuildingLocation("E",
                new Location.BuildingLocation("E-1",
                        new Location.BuildingLocation("E-2", f)),
                rockfallSection);
        e.placeBuilding(buildingsToPlace.poll());

        Location.BuildingLocation d = new Location.BuildingLocation("D", e);
        d.placeBuilding(buildingsToPlace.poll());

        Location.TeepeeLocation teepeeLocation10 = new Location.TeepeeLocation(10,
                new Location.BuildingLocation("INDIAN-TRADE-RISK-1",
                        new Location.BuildingLocation("INDIAN-TRADE-RISK-2", e)));
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

        Location.BuildingLocation crossRoadsIndianTrade = new Location.BuildingLocation("C-2", d, teepeeLocation1);

        Location.BuildingLocation c = new Location.BuildingLocation("C",
                new Location.BuildingLocation("C-1-1",
                        new Location.BuildingLocation("C-1-2", e)),
                crossRoadsIndianTrade);
        c.placeBuilding(buildingsToPlace.poll());

        Location.HazardLocation droughtSection = new Location.HazardLocation(HazardType.DROUGHT, 1,
                new Location.HazardLocation(HazardType.DROUGHT, 2,
                        new Location.HazardLocation(HazardType.DROUGHT, 3,
                                new Location.HazardLocation(HazardType.DROUGHT, 4,
                                        new Location.BuildingLocation(HazardType.DROUGHT + "-RISK-1", c)))));  // TODO Add risk action

        Location.BuildingLocation b = new Location.BuildingLocation("B",
                droughtSection,
                new Location.BuildingLocation("B-1",
                        new Location.BuildingLocation("B-2",
                                new Location.BuildingLocation("B-3", c))));
        b.placeBuilding(buildingsToPlace.poll());

        Location.HazardLocation floodSection = new Location.HazardLocation(HazardType.FLOOD, 1,
                new Location.HazardLocation(HazardType.FLOOD, 2,
                        new Location.HazardLocation(HazardType.FLOOD, 3,
                                new Location.HazardLocation(HazardType.FLOOD, 4,
                                        new Location.BuildingLocation(HazardType.FLOOD + "-RISK-1", // TODO Add risk action
                                                new Location.BuildingLocation(HazardType.FLOOD + "-RISK-2", b))))));  // TODO Add risk action
        Location.BuildingLocation a = new Location.BuildingLocation("A",
                new Location.BuildingLocation("A-1",
                        new Location.BuildingLocation("A-2",
                                new Location.BuildingLocation("A-3", b))),
                floodSection);
        a.placeBuilding(buildingsToPlace.poll());

        start = new Location.Start(a);
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

    public Location.BuildingLocation getBuildingLocation(Class<? extends Building> clazz) {
        return getBuildingLocations()
                .stream()
                .filter(buildingLocation -> buildingLocation.getBuilding()
                        .map(building -> clazz.equals(building.getClass()))
                        .orElse(false))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Building not on trail: " + clazz.getSimpleName()));
    }

    public Set<Location.BuildingLocation> getBuildingLocations() {
        return getLocations().stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .collect(Collectors.toSet());
    }

    public Location getCurrentLocation(Player player) {
        Location location = playerLocations.get(player);
        if (location == null) {
            throw new IllegalStateException("Player currently not at a location");
        }
        return location;
    }

    public boolean isAtLocation(Player player) {
        return playerLocations.get(player) != null;
    }

    public void movePlayer(Player player, Location to) {
        playerLocations.put(player, to);
    }

    public void removeHazard(HazardType type) {

    }

    public Location getLocation(String name) {
        return getLocations().stream()
                .filter(location -> location.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown location: " + name));
    }

    public Location.TeepeeLocation getTeepeeLocation(int reward) {
        return teepeeLocations.stream()
                .filter(teepeeLocation -> teepeeLocation.getReward() == reward).findAny()
                .orElseThrow(() -> new IllegalArgumentException("No teepee location for " + reward));
    }
}
