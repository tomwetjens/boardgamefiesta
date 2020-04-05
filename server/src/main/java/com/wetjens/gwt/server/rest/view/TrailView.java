package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.Location;
import com.wetjens.gwt.Trail;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
public class TrailView {

    private Map<String, LocationView> locations;
    private LocationView start;
    private LocationView kansasCity;
    private List<LocationView> teepeeLocations;
    private final Map<String, String> playerLocations;

    TrailView(Trail trail) {
        start = new LocationView(trail.getStart());
        kansasCity = new LocationView(trail.getKansasCity());
        locations = trail.getLocations().stream().collect(Collectors.toMap(Location::getName, LocationView::new));
        teepeeLocations = trail.getTeepeeLocations().stream().map(LocationView::new).collect(Collectors.toList());
        playerLocations = trail.getPlayerLocations().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getName(), entry -> entry.getValue().getName()));
    }

    @Value
    public static class LocationView {

        String name;
        Type type;
        Set<String> next;

        LocationView(Location location) {
            this.name = location.getName();

            if (location instanceof Location.Start) {
                type = Type.START;
            } else if (location instanceof Location.BuildingLocation) {
                type = Type.BUILDING;
            } else if (location instanceof Location.HazardLocation) {
                type = Type.HAZARD;
            } else if (location instanceof Location.TeepeeLocation) {
                type = Type.TEEPEE;
            } else if (location instanceof Location.KansasCity) {
                type = Type.KANSAS_CITY;
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
