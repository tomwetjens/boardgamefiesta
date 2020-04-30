package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.*;

import static com.wetjens.gwt.RailroadTrack.MAX_CERTIFICATES;
import static com.wetjens.gwt.RailroadTrack.MAX_HAND_VALUE;
import static com.wetjens.gwt.RailroadTrack.MIN_HAND_VALUE;
import static org.assertj.core.api.Assertions.*;

class RailroadTrackTest {

    private RailroadTrack railroadTrack;

    @BeforeEach
    void setUp() {
        railroadTrack = new RailroadTrack(Arrays.asList(Player.BLUE, Player.RED, Player.WHITE, Player.YELLOW), new Random(0));
    }

    @Test
    void create() {

    }

    @Test
    void moveEngineForwardOptional1() {
        // When
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
    }

    @Test
    void moveEngineForwardOptional2() {
        // When
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(2), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardNotFarEnough() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 2, 6)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
    }

    @Test
    void moveEngineForwardTooFar() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(2), 0, 1)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
    }

    @Test
    void moveEngineForwardJumpOver1() {
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);

        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.RED)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardJumpOver2() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(3), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.WHITE)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineForwardJumpOverPlusOne() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(4), 1, 2);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.WHITE)).getNumber()).isEqualTo(4);
    }

    @Test
    void moveEngineForwardJumpOverTooFar() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(4), 1, 1)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
    }

    @Test
    void moveEngineForwardJumpOverNotFarEnough() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(3), 2, 2)).hasMessage(GWTError.SPACE_NOT_REACHABLE.toString());
    }

    @Test
    void moveEngineForwardJumpOver3() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(3), 1, 1);

        // When
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(4), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.WHITE)).getNumber()).isEqualTo(3);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.YELLOW)).getNumber()).isEqualTo(4);
    }

    @Test
    void moveEngineForwardAlreadyPlayerOnSpace() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(1), 1, 1))
                .hasMessage(GWTError.ALREADY_PLAYER_ON_SPACE.toString());
    }

    @Test
    void moveEngineBackwardsJumpOver() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(3), 1, 1);
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(4), 1, 1);

        // When
        railroadTrack.moveEngineBackwards(Player.YELLOW, railroadTrack.getStart(), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.WHITE)).getNumber()).isEqualTo(3);
        assertThat(railroadTrack.currentSpace(Player.YELLOW)).isSameAs(railroadTrack.getStart());
    }

    @Test
    void moveEngineBackwardsJumpOverIntoGapNormalSpace() {
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 2);
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(4), 1, 3);
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(5), 1, 4);

        railroadTrack.moveEngineBackwards(Player.YELLOW, railroadTrack.getSpace(3), 1, 1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.WHITE)).getNumber()).isEqualTo(4);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.YELLOW)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineBackwardsToStart() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);

        // When
        railroadTrack.moveEngineBackwards(Player.BLUE, railroadTrack.getStart(), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.currentSpace(Player.BLUE))).isSameAs(railroadTrack.getStart());
    }

    @Test
    void moveEngineBackwardsAlreadyPlayerOnSpace() {
        // Given
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineBackwards(Player.RED, railroadTrack.getSpace(1), 1, 1))
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

            var possibleDeliveries = railroadTrack.possibleDeliveries(Player.BLUE, MIN_HAND_VALUE, 0);

            assertThat(possibleDeliveries).containsExactlyInAnyOrder(
                    new RailroadTrack.PossibleDelivery(City.KANSAS_CITY, 0, MIN_HAND_VALUE),
                    new RailroadTrack.PossibleDelivery(City.TOPEKA, 0, MIN_HAND_VALUE),
                    new RailroadTrack.PossibleDelivery(City.WICHITA, 0, 4)
            );
        }

        @Test
        void alreadyDelivered() {
            var cities = new HashMap<City, List<Player>>();
            cities.put(City.TOPEKA, new LinkedList<>(Collections.singleton(Player.BLUE)));
            var railroadTrack = RailroadTrack.builder().cities(cities).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(Player.BLUE, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).doesNotContain(new RailroadTrack.PossibleDelivery(City.TOPEKA, 0, MAX_HAND_VALUE));
        }

        @Test
        void alreadyDeliveredKansasCity() {
            var cities = new HashMap<City, List<Player>>();
            cities.put(City.KANSAS_CITY, new LinkedList<>(Collections.singleton(Player.BLUE)));
            var railroadTrack = RailroadTrack.builder().cities(cities).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(Player.BLUE, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.KANSAS_CITY, 0, MAX_HAND_VALUE));
        }

        @Test
        void alreadyDeliveredSanFrancisco() {
            var cities = new HashMap<City, List<Player>>();
            cities.put(City.SAN_FRANCISCO, new LinkedList<>(Collections.singleton(Player.BLUE)));
            var railroadTrack = RailroadTrack.builder().cities(cities).build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(Player.BLUE, MAX_HAND_VALUE, 0);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.SAN_FRANCISCO, 0, 17));
        }

        @Test
        void minHandValueMaxCertificates() {
            var railroadTrack = RailroadTrack.builder()
                    .cities(new HashMap<>())
                    .build();

            var possibleDeliveries = railroadTrack.possibleDeliveries(Player.BLUE, MIN_HAND_VALUE, MAX_CERTIFICATES);

            assertThat(possibleDeliveries).contains(new RailroadTrack.PossibleDelivery(City.COLORADO_SPRINGS, 1, 2));
        }
    }
}
