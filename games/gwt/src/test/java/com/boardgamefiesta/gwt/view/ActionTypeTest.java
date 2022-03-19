/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionTypeTest {

    @Mock
    GWT game;

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
        when(objectiveCard1.getPenalty()).thenReturn(2);
        when(objectiveCard1.getPossibleActions()).thenReturn(Set.of(Action.DrawCard.class, Action.Draw2Cards.class, Action.Draw3Cards.class));
        when(objectiveCard1.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE));

        when(currentPlayerState.getHand()).thenReturn(Set.of(objectiveCard1));


        Action action = ActionType.toAction(Json.createObjectBuilder()
                .add("type", ActionType.PLAY_OBJECTIVE_CARD.name())
                .add("objectiveCard", Json.createObjectBuilder()
                        .add("points", 3)
                        .add("penalty", 2)
                        .add("action", ActionType.DRAW_CARD.name())
                        .add("tasks", Json.createArrayBuilder()
                                .add("BUILDING")
                                .add("BLUE_TEEPEE")
                                .add("BLUE_TEEPEE")))
                .build(), game);

        assertThat(action).isInstanceOf(Action.PlayObjectiveCard.class);
    }

    @Test
    void takeObjectiveCard() {
        ObjectiveCard objectiveCard1 = mock(ObjectiveCard.class);
        lenient().when(objectiveCard1.getPoints()).thenReturn(3);
        lenient().when(objectiveCard1.getPenalty()).thenReturn(2);
        lenient().when(objectiveCard1.getPossibleActions()).thenReturn(Set.of(Action.Gain2Dollars.class));
        lenient().when(objectiveCard1.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE));

        ObjectiveCard objectiveCard2 = mock(ObjectiveCard.class);
        lenient().when(objectiveCard2.getPoints()).thenReturn(3);
        lenient().when(objectiveCard2.getPenalty()).thenReturn(2);
        lenient().when(objectiveCard2.getPossibleActions()).thenReturn(Set.of(Action.Gain2Dollars.class));
        lenient().when(objectiveCard2.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.GREEN_TEEPEE));

        ObjectiveCard objectiveCard3 = mock(ObjectiveCard.class);
        lenient().when(objectiveCard3.getPoints()).thenReturn(3);
        lenient().when(objectiveCard3.getPenalty()).thenReturn(2);
        lenient().when(objectiveCard3.getPossibleActions()).thenReturn(Set.of(Action.Gain2Dollars.class));
        lenient().when(objectiveCard3.getTasks()).thenReturn(List.of(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.GREEN_TEEPEE));

        ObjectiveCards objectiveCards = mock(ObjectiveCards.class);
        when(game.getObjectiveCards()).thenReturn(objectiveCards);

        when(objectiveCards.getAvailable()).thenReturn(Set.of(objectiveCard3, objectiveCard2, objectiveCard1));

        var action = ActionType.toAction(Json.createObjectBuilder()
                .add("type", "TAKE_OBJECTIVE_CARD")
                .add("objectiveCard", Json.createObjectBuilder()
                        .add("points", 3)
                        .add("penalty", 2)
                        .add("action", "GAIN_2_DOLLARS")
                        .add("tasks", Json.createArrayBuilder()
                                .add("BUILDING")
                                .add("BLUE_TEEPEE")
                                .add("BLUE_TEEPEE")))
                .build(), game);

        assertThat(action).isInstanceOf(Action.TakeObjectiveCard.class);

        var takeObjectiveCard = (Action.TakeObjectiveCard) action;
        assertThat(takeObjectiveCard.getObjectiveCard().getTasks()).containsExactlyInAnyOrder(ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BUILDING);
        assertThat(takeObjectiveCard.getObjectiveCard().getPoints()).isEqualTo(3);
        assertThat(takeObjectiveCard.getObjectiveCard().getPenalty()).isEqualTo(2);
        assertThat(takeObjectiveCard.getObjectiveCard().getPossibleActions()).containsExactly(Action.Gain2Dollars.class);
    }
}
