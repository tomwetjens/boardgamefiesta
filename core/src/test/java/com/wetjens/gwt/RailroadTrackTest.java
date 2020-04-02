package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.*;

import static com.wetjens.gwt.RailroadTrack.MAX_CERTIFICATES;
import static com.wetjens.gwt.RailroadTrack.MAX_HAND_VALUE;
import static com.wetjens.gwt.RailroadTrack.MIN_HAND_VALUE;
import static org.assertj.core.api.Assertions.*;

class RailroadTrackTest {

    private RailroadTrack railroadTrack;
    private List<Player> players;
    private Player playerA;
    private Player playerB;
    private Player playerC;
    private Player playerD;

    @BeforeEach
    void setUp() {
        playerA = new Player("A", Player.Color.BLUE);
        playerB = new Player("B", Player.Color.RED);
        playerC = new Player("C", Player.Color.WHITE);
        playerD = new Player("D", Player.Color.YELLOW);

        players = Arrays.asList(playerA, playerB, playerC, playerD);

        railroadTrack = new RailroadTrack(players, new Random(0));
    }

    @Test
    void create() {

    }

    @Test
    void moveEngineForwardOptional1() {
        // When
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
    }

    @Test
    void moveEngineForwardOptional2() {
        // When
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(2), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardNotFarEnough() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 2, 6)).hasMessage("Space not reachable within 2..6 steps");
    }

    @Test
    void moveEngineForwardTooFar() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(2), 0, 1)).hasMessage("Space not reachable within 0..1 steps");
    }

    @Test
    void moveEngineForwardJumpOver1() {
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);

        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerB)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardJumpOver2() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(3), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerC)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineForwardJumpOverPlusOne() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(4), 1, 2);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerC)).getNumber()).isEqualTo(4);
    }

    @Test
    void moveEngineForwardJumpOverTooFar() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(4), 1, 1)).hasMessage("Space not reachable within 1..1 steps");
    }

    @Test
    void moveEngineForwardJumpOverNotFarEnough() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(3), 2, 2)).hasMessage("Space not reachable within 2..2 steps");
    }

    @Test
    void moveEngineForwardJumpOver3() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(3), 1, 1);

        // When
        railroadTrack.moveEngineForward(playerD, railroadTrack.getSpace(4), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerC)).getNumber()).isEqualTo(3);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerD)).getNumber()).isEqualTo(4);
    }

    @Test
    void moveEngineForwardAlreadyPlayerOnSpace() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(1), 1, 1))
                .hasMessage("Another player already on space");
    }

    @Test
    void moveEngineBackwardsJumpOver() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(3), 1, 1);
        railroadTrack.moveEngineForward(playerD, railroadTrack.getSpace(4), 1, 1);

        // When
        railroadTrack.moveEngineBackwards(playerD, railroadTrack.getStart(), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerC)).getNumber()).isEqualTo(3);
        assertThat(railroadTrack.currentSpace(playerD)).isSameAs(railroadTrack.getStart());
    }

    @Test
    void moveEngineBackwardsJumpOverIntoGapNormalSpace() {
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 2);
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(4), 1, 3);
        railroadTrack.moveEngineForward(playerD, railroadTrack.getSpace(5), 1, 4);

        railroadTrack.moveEngineBackwards(playerD, railroadTrack.getSpace(3), 1, 1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerC)).getNumber()).isEqualTo(4);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerD)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineBackwardsToStart() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);

        // When
        railroadTrack.moveEngineBackwards(playerA, railroadTrack.getStart(), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(playerA))).isSameAs(railroadTrack.getStart());
    }

    @Test
    void moveEngineBackwardsAlreadyPlayerOnSpace() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineBackwards(playerB, railroadTrack.getSpace(1), 1, 1))
                .hasMessage("Another player already on space");
    }

    @Test
    void moveEngineBackwardsJumpOverToTurnout() {

    }

    @Nested
    class PossibleDeliveries {

        @Test
        void minHandValue() {
            RailroadTrack railroadTrack = RailroadTrack.builder().cities(new HashMap<>()).build();

            Set<RailroadTrack.PossibleDelivery> possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MIN_HAND_VALUE, 0);

            assertThat(possibleDeliveries).containsExactlyInAnyOrder(
                    new RailroadTrack.PossibleDelivery(City.KANSAS_CITY, 0),
                    new RailroadTrack.PossibleDelivery(City.TOPEKA, 0),
                    new RailroadTrack.PossibleDelivery(City.WICHITA, 0)
            );
        }

        @Test
        void alreadyDelivered() {
            HashMap<City, List<Player>> cities = new HashMap<>();
            cities.put(City.TOPEKA, new LinkedList<>(Collections.singleton(playerA)));
            RailroadTrack railroadTrack = RailroadTrack.builder().cities(cities).build();

            Set<RailroadTrack.PossibleDelivery> possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).doesNotContain(new RailroadTrack.PossibleDelivery(City.TOPEKA, 0));
        }

        @Test
        void alreadyDeliveredKansasCity() {
            HashMap<City, List<Player>> cities = new HashMap<>();
            cities.put(City.KANSAS_CITY, new LinkedList<>(Collections.singleton(playerA)));
            RailroadTrack railroadTrack = RailroadTrack.builder().cities(cities).build();

            Set<RailroadTrack.PossibleDelivery> possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.KANSAS_CITY, 0));
        }

        @Test
        void alreadyDeliveredSanFrancisco() {
            HashMap<City, List<Player>> cities = new HashMap<>();
            cities.put(City.SAN_FRANCISCO, new LinkedList<>(Collections.singleton(playerA)));
            RailroadTrack railroadTrack = RailroadTrack.builder().cities(cities).build();

            Set<RailroadTrack.PossibleDelivery> possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.SAN_FRANCISCO, 0));
        }

        @Test
        void minHandValueMaxCertificates() {
            RailroadTrack railroadTrack = RailroadTrack.builder()
                    .cities(new HashMap<>())
                    .build();

            Set<RailroadTrack.PossibleDelivery> possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MIN_HAND_VALUE, MAX_CERTIFICATES);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.COLORADO_SPRINGS, 1));
        }
    }
}
