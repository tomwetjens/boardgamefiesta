package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.gwt.logic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateViewTest {

    @Mock
    Game game;

    @Mock
    RailroadTrack railroadTrack;

    @Mock
    Trail trail;

    @Mock
    Foresights foresights;

    @Mock
    ObjectiveCards objectiveCards;

    @Mock
    CattleMarket cattleMarket;

    @Mock
    JobMarket jobMarket;

    Player playerA = new Player("Player A", PlayerColor.WHITE);
    Player playerB = new Player("Player B", PlayerColor.RED);

    @BeforeEach
    void setUp() {
        lenient().when(game.getRailroadTrack()).thenReturn(railroadTrack);
        lenient().when(game.getTrail()).thenReturn(trail);
        lenient().when(game.getForesights()).thenReturn(foresights);
        lenient().when(game.getObjectiveCards()).thenReturn(objectiveCards);
        lenient().when(game.getCattleMarket()).thenReturn(cattleMarket);
        lenient().when(game.getJobMarket()).thenReturn(jobMarket);
        lenient().when(game.getCurrentPlayer()).thenReturn(playerA);

        lenient().when(trail.getStart()).thenReturn(mock(Location.Start.class));
        lenient().when(trail.getKansasCity()).thenReturn(mock(Location.KansasCity.class));
    }

    @Nested
    class PossibleSpaces {

        @Test
        void allAuxActionsAvailable() {
            when(game.possibleActions()).thenReturn(Set.of(
                    Action.Gain1Dollar.class,
                    Action.Gain2Dollars.class,
                    Action.DrawCard.class,
                    Action.Draw2Cards.class,
                    Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class,
                    Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class,
                    Action.Pay1DollarToMoveEngine1Forward.class,
                    Action.Pay2DollarsToMoveEngine2Forward.class,
                    Action.MoveEngine1BackwardsToRemove1Card.class,
                    Action.MoveEngine2BackwardsToRemove2Cards.class
            ));

            var space1 = mock(RailroadTrack.Space.class);
            when(space1.getName()).thenReturn("1");
            var space2 = mock(RailroadTrack.Space.class);
            when(space2.getName()).thenReturn("2");

            when(railroadTrack.reachableSpacesForward(any(), eq(1), eq(1))).thenReturn(Set.of(space1));
            when(railroadTrack.reachableSpacesForward(any(), eq(1), eq(2))).thenReturn(Set.of(space1, space2));
            when(railroadTrack.reachableSpacesBackwards(any(), eq(1), eq(1))).thenReturn(Set.of(space1));
            when(railroadTrack.reachableSpacesBackwards(any(), eq(2), eq(2))).thenReturn(Set.of(space1, space2));

            var view = new StateView(game, playerA);

            assertThat(view.getPossibleSpaces().get(ActionType.PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE)).hasSize(1);
            assertThat(view.getPossibleSpaces().get(ActionType.PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES)).hasSize(2);
            assertThat(view.getPossibleSpaces().get(ActionType.PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD)).hasSize(1);
            assertThat(view.getPossibleSpaces().get(ActionType.PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD)).hasSize(2);
            assertThat(view.getPossibleSpaces().get(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD)).hasSize(1);
            assertThat(view.getPossibleSpaces().get(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS)).hasSize(2);
        }
    }

}