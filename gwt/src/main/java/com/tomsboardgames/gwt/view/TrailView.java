package com.tomsboardgames.gwt.view;

import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.gwt.Location;
import com.tomsboardgames.gwt.Teepee;
import com.tomsboardgames.gwt.Trail;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class TrailView {

    Map<String, LocationView> locations;
    LocationView start;
    LocationView kansasCity;
    Map<String, LocationView> teepeeLocations;
    Map<PlayerColor, String> playerLocations;

    TrailView(Trail trail) {
        start = new LocationView(trail.getStart());
        kansasCity = new LocationView(trail.getKansasCity());
        locations = trail.getLocations().stream().collect(Collectors.toMap(Location::getName, LocationView::new));
        // Separate property for teepee locations, because not all locations are on the trail
        teepeeLocations = trail.getTeepeeLocations().stream().collect(Collectors.toMap(Location::getName, LocationView::new));
        playerLocations = trail.getPlayerLocations().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getColor(), entry -> entry.getValue().getName()));
    }

    @Value
    public static class LocationView {

        String name;
        Type type;
        Set<String> next;
        HazardView hazard;
        Teepee teepee;
        Integer reward;
        BuildingView building;

        LocationView(Location location) {
            this.name = location.getName();

            if (location instanceof Location.Start) {
                type = Type.START;
                hazard = null;
                teepee = null;
                reward = null;
                building = null;
            } else if (location instanceof Location.BuildingLocation) {
                type = Type.BUILDING;
                building = ((Location.BuildingLocation) location).getBuilding()
                        .map(BuildingView::new)
                        .orElse(null);
                hazard = null;
                teepee = null;
                reward = null;
            } else if (location instanceof Location.HazardLocation) {
                type = Type.HAZARD;
                hazard = ((Location.HazardLocation) location).getHazard().map(HazardView::new).orElse(null);
                teepee = null;
                reward = null;
                building = null;
            } else if (location instanceof Location.TeepeeLocation) {
                type = Type.TEEPEE;
                teepee = ((Location.TeepeeLocation) location).getTeepee().orElse(null);
                reward = ((Location.TeepeeLocation) location).getReward();
                hazard = null;
                building = null;
            } else if (location instanceof Location.KansasCity) {
                type = Type.KANSAS_CITY;
                hazard = null;
                teepee = null;
                reward = null;
                building = null;
            } else {
                throw new IllegalArgumentException("Unsupported location: " + location);
            }

            next = location.getNext().stream().map(Location::getName).collect(Collectors.toSet());
        }

        public enum Type {
            START,
            BUILDING,
            HAZARD,
            TEEPEE,
            KANSAS_CITY
        }
    }
}
