package com.wetjens.gwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RailroadTrackTest {

    private RailroadTrack railroadTrack;

    @BeforeEach
    void setUp() {
        railroadTrack = new RailroadTrack(Arrays.asList(Player.values()), new Random(0));
    }

    @Test
    void create() {

    }

    @Test
    void moveEngineForwardOptional1() {
        // When
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
    }

    @Test
    void moveEngineForwardOptional2() {
        // When
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(2), 0, 6);

        // Then
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardNotFarEnough() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 2, 6)).hasMessage("Space not reachable within 2..6 steps");
    }

    @Test
    void moveEngineForwardTooFar() {
        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(2), 0, 1)).hasMessage("Space not reachable within 0..1 steps");
    }

    @Test
    void moveEngineForwardJumpOver1() {
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);

        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.RED)).getNumber()).isEqualTo(2);
    }

    @Test
    void moveEngineForwardJumpOver2() {
        // Given
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(3), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.BLUE)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineForwardJumpOverPlusOne() {
        // Given
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(4), 1, 2);

        // Then
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.BLUE)).getNumber()).isEqualTo(4);
    }

    @Test
    void moveEngineForwardJumpOverTooFar() {
        // Given
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(4), 1, 1)).hasMessage("Space not reachable within 1..1 steps");
    }

    @Test
    void moveEngineForwardJumpOverNotFarEnough() {
        // Given
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);

        // When
        assertThatThrownBy(() -> railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(3), 2, 2)).hasMessage("Space not reachable within 2..2 steps");
    }

    @Test
    void moveEngineForwardJumpOver3() {
        // Given
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(3), 1, 1);

        // When
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(4), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.BLUE)).getNumber()).isEqualTo(3);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.WHITE)).getNumber()).isEqualTo(4);
    }

    @Test
    void moveEngineBackwardsJumpOver() {
        // Given
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 1);
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(3), 1, 1);
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(4), 1, 1);

        // When
        railroadTrack.moveEngineBackwards(Player.WHITE, railroadTrack.getStart(), 1, 1);

        // Then
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.BLUE)).getNumber()).isEqualTo(3);
        assertThat(railroadTrack.current(Player.WHITE)).isSameAs(railroadTrack.getStart());
    }

    @Test
    void moveEngineBackwardsJumpOverGapNormalSpace() {
        railroadTrack.moveEngineForward(Player.YELLOW, railroadTrack.getSpace(1), 1, 1);
        railroadTrack.moveEngineForward(Player.RED, railroadTrack.getSpace(2), 1, 2);
        railroadTrack.moveEngineForward(Player.BLUE, railroadTrack.getSpace(4), 1, 3);
        railroadTrack.moveEngineForward(Player.WHITE, railroadTrack.getSpace(5), 1, 4);

        railroadTrack.moveEngineBackwards(Player.WHITE, railroadTrack.getSpace(1), 1, 1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.YELLOW)).getNumber()).isEqualTo(1);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.RED)).getNumber()).isEqualTo(2);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.BLUE)).getNumber()).isEqualTo(4);
        assertThat(((RailroadTrack.Space.NormalSpace) railroadTrack.current(Player.WHITE)).getNumber()).isEqualTo(3);
    }

    @Test
    void moveEngineBackwardsJumpOverToTurnout() {

    }
}