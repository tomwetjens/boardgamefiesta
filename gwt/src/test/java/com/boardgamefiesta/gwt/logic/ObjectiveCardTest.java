package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectiveCardTest {

    @Mock
    Game game;

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
            var start = new ObjectiveCard(null, List.of(ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 3, 0);
            var a = new ObjectiveCard(null, List.of(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.HAZARD), 3, 2);
            var b = new ObjectiveCard(null, List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 3);

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
        void performance() {
            var objectiveCards = Set.of(
                    objectiveCard("a"),
                    objectiveCard("b"),
                    objectiveCard("c"),
                    objectiveCard("d"),
                    objectiveCard("e"),
                    objectiveCard("f"),
                    objectiveCard("g"));

            when(playerState.numberOfHazards()).thenReturn(8);

            var start = Instant.now();
//            for (int i = 0; i < 1000; i++) {
            ObjectiveCard.score(Collections.emptySet(), objectiveCards, game, player, true);
//            }
            var end = Instant.now();
            System.out.println(Duration.between(start, end));
        }
    }

}