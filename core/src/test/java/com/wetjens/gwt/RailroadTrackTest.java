package com.wetjens.gwt;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.*;

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
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
    }

    @Test
    void moveEngineForwardOptional2() {
        // When
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(2), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(2);
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
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerB)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardJumpOver2() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(3), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerC)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineForwardJumpOverPlusOne() {
        // Given
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(4), 1, 2);

        // Then
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerC)).getNumber()).isEqualTo(4);
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
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerC)).getNumber()).isEqualTo(3);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerD)).getNumber()).isEqualTo(4);
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
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerC)).getNumber()).isEqualTo(3);
        assertThat(railroadTrack.current(playerD)).isSameAs(railroadTrack.getStart());
    }

    @Test
    void moveEngineBackwardsJumpOverIntoGapNormalSpace() {
        railroadTrack.moveEngineForward(playerA, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(playerB, railroadTrack.getSpace(2), 1, 2);
        railroadTrack.moveEngineForward(playerC, railroadTrack.getSpace(4), 1, 3);
        railroadTrack.moveEngineForward(playerD, railroadTrack.getSpace(5), 1, 4);

        railroadTrack.moveEngineBackwards(playerD, railroadTrack.getSpace(3), 1, 1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerA)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerB)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerC)).getNumber()).isEqualTo(4);
        assertThat(((RailroadTrack.Space.NumberedSpace) railroadTrack.current(playerD)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineBackwardsJumpOverToTurnout() {

    }
}
