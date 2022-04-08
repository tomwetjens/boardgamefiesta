/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.dominantspecies.logic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionTest {

    @Mock(lenient = true)
    DominantSpecies game;

    @Mock(lenient = true)
    ActionDisplay actionDisplay;

    @Nested
    class RegressionTest {

        @Mock(lenient = true)
        Animal arachnids;

        @Mock(lenient = true)
        Animal insects;

        @Test
        void name() {
            when(game.isAnimalPlaying(AnimalType.ARACHNIDS)).thenReturn(true);
            when(game.isAnimalPlaying(AnimalType.INSECTS)).thenReturn(true);
            when(game.getAnimal(AnimalType.ARACHNIDS)).thenReturn(arachnids);
            when(game.getAnimal(AnimalType.INSECTS)).thenReturn(insects);
            when(game.getActionDisplay()).thenReturn(actionDisplay);

            when(arachnids.getType()).thenReturn(AnimalType.ARACHNIDS);
            when(insects.getType()).thenReturn(AnimalType.INSECTS);

            when(actionDisplay.getElements(ActionType.REGRESSION)).thenReturn(List.of(ElementType.SEED, ElementType.GRASS, ElementType.SUN));
            when(actionDisplay.getNumberOfActionPawns(ActionType.REGRESSION, AnimalType.INSECTS)).thenReturn(1);
            when(actionDisplay.getNumberOfActionPawns(ActionType.REGRESSION, AnimalType.ARACHNIDS)).thenReturn(1);
            when(arachnids.canRemoveOneOfElementTypes(anySet())).thenReturn(false);
            when(insects.canRemoveOneOfElementTypes(anySet())).thenReturn(false);

            var followUpActions = Action.Regression.activate(game);

            assertThat(followUpActions.isEmpty()).isTrue();
        }
    }

}