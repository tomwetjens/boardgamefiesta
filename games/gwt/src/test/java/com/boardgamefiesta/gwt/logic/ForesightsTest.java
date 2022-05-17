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

package com.boardgamefiesta.gwt.logic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ForesightsTest {

    @Mock
    KansasCitySupply kansasCitySupply;

    @Nested
    class Deserialize {

        @Test
        void emptyColumn() {
            var foresights = Foresights.deserialize(Json.createObjectBuilder()
                    .add("spaces", Json.createArrayBuilder()
                            .add(Json.createArrayBuilder())
                            .add(Json.createArrayBuilder())
                            .add(Json.createArrayBuilder()))
                    .build());

            assertThat(foresights.choices(0)).containsExactly(null, null);
            assertThat(foresights.choices(1)).containsExactly(null, null);
            assertThat(foresights.choices(2)).containsExactly(null, null);
        }

        @Test
        void nullInColumn() {
            var foresights = Foresights.deserialize(Json.createObjectBuilder()
                    .add("spaces", Json.createArrayBuilder()
                            .add(Json.createArrayBuilder()
                                    .addNull()
                                    .add(Json.createObjectBuilder()
                                            .add("worker", "COWBOY")))
                            .add(Json.createArrayBuilder()
                                    .addNull()
                                    .add(Json.createObjectBuilder()
                                            .add("teepee", "BLUE")))
                            .add(Json.createArrayBuilder()
                                    .addNull()
                                    .add(Json.createObjectBuilder()
                                            .add("hazard", Json.createObjectBuilder()
                                                    .add("type", "FLOOD")
                                                    .add("hand", "GREEN")
                                                    .add("points", 3)))))
                    .build());

            assertThat(foresights.choices(0).get(0)).isNull();
            assertThat(foresights.choices(0).get(1).getWorker()).isEqualTo(Worker.COWBOY);
            assertThat(foresights.choices(1).get(0)).isNull();
            assertThat(foresights.choices(1).get(1).getTeepee()).isEqualTo(Teepee.BLUE);
            assertThat(foresights.choices(2).get(0)).isNull();
            assertThat(foresights.choices(2).get(1).getHazard().getType()).isEqualTo(HazardType.FLOOD);
            assertThat(foresights.choices(2).get(1).getHazard().getPoints()).isEqualTo(3);
            assertThat(foresights.choices(2).get(1).getHazard().getHand()).isEqualTo(Hand.GREEN);
        }

        @Test
        void oneInColumn() {
            var foresights = Foresights.deserialize(Json.createObjectBuilder()
                    .add("spaces", Json.createArrayBuilder()
                            .add(Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("worker", "COWBOY")))
                            .add(Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("teepee", "BLUE")))
                            .add(Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("hazard", Json.createObjectBuilder()
                                                    .add("type", "FLOOD")
                                                    .add("hand", "GREEN")
                                                    .add("points", 3)))))
                    .build());

            assertThat(foresights.choices(0).get(0).getWorker()).isEqualTo(Worker.COWBOY);
            assertThat(foresights.choices(0).get(1)).isNull();
            assertThat(foresights.choices(1).get(0).getTeepee()).isEqualTo(Teepee.BLUE);
            assertThat(foresights.choices(1).get(1)).isNull();
            assertThat(foresights.choices(2).get(0).getHazard().getType()).isEqualTo(HazardType.FLOOD);
            assertThat(foresights.choices(2).get(0).getHazard().getPoints()).isEqualTo(3);
            assertThat(foresights.choices(2).get(0).getHazard().getHand()).isEqualTo(Hand.GREEN);
            assertThat(foresights.choices(2).get(1)).isNull();
        }

        @Test
        void twoInColumn() {
// TODO
        }
    }

    @Test
    void fillUpWhenSupplyIsEmpty() {
        var foresights = Foresights.empty();

        foresights.fillUp(kansasCitySupply, false);

        assertThat(foresights.isEmpty()).isTrue();
    }
}