package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        @Test
        void test() {
            var start = new ObjectiveCard(null, Arrays.asList(ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 3, 0);
            var a = new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.HAZARD), 3, 2);
            var b = new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 3);

            when(game.playerState(player)).thenReturn(playerState);
            when(game.getTrail()).thenReturn(trail);
            when(game.getRailroadTrack()).thenReturn(railroadTrack);
            when(game.getRailroadTrack().numberOfUpgradedStations(player)).thenReturn(5);
            when(playerState.getHazards()).thenReturn(Set.of(mock(Hazard.class), mock(Hazard.class), mock(Hazard.class)));
            when(playerState.getTeepees()).thenReturn(List.of(Teepee.BLUE));
            when(trail.numberOfBuildings(player)).thenReturn(3);

            assertThat(ObjectiveCard.score(Set.of(start, a, b), Collections.emptySet(), game, player).getTotal()).isEqualTo(8);
        }
    }

}