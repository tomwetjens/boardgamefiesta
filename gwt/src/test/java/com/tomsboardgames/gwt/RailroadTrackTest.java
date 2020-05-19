package com.tomsboardgames.gwt;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.tomsboardgames.gwt.RailroadTrack.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RailroadTrackTest {

    private RailroadTrack railroadTrack;
    private Player playerA = new Player("Player A", PlayerColor.BLUE);
    private Player playerB = new Player("Player B", PlayerColor.RED);
    private Player playerC = new Player("Player C", PlayerColor.WHITE);
    private Player playerD = new Player("Player D", PlayerColor.YELLOW);

    @BeforeEach
    void setUp() {
        railroadTrack = new RailroadTrack(new LinkedHashSet<>(Arrays.asList(playerA, playerB, playerC, playerD)), new Random(0));
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
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 2, 6)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
    }

    @Test
    void moveEngineForwardTooFar() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(2), 0, 1)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
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
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(4), 1, 1)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
    }

    @Test
    void moveEngineForwardJumpOverNotFarEnough() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(3), 2, 2)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
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
                .hasMessage(GWTError.ALREADY_PLAYER_ON_SPACE.toString());
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
                .hasMessage(GWTError.ALREADY_PLAYER_ON_SPACE.toString());
    }

    @Test
    void moveEngineBackwardsJumpOverToTurnout() {

    }

    @Nested
    class PossibleDeliveries {

        @Test
        void minHandValue() {
            var railroadTrack = RailroadTrack.builder().cities(new HashMap<>()).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MIN_HAND_VALUE, 0);

            assertThat(possibleDeliveries).containsExactlyInAnyOrder(
                    new RailroadTrack.PossibleDelivery(City.KANSAS_CITY, 0, MIN_HAND_VALUE),
                    new RailroadTrack.PossibleDelivery(City.TOPEKA, 0, MIN_HAND_VALUE),
                    new RailroadTrack.PossibleDelivery(City.WICHITA, 0, 4)
            );
        }

        @Test
        void alreadyDelivered() {
            var cities = new HashMap<City, List<Player>>();
            cities.put(City.TOPEKA, new LinkedList<>(Collections.singleton(playerA)));
            var railroadTrack = RailroadTrack.builder().cities(cities).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).doesNotContain(new RailroadTrack.PossibleDelivery(City.TOPEKA, 0, MAX_HAND_VALUE));
        }

        @Test
        void alreadyDeliveredKansasCity() {
            var cities = new HashMap<City, List<Player>>();
            cities.put(City.KANSAS_CITY, new LinkedList<>(Collections.singleton(playerA)));
            var railroadTrack = RailroadTrack.builder().cities(cities).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.KANSAS_CITY, 0, MAX_HAND_VALUE));
        }

        @Test
        void alreadyDeliveredSanFrancisco() {
            var cities = new HashMap<City, List<Player>>();
            cities.put(City.SAN_FRANCISCO, new LinkedList<>(Collections.singleton(playerA)));
            var railroadTrack = RailroadTrack.builder().cities(cities).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.SAN_FRANCISCO, 0, 17));
        }

        @Test
        void minHandValueMaxCertificates() {
            var railroadTrack = RailroadTrack.builder()
                    .cities(new HashMap<>())
                    .build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(playerA, MIN_HAND_VALUE, MAX_CERTIFICATES);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.COLORADO_SPRINGS, 1, 2));
        }
    }

    @Nested
    class ReachableSpacesForward {

        @Test
        void turnOutAndNormalJumpOverOtherPlayer() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA, playerB), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(3), 0, 6);
            railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(4), 0, 6);

            assertThat(railroadTrack.reachableSpacesForward(railroadTrack.getSpace(3), 1, 2)).containsExactlyInAnyOrder(
                    railroadTrack.getTurnouts().get(0),
                    railroadTrack.getSpace(5),
                    railroadTrack.getSpace(6));
        }
    }

    @Nested
    class SignalsPassed {

        @Test
        void atTurnoutBetween4And5() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getTurnouts().get(0), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(2);
        }

        @Test
        void atTurnoutBetween7And8() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getTurnouts().get(1), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(4);
        }

        @Test
        void atTurnoutBetween10And11() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getTurnouts().get(2), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(6);
        }

        @Test
        void atTurnoutBetween13And14() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getTurnouts().get(3), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(8);
        }

        @Test
        void atTurnoutBetween16And17() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getTurnouts().get(4), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(10);
        }

        @Test
        void at16() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(16), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(9);
        }

        @Test
        void at17() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(17), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(10);
        }

        @Test
        void at18() {
            RailroadTrack railroadTrack = new RailroadTrack(Set.of(playerA), new Random(0));
            railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(18), 0, Integer.MAX_VALUE);

            assertThat(railroadTrack.signalsPassed(playerA)).isEqualTo(11);
        }
    }
}
