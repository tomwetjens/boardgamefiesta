package com.wetjens.gwt;

import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.*;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

class TrailTest {

    private Trail trail;

    @BeforeEach
    void setUp() {
        trail = new Trail(new Random(0));

        Player playerA = new Player("Player A", Player.Color.BLUE);

        ((Location.BuildingLocation) trail.getLocation("A-1")).placeBuilding(new PlayerBuilding.Building1A(playerA));
        ((Location.BuildingLocation) trail.getLocation("A-2")).placeBuilding(new PlayerBuilding.Building2A(playerA));

        ((Location.HazardLocation) trail.getLocation("FLOOD-1")).placeHazard(new Hazard(HazardType.FLOOD, Fee.GREEN, 2));
        ((Location.HazardLocation) trail.getLocation("FLOOD-2")).placeHazard(new Hazard(HazardType.FLOOD, Fee.GREEN, 2));
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
}
