package com.wetjens.gwt;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Getter;

public class Trail {

    @Getter
    private final Location.Start start;

    private final Set<Location.TeepeeLocation> teepeeLocations;

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

        Location.BuildingLocation g = new Location.BuildingLocation(
                new Location.BuildingLocation(kansasCity),
                new Location.BuildingLocation(kansasCity));
        g.placeBuilding(buildingsToPlace.poll());

        Location.BuildingLocation f = new Location.BuildingLocation(
                new Location.BuildingLocation(g),
                new Location.BuildingLocation(g));
        f.placeBuilding(buildingsToPlace.poll());

        Location.HazardLocation rockfallSection = new Location.HazardLocation(HazardType.ROCKFALL,
                new Location.HazardLocation(HazardType.ROCKFALL,
                        new Location.HazardLocation(HazardType.ROCKFALL,
                                new Location.HazardLocation(HazardType.ROCKFALL,
                                        new Location.BuildingLocation(
                                                new Location.BuildingLocation(f))))));

        Location.BuildingLocation e = new Location.BuildingLocation(
                new Location.BuildingLocation(
                        new Location.BuildingLocation(f)),
                rockfallSection);
        e.placeBuilding(buildingsToPlace.poll());

        Location.BuildingLocation d = new Location.BuildingLocation(e);
        d.placeBuilding(buildingsToPlace.poll());

        Location.TeepeeLocation teepeeLocation10 = new Location.TeepeeLocation(10,
                new Location.BuildingLocation(
                        new Location.BuildingLocation(e)));
        Location.TeepeeLocation teepeeLocation8 = new Location.TeepeeLocation(8, teepeeLocation10);
        Location.TeepeeLocation teepeeLocation6 = new Location.TeepeeLocation(6, teepeeLocation8);
        Location.TeepeeLocation teepeeLocation4 = new Location.TeepeeLocation(4, teepeeLocation6);
        Location.TeepeeLocation teepeeLocation2 = new Location.TeepeeLocation(2, teepeeLocation4);
        Location.TeepeeLocation teepeeLocation1 = new Location.TeepeeLocation(1, teepeeLocation2);

        teepeeLocations = new HashSet<>(Arrays.asList(
                teepeeLocation10,
                teepeeLocation8,
                teepeeLocation6,
                teepeeLocation4,
                teepeeLocation2,
                teepeeLocation1,
                new Location.TeepeeLocation(-1),
                new Location.TeepeeLocation(-2),
                new Location.TeepeeLocation(-3)));

        Location.BuildingLocation crossRoadsIndianTrade = new Location.BuildingLocation(
                new Location.BuildingLocation(d),
                teepeeLocation1);

        Location.BuildingLocation c = new Location.BuildingLocation(
                new Location.BuildingLocation(
                        new Location.BuildingLocation(e)),
                crossRoadsIndianTrade);
        c.placeBuilding(buildingsToPlace.poll());

        Location.HazardLocation droughtSection = new Location.HazardLocation(HazardType.DROUGHT,
                new Location.HazardLocation(HazardType.DROUGHT,
                        new Location.HazardLocation(HazardType.DROUGHT,
                                new Location.HazardLocation(HazardType.DROUGHT,
                                        new Location.BuildingLocation(c)))));  // TODO Add risk action

        Location.BuildingLocation b = new Location.BuildingLocation(
                droughtSection,
                new Location.BuildingLocation(
                        new Location.BuildingLocation(
                                new Location.BuildingLocation(c))));
        b.placeBuilding(buildingsToPlace.poll());

        Location.HazardLocation floodSection = new Location.HazardLocation(HazardType.FLOOD,
                new Location.HazardLocation(HazardType.FLOOD,
                        new Location.HazardLocation(HazardType.FLOOD,
                                new Location.HazardLocation(HazardType.FLOOD,
                                        new Location.BuildingLocation(  // TODO Add risk action
                                                new Location.BuildingLocation(b))))));  // TODO Add risk action
        Location.BuildingLocation a = new Location.BuildingLocation(
                new Location.BuildingLocation(
                        new Location.BuildingLocation(
                                new Location.BuildingLocation(b))),
                floodSection);
        a.placeBuilding(buildingsToPlace.poll());

        start = new Location.Start(a);
    }

    private Stream<Location> getLocations() {
        return getLocations(start);
    }

    private Stream<Location> getLocations(Location from) {
        return Stream.concat(Stream.of(from), from.getNext().stream().flatMap(this::getLocations));
    }

    public Stream<Location.HazardLocation> getHazardLocations() {
        return getLocations()
                .filter(location -> location instanceof Location.HazardLocation)
                .map(location -> (Location.HazardLocation) location);
    }

    public Stream<Location.TeepeeLocation> getTeepeeLocations() {
        return getLocations()
                .filter(location -> location instanceof Location.TeepeeLocation)
                .map(location -> (Location.TeepeeLocation) location);
    }

    public Location.BuildingLocation getBuildingLocation(Class<? extends NeutralBuilding> clazz) {
        return getLocations()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .filter(buildingLocation -> buildingLocation.getBuilding()
                        .map(building -> clazz.equals(building.getClass()))
                        .orElse(false))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Neutral building not on trail: " + clazz.getSimpleName()));
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

}
