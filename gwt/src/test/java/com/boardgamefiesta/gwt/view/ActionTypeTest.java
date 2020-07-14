package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Action;
import com.boardgamefiesta.gwt.logic.Game;
import com.boardgamefiesta.gwt.logic.ObjectiveCard;
import com.boardgamefiesta.gwt.logic.PlayerState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }

    @Test
    void playObjectiveCard() {
        when(game.currentPlayerState()).thenReturn(currentPlayerState);

        ObjectiveCard objectiveCard1 = mock(ObjectiveCard.class);
        when(objectiveCard1.getPoints()).thenReturn(3);
        when(objectiveCard1.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE));

        when(currentPlayerState.getHand()).thenReturn(Set.of(objectiveCard1));


        Action action = ActionType.toAction(Json.createObjectBuilder()
                .add("type", ActionType.PLAY_OBJECTIVE_CARD.name())
                .add("objectiveCard", Json.createObjectBuilder()
                        .add("points", 3)
                        .add("tasks", Json.createArrayBuilder()
                                .add("BUILDING")
                                .add("BLUE_TEEPEE")
                                .add("BLUE_TEEPEE")))
                .build(), game);
    }
}
