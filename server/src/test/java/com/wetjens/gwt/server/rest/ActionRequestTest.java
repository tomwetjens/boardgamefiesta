package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Game;
import com.wetjens.gwt.ObjectiveCard;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.server.domain.ActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionRequestTest {

    @Mock
    Game game;

    @Mock
    PlayerState currentPlayerState;

    @Test
    void playObjectiveCard() {
        when(game.currentPlayerState()).thenReturn(currentPlayerState);

        ObjectiveCard objectiveCard1 = mock(ObjectiveCard.class);
        when(objectiveCard1.getPoints()).thenReturn(3);
        when(objectiveCard1.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE));

        when(currentPlayerState.getHand()).thenReturn(Set.of(objectiveCard1));

        Action action = new ActionRequest(ActionType.PLAY_OBJECTIVE_CARD, Json.createObjectBuilder()
                .add("objectiveCard", Json.createObjectBuilder()
                        .add("points", 3)
                        .add("tasks", Json.createArrayBuilder()
                                .add("BUILDING")
                                .add("BLUE_TEEPEE")
                                .add("BLUE_TEEPEE")))
                .build()).toAction(game);
    }

}
