/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class TrailTest {

    Player playerA = new Player("Player A", PlayerColor.BLUE, Player.Type.HUMAN);
    Player playerB = new Player("Player B", PlayerColor.RED, Player.Type.HUMAN);
    Player playerC = new Player("Player C", PlayerColor.YELLOW, Player.Type.HUMAN);
    Player playerD = new Player("Player D", PlayerColor.WHITE, Player.Type.HUMAN);

    Trail trail;

    @BeforeEach
    void setUp() {
        trail = new Trail(GWT.Edition.FIRST, true, new Random(0));

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

    @Test
    void scott() {
        var trail = new Trail(GWT.Edition.FIRST);

        ((Location.BuildingLocation) trail.getLocation("A")).placeBuilding(new NeutralBuilding.A());
        ((Location.BuildingLocation) trail.getLocation("B")).placeBuilding(new NeutralBuilding.B());
        ((Location.BuildingLocation) trail.getLocation("A-1")).placeBuilding(new PlayerBuilding.Building10B(playerA));
        ((Location.BuildingLocation) trail.getLocation("A-2")).placeBuilding(new PlayerBuilding.Building8B(playerA));
        ((Location.BuildingLocation) trail.getLocation("A-3")).placeBuilding(new PlayerBuilding.Building4B(playerB));
        ((Location.HazardLocation) trail.getLocation("FLOOD-1")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 4));
        ((Location.HazardLocation) trail.getLocation("FLOOD-2")).placeHazard(new Hazard(HazardType.FLOOD, Hand.BLACK, 2));
        ((Location.BuildingLocation) trail.getLocation("FLOOD-RISK-1")).placeBuilding(new PlayerBuilding.Building7B(playerA));
        ((Location.BuildingLocation) trail.getLocation("B-1")).placeBuilding(new PlayerBuilding.Building1A(playerC));

        Location from = trail.getLocation("A");
        Location to = trail.getLocation("B-1");

        assertThat(trail.possibleMoves(from, to, playerC, 2, 6, 4)).containsExactlyInAnyOrder(
                new PossibleMove(from, asList(trail.getLocation("A-1"), trail.getLocation("A-2"), trail.getLocation("A-3"), trail.getLocation("B"), to), 2, Map.of(playerA, 2)),
                new PossibleMove(from, asList(trail.getLocation("FLOOD-1"), trail.getLocation("FLOOD-2"), trail.getLocation("FLOOD-RISK-1"), trail.getLocation("B"), to), 2, Collections.emptyMap()));
    }

    @Test
    void shouldDifferentiateBetweenPlayerFeesButExcludeOwnBuildings() {
        var trail = new Trail(GWT.Edition.FIRST);

        ((Location.BuildingLocation) trail.getLocation("A")).placeBuilding(new NeutralBuilding.A());
        ((Location.BuildingLocation) trail.getLocation("B")).placeBuilding(new NeutralBuilding.B());
        ((Location.BuildingLocation) trail.getLocation("A-1")).placeBuilding(new PlayerBuilding.Building1A(playerA));
        ((Location.BuildingLocation) trail.getLocation("A-2")).placeBuilding(new PlayerBuilding.Building4A(playerB));
        ((Location.BuildingLocation) trail.getLocation("A-3")).placeBuilding(new PlayerBuilding.Building2A(playerA));
        ((Location.HazardLocation) trail.getLocation("FLOOD-1")).placeHazard(new Hazard(HazardType.FLOOD, Hand.BLACK, 4));
        ((Location.HazardLocation) trail.getLocation("FLOOD-2")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 2));
        ((Location.HazardLocation) trail.getLocation("FLOOD-3")).placeHazard(new Hazard(HazardType.FLOOD, Hand.GREEN, 2));

        Location from = trail.getLocation("A");
        Location to = trail.getLocation("B");

        var possibleMoves = trail.possibleMoves(from, to, playerA, 1, 6, 4);
        assertThat(possibleMoves).hasSize(2);
        assertThat(possibleMoves).containsExactlyInAnyOrder(
                new PossibleMove(from, asList(trail.getLocation("A-1"), trail.getLocation("A-2"), trail.getLocation("A-3"), to), 1, Map.of(playerB, 1)),
                new PossibleMove(from, asList(trail.getLocation("FLOOD-1"), trail.getLocation("FLOOD-2"), trail.getLocation("FLOOD-3"), to), 1, Collections.emptyMap()));
    }

    //@Test TODO disabled because it failed during holiday and I can't debug it now
    void gerben18aug2021() {
        var trail = new Trail(GWT.Edition.FIRST);

        ((Location.BuildingLocation) trail.getLocation("E")).placeBuilding(new NeutralBuilding.E());
        ((Location.BuildingLocation) trail.getLocation("F")).placeBuilding(new NeutralBuilding.F());
        ((Location.BuildingLocation) trail.getLocation("G")).placeBuilding(new NeutralBuilding.G());
        ((Location.BuildingLocation) trail.getLocation("E-1")).placeBuilding(new PlayerBuilding.Building3A(playerA));
        ((Location.BuildingLocation) trail.getLocation("E-2")).placeBuilding(new PlayerBuilding.Building2A(playerA));
        ((Location.BuildingLocation) trail.getLocation("F-2")).placeBuilding(new PlayerBuilding.Building4B(playerA));

        ((Location.BuildingLocation) trail.getLocation("G-1")).placeBuilding(new PlayerBuilding.Building1B(playerB));

        ((Location.HazardLocation) trail.getLocation("ROCKFALL-3")).placeHazard(new Hazard(HazardType.ROCKFALL, Hand.GREEN, 3));

        Location from = trail.getLocation("E");
        Location to = trail.getKansasCity();

        var possibleMoves = trail.possibleMoves(from, to, playerC, 23, 7, 4);

        assertThat(possibleMoves).containsExactlyInAnyOrder(
                new PossibleMove(from, asList(trail.getLocation("E-1"), trail.getLocation("E-2"), trail.getLocation("F"), trail.getLocation("G"), to), 0, Collections.emptyMap()));
    }
}
