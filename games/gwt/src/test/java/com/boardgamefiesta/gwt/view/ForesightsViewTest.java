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

import com.boardgamefiesta.gwt.logic.Foresights;
import com.boardgamefiesta.gwt.logic.KansasCitySupply;
import com.boardgamefiesta.gwt.logic.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForesightsViewTest {

    @Mock
    Foresights foresights;

    @Test
    void oneNullInColumn() {
        when(foresights.choices(0)).thenReturn(Arrays.asList(null, new KansasCitySupply.Tile(Worker.COWBOY)));
        when(foresights.choices(1)).thenReturn(Arrays.asList(null, new KansasCitySupply.Tile(Worker.COWBOY)));
        when(foresights.choices(2)).thenReturn(Arrays.asList(null, new KansasCitySupply.Tile(Worker.COWBOY)));

        var view = new ForesightsView(foresights);

        assertThat(view.getChoices().get(0).get(0)).isNull();
        assertThat(view.getChoices().get(0).get(1).getWorker()).isEqualTo(Worker.COWBOY);
    }
}