package com.wetjens.gwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class TrailTest {

    Trail trail;

    @BeforeEach
    void setUp() {
        trail = new Trail(Arrays.asList(Player.BLUE, Player.YELLOW, Player.RED, Player.WHITE), true, new Random(0));

        ((Location.BuildingLocation) trail.getLocation("A-1")).placeBuilding(new PlayerBuilding.Building1A(Player.BLUE));
        ((Location.BuildingLocation) trail.getLocation("A-2")).placeBuilding(new PlayerBuilding.Building2A(Player.BLUE));

        ((Location.HazardLocation) trail.getLocation("FLOOD-1")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 2));
        ((Location.HazardLocation) trail.getLocation("FLOOD-2")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 2));
    }

    @Test
    void startToA() {
        Location a = trail.getLocation("A");

        assertThat(trail.possibleMoves(trail.getStart(), a, 3)).containsOnly(Collections.singletonList(a));
    }

    @Test
    void backwards() {
        Location a = trail.getLocation("A");

        assertThat(trail.possibleMoves(a, trail.getStart(), 3)).isEmpty();
    }

    @Test
    void fromToEqual() {
        Location a = trail.getLocation("A");

        assertThat(trail.possibleMoves(a, a, 6)).isEmpty();
    }

    @Test
    void aToB() {
        Location a = trail.getLocation("A");
        Location b = trail.getLocation("B");

        assertThat(trail.possibleMoves(a, b, 6)).containsExactlyInAnyOrder(
                asList(trail.getLocation("A-1"), trail.getLocation("A-2"), b),
                asList(trail.getLocation("FLOOD-1"), trail.getLocation("FLOOD-2"), b));
    }

    @Test
    void twoForks() {
        ((Location.BuildingLocation) trail.getLocation("F-1")).placeBuilding(new PlayerBuilding.Building10A(Player.BLUE));
        ((Location.BuildingLocation) trail.getLocation("F-2")).placeBuilding(new PlayerBuilding.Building10A(Player.YELLOW));
        ((Location.BuildingLocation) trail.getLocation("G-1")).placeBuilding(new PlayerBuilding.Building10A(Player.RED));
        ((Location.BuildingLocation) trail.getLocation("G-2")).placeBuilding(new PlayerBuilding.Building10A(Player.WHITE));

        Location from = trail.getLocation("F");
        Location to = trail.getKansasCity();

        assertThat(trail.possibleMoves(from, to, 4)).containsExactlyInAnyOrder(
                asList(trail.getLocation("F-1"), trail.getLocation("G"), trail.getLocation("G-1"), to),
                asList(trail.getLocation("F-1"), trail.getLocation("G"), trail.getLocation("G-2"), to),
                asList(trail.getLocation("F-2"), trail.getLocation("G"), trail.getLocation("G-1"), to),
                asList(trail.getLocation("F-2"), trail.getLocation("G"), trail.getLocation("G-2"), to));
    }
}
