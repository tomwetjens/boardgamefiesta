package com.wetjens.gwt;

import com.wetjens.gwt.api.Player;
import com.wetjens.gwt.api.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class TrailTest {

    Player playerA = new Player("Player A", PlayerColor.BLUE);
    Player playerB = new Player("Player B", PlayerColor.RED);
    Player playerC = new Player("Player C", PlayerColor.YELLOW);
    Player playerD = new Player("Player D", PlayerColor.WHITE);

    Trail trail;

    @BeforeEach
    void setUp() {
        trail = new Trail(Arrays.asList(playerA, playerB, playerC, playerD), true, new Random(0));

        ((Location.BuildingLocation) trail.getLocation("A-1")).placeBuilding(new PlayerBuilding.Building1A(playerA));
        ((Location.BuildingLocation) trail.getLocation("A-2")).placeBuilding(new PlayerBuilding.Building2A(playerA));

        ((Location.HazardLocation) trail.getLocation("FLOOD-1")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 2));
        ((Location.HazardLocation) trail.getLocation("FLOOD-2")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 2));
    }

    @Test
    void startToA() {
        Location a = trail.getLocation("A");

        assertThat(trail.possibleMoves(trail.getStart(), a, playerA, 0, 3, 4)).containsOnly(
                new PossibleMove(trail.getStart(), Collections.singletonList(a), 0, Collections.emptyMap()));
    }

    @Test
    void backwards() {
        Location a = trail.getLocation("A");

        assertThat(trail.possibleMoves(a, trail.getStart(), playerA, 0, 3, 4)).isEmpty();
    }

    @Test
    void fromToEqual() {
        Location a = trail.getLocation("A");

        assertThat(trail.possibleMoves(a, a, playerA, 0, 6, 4)).isEmpty();
    }

    @Test
    void aToB() {
        Location a = trail.getLocation("A");
        Location b = trail.getLocation("B");

        assertThat(trail.possibleMoves(a, b, playerC, 20, 6, 4)).containsExactlyInAnyOrder(
                new PossibleMove(a, asList(trail.getLocation("A-1"), trail.getLocation("A-2"), b), 1, Collections.singletonMap(playerA, 1)),
                new PossibleMove(a, asList(trail.getLocation("FLOOD-1"), trail.getLocation("FLOOD-2"), b), 2, Collections.emptyMap()));
    }

    @Test
    void twoForks() {
        ((Location.BuildingLocation) trail.getLocation("F-1")).placeBuilding(new PlayerBuilding.Building10A(playerA));
        ((Location.BuildingLocation) trail.getLocation("F-2")).placeBuilding(new PlayerBuilding.Building10A(playerB));
        ((Location.BuildingLocation) trail.getLocation("G-1")).placeBuilding(new PlayerBuilding.Building10A(playerC));
        ((Location.BuildingLocation) trail.getLocation("G-2")).placeBuilding(new PlayerBuilding.Building10A(playerD));

        Location from = trail.getLocation("F");
        Location to = trail.getKansasCity();

        assertThat(trail.possibleMoves(from, to, playerA, 20, 4, 4)).containsExactlyInAnyOrder(
                new PossibleMove(from, asList(trail.getLocation("F-1"), trail.getLocation("G"), trail.getLocation("G-1"), to), 2, Map.of(playerC, 2)),
                new PossibleMove(from, asList(trail.getLocation("F-1"), trail.getLocation("G"), trail.getLocation("G-2"), to), 2, Map.of(playerD, 2)),
                new PossibleMove(from, asList(trail.getLocation("F-2"), trail.getLocation("G"), trail.getLocation("G-1"), to), 4, Map.of(playerC, 2, playerB, 2)),
                new PossibleMove(from, asList(trail.getLocation("F-2"), trail.getLocation("G"), trail.getLocation("G-2"), to), 4, Map.of(playerD, 2, playerB, 2)));
    }

    @Test
    void placeTeepee() {
        trail.placeTeepee(Teepee.GREEN);
        trail.placeTeepee(Teepee.GREEN);
        trail.placeTeepee(Teepee.GREEN);

        assertThat(trail.getTeepeeLocation(-3).getTeepee().get()).isEqualTo(Teepee.GREEN);
        assertThat(trail.getTeepeeLocation(-2).getTeepee().get()).isEqualTo(Teepee.GREEN);
        assertThat(trail.getTeepeeLocation(-1).getTeepee().get()).isEqualTo(Teepee.GREEN);
    }
}
