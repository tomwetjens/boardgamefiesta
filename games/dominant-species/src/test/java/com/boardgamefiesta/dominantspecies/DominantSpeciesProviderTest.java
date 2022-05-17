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

package com.boardgamefiesta.dominantspecies;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.dominantspecies.logic.*;
import org.eclipse.yasson.internal.jsonstructure.JsonGeneratorToStructureAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominantSpeciesProviderTest {

    DominantSpeciesProvider provider = new DominantSpeciesProvider();

    Player playerA = new Player("Player A", PlayerColor.BLACK, Player.Type.HUMAN);
    Player playerB = new Player("Player B", PlayerColor.RED, Player.Type.HUMAN);

    @Nested
    class StateSerializer {

        @Test
        void serialize() {
            var ds = DominantSpecies.start(Set.of(playerA, playerB), new Random(0));

            var jsonGenerator = new JsonGeneratorToStructureAdapter(JsonProvider.provider());
            provider.getStateSerializer().serialize(ds, jsonGenerator);

            var serialized = (JsonObject) jsonGenerator.getRootStructure();

            var jsonString = serialized.toString();
            System.out.println(jsonString);

            assertThat(serialized).containsOnlyKeys(
                    "phase",
                    "initiativeTrack",
                    "elements",
                    "tiles",
                    "animals",
                    "currentAnimal",
                    "round",
                    "actionDisplay",
                    "drawBag",
                    "actionQueue",
                    "actionDisplay",
                    "canUndo",
                    "scoredTiles",
                    "deck",
                    "availableCards",
                    "availableTundraTiles",
                    "wanderlustTiles");

            // Must fit into a DynamoDB item with extra room for PK, indexed attributes
            assertThat(jsonString.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(3200);
        }
    }

    @Nested
    class StateDeserializer {
        @Test
        void deserialize() {
            var ds = DominantSpecies.start(Set.of(playerA, playerB), new Random(0));

            var jsonGenerator = new JsonGeneratorToStructureAdapter(JsonProvider.provider());
            provider.getStateSerializer().serialize(ds, jsonGenerator);

            var serialized = (JsonObject) jsonGenerator.getRootStructure();

            var deserialized = provider.getStateDeserializer().deserialize(serialized);

            assertThat(deserialized.getPhase()).isEqualTo(ds.getPhase());
            assertThat(deserialized.getInitiativeTrack()).isEqualTo(ds.getInitiativeTrack());
            assertThat(deserialized.getElements()).isEqualTo(ds.getElements());
            assertThat(deserialized.getTiles()).isEqualTo(ds.getTiles());
            assertThat(deserialized.getAnimals()).containsOnlyKeys(ds.getAnimals().keySet());
        }
    }

    @Nested
    class ActionMapper {

        DominantSpecies state;

        @BeforeEach
        void setUp() {
            state = DominantSpecies.start(Set.of(playerA, playerB), new Random(0));
        }

        @Test
        void emptyObject() {
            assertThatThrownBy(() -> provider.getActionMapper().toAction(JsonObject.EMPTY_JSON_OBJECT, state))
                    .isInstanceOf(JsonException.class)
                    .hasMessage("Exactly 1 key was expected, but got: []");
        }

        @Test
        void unknownAction() {
            var jsonObject = Json.createObjectBuilder()
                    .add("Foo", JsonObject.EMPTY_JSON_OBJECT)
                    .build();

            assertThatThrownBy(() -> provider.getActionMapper().toAction(jsonObject, state))
                    .isInstanceOf(JsonException.class)
                    .hasMessage("Unknown action: Foo");
        }

        @Test
        void notObject() {
            var jsonObject = Json.createObjectBuilder()
                    .add("Speciation", JsonValue.TRUE)
                    .build();

            assertThatThrownBy(() -> provider.getActionMapper().toAction(jsonObject, state))
                    .isInstanceOf(JsonException.class)
                    .hasMessage("Value of 'Speciation' must be of type OBJECT, but was: TRUE");
        }

        @Test
        void speciation() {
            var action = (Action.Speciation) provider.getActionMapper().toAction(Json.createObjectBuilder()
                    .add("Speciation", Json.createObjectBuilder()
                            .add("element", "((0,0),(0,1),(1,0))")
                            .add("species", Json.createArrayBuilder()
                                    .add(4)
                                    .add(2))
                            .add("tiles", Json.createArrayBuilder()
                                    .add("(0,0)")
                                    .add("(1,0)")))
                    .build(), state);

            assertThat(action.getSpecies()).containsExactly(4, 2);
            assertThat(action.getTiles()).containsExactly(new Hex(0, 0), new Hex(1, 0));
            assertThat(action.getElement()).isEqualTo(new Corner(new Hex(0, 0), new Hex(0, 1), new Hex(1, 0)));
        }

        @Test
        void abundance() {
            var action = (Action.Abundance) provider.getActionMapper().toAction(Json.createObjectBuilder()
                    .add("Abundance", Json.createObjectBuilder()
                            .add("elementType", ElementType.GRASS.name())
                            .add("corner", "((-1,0),(0,0),(0,-1))"))
                    .build(), state);

            assertThat(action.getElementType()).isEqualTo(ElementType.GRASS);
            assertThat(action.getCorner().getA()).isEqualTo(new Hex(-1, 0));
            assertThat(action.getCorner().getB()).isEqualTo(new Hex(0, 0));
            assertThat(action.getCorner().getC()).isEqualTo(new Hex(0, -1));
        }
    }
}