package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectiveCardTest {

    @Mock
    GWT game;

    @Mock
    Player player;

    @Mock
    PlayerState playerState;

    @Mock
    Trail trail;

    @Mock
    RailroadTrack railroadTrack;

    @Nested
    class Score {

        @BeforeEach
        void setUp() {
            when(game.playerState(player)).thenReturn(playerState);
            when(game.getTrail()).thenReturn(trail);
            when(game.getRailroadTrack()).thenReturn(railroadTrack);
        }

        @Test
        void test() {
            var start = new ObjectiveCard(ObjectiveCard.Type.START_BLHH);
            var a = new ObjectiveCard(ObjectiveCard.Type.GAIN2_SSH);
            var b = new ObjectiveCard(ObjectiveCard.Type.ENGINE_BBLHH);

            when(game.getRailroadTrack().numberOfUpgradedStations(player)).thenReturn(5);
            when(playerState.numberOfHazards()).thenReturn(3);
            when(playerState.numberOfTeepees()).thenReturn(1);
            when(trail.numberOfBuildings(player)).thenReturn(3);

            var result = ObjectiveCard.score(Set.of(start, a, b), Collections.emptySet(), game, player, false);
            assertThat(result.getTotal()).isEqualTo(8);
        }

        @Test
        void stationMasterTile() {
            ObjectiveCard a = objectiveCard("a");
            ObjectiveCard b = objectiveCard("b");

            when(playerState.numberOfHazards()).thenReturn(1);

            // When player has the station master tile "3 points per pair of committed objective cards"
            // Then committing to "a" and "b" makes sense, giving a lower score on objective cards (-1), but a higher score overall (4)

            var result = ObjectiveCard.score(Collections.emptySet(), Set.of(a, b), game, player, true);
            assertThat(result.getCommitted()).containsExactlyInAnyOrder(a, b);
            assertThat(result.getTotal()).isEqualTo(1);
        }

        private ObjectiveCard objectiveCard(String name) {
            var b = mock(ObjectiveCard.class, name);
            lenient().when(b.getTasks()).thenReturn(Collections.singletonList(ObjectiveCard.Task.HAZARD));
            lenient().when(b.getPoints()).thenReturn(3);
            lenient().when(b.getPenalty()).thenReturn(2);
            return b;
        }

        @Test
        void scott() {
            var start = new ObjectiveCard(ObjectiveCard.Type.START_BLHH);
            var a = new ObjectiveCard(ObjectiveCard.Type.GAIN2_BGBL);
            var b = new ObjectiveCard(ObjectiveCard.Type.ENGINE_SSHH);

            when(game.getRailroadTrack().numberOfUpgradedStations(player)).thenReturn(0);
            when(playerState.numberOfHazards()).thenReturn(3);
            when(playerState.numberOfTeepees()).thenReturn(2);
            when(playerState.numberOfGreenTeepees()).thenReturn(1);
            when(trail.numberOfBuildings(player)).thenReturn(2);

            var result = ObjectiveCard.score(Set.of(start, a), Set.of(b), game, player, false);
            assertThat(result.getTotal()).isEqualTo(3);
        }

        @Test
        void scott2() {
            var start = new ObjectiveCard(ObjectiveCard.Type.START_34B); // 3/0, not completed
            var a = new ObjectiveCard(ObjectiveCard.Type.GAIN2_BGBL); // 3/-2, completed, committed
            var b = new ObjectiveCard(ObjectiveCard.Type.GAIN2_BBLBL); // 3/-2, completed, committed

            when(game.getRailroadTrack().numberOfUpgradedStations(player)).thenReturn(0);
            when(playerState.numberOfHazards()).thenReturn(2);
            when(playerState.numberOfTeepees()).thenReturn(3);
            when(playerState.numberOfGreenTeepees()).thenReturn(1);
            when(trail.numberOfBuildings(player)).thenReturn(2);

            var result = ObjectiveCard.score(Set.of(start, a, b), Collections.emptySet(), game, player, false);
            assertThat(result.getTotal()).isEqualTo(1);
        }

        @Test
        void anotherCase() {
            var start = new ObjectiveCard(ObjectiveCard.Type.START_SSG);
            var a = new ObjectiveCard(ObjectiveCard.Type.DRAW_BBH);
            var b = new ObjectiveCard(ObjectiveCard.Type.DRAW_5H);
            var c = new ObjectiveCard(ObjectiveCard.Type.DRAW_333S);
            var d = new ObjectiveCard(ObjectiveCard.Type.AUX_SF);

            when(game.getRailroadTrack().numberOfUpgradedStations(player)).thenReturn(0);
            when(playerState.numberOfHazards()).thenReturn(1);
            when(playerState.numberOfTeepees()).thenReturn(0);
            when(playerState.numberOfGreenTeepees()).thenReturn(0);
            when(trail.numberOfBuildings(player)).thenReturn(3);

            var result = ObjectiveCard.score(Set.of(start, a, b, c, d), Collections.emptySet(), game, player, false);
            assertThat(result.getTotal()).isEqualTo(-4);
        }
    }

}