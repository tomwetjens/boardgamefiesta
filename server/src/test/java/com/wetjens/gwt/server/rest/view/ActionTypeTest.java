package com.wetjens.gwt.server.rest.view;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.json.Json;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Game;
import com.wetjens.gwt.ObjectiveCard;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.server.rest.view.state.ActionType;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionTypeTest {

    @Mock
    Game game;

    @Mock
    PlayerState currentPlayerState;

    @ParameterizedTest
    @MethodSource("allActions")
    void of(Class<? extends Action> action) {
        assertThat(ActionType.of(action)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ActionType.class)
    void naming(ActionType actionType) {
        assertThat(actionType.getAction().getSimpleName().toUpperCase())
                .describedAs("Enum constant " + actionType + " should match naming of action " + actionType.getAction().getSimpleName())
                .isEqualTo(actionType.name().replaceAll("[_]", ""));
    }

    @Test
    void playObjectiveCard() {
        when(game.currentPlayerState()).thenReturn(currentPlayerState);

        ObjectiveCard objectiveCard1 = mock(ObjectiveCard.class);
        when(objectiveCard1.getPoints()).thenReturn(3);
        when(objectiveCard1.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE));

        when(currentPlayerState.getHand()).thenReturn(Set.of(objectiveCard1));

        ActionType.PLAY_OBJECTIVE_CARD.toAction(Json.createObjectBuilder()
                .add("objectiveCard", Json.createObjectBuilder()
                        .add("points", 3)
                        .add("tasks", Json.createArrayBuilder()
                                .add("BUILDING")
                                .add("BLUE_TEEPEE")
                                .add("BLUE_TEEPEE")))
                .build(), game);
    }

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }
}
