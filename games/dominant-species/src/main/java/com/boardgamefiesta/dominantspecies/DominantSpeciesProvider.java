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

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.dominantspecies.logic.*;
import com.boardgamefiesta.dominantspecies.view.DominantSpeciesView;
import lombok.NonNull;
import org.eclipse.yasson.FieldAccessStrategy;
import org.eclipse.yasson.JsonBindingProvider;
import org.eclipse.yasson.YassonConfig;
import org.eclipse.yasson.YassonJsonb;

import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.JsonbException;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class DominantSpeciesProvider implements GameProvider<DominantSpecies> {

    public static final String ID = "ds";

    private static final Set<PlayerColor> COLORS = Set.of(
            PlayerColor.BLUE,
            PlayerColor.WHITE,
            PlayerColor.BLACK,
            PlayerColor.GREEN,
            PlayerColor.RED,
            PlayerColor.YELLOW);

    private static final YassonJsonb JSONB = (YassonJsonb) new JsonBindingProvider().create()
            .withConfig(new YassonConfig()
                    .withFailOnUnknownProperties(false)
                    .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                    .withSerializers(
                            new ElementsMapSerDes(),
                            new TilesMapSerDes())
                    .withDeserializers(
                            new ElementsMapSerDes(),
                            new TilesMapSerDes())
                    .withAdapters(
                            new CornerJsonbAdapter(),
                            new HexJsonbAdapter(),
                            new ActionJsonbAdapter(),
                            new ActionsJsonbAdapter()))
            .build();

    private static final StateSerializer<DominantSpecies> SERIALIZER = new DominantSpeciesSerializer();
    private static final StateDeserializer<DominantSpecies> DESERIALIZER = jsonObject -> JSONB.fromJsonStructure(jsonObject, DominantSpecies.class);
    private static final ActionMapper<DominantSpecies> ACTION_MAPPER = new DominantSpeciesActionMapper();
    private static final ViewMapper<DominantSpecies> VIEW_MAPPER = new ViewMapper<>() {
        @Override
        public Object toView(@NonNull DominantSpecies state, Player viewer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isJsonGeneratorSupported() {
            return true;
        }

        @Override
        public void serialize(@NonNull DominantSpecies state, Player viewer, @NonNull JsonGenerator jsonGenerator) {
            JSONB.toJson(new DominantSpeciesView(state), jsonGenerator);
        }
    };

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 6;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return COLORS;
    }

    @Override
    public DominantSpecies start(Set<Player> players, Options options, InGameEventListener eventListener, Random random) {
        return DominantSpecies.start(players, random);
    }

    @Override
    public StateSerializer<DominantSpecies> getStateSerializer() {
        return SERIALIZER;
    }

    @Override
    public StateDeserializer<DominantSpecies> getStateDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public ActionMapper<DominantSpecies> getActionMapper() {
        return ACTION_MAPPER;
    }

    @Override
    public ViewMapper<DominantSpecies> getViewMapper() {
        return VIEW_MAPPER;
    }

    @Override
    public void executeAutoma(DominantSpecies state, Player player, Random random) {
        new Automa().perform(state, player, random);
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

    private static class DominantSpeciesSerializer implements StateSerializer<DominantSpecies> {
        @Override
        public JsonObject serialize(DominantSpecies state, JsonBuilderFactory factory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isJsonGeneratorSupported() {
            return true;
        }

        @Override
        public void serialize(DominantSpecies state, JsonGenerator jsonGenerator) {
            JSONB.toJson(state, jsonGenerator);
        }
    }

    private static class DominantSpeciesActionMapper implements ActionMapper<DominantSpecies> {

        @Override
        public Action toAction(JsonObject jsonObject, DominantSpecies state) {
            if (jsonObject.keySet().size() != 1) {
                throw new JsonException("Exactly 1 key was expected, but got: " + jsonObject.keySet());
            }

            var key = jsonObject.keySet().iterator().next();

            try {
                var value = jsonObject.get(key);
                if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new JsonException("Value of '" + key + "' must be of type " + JsonValue.ValueType.OBJECT + ", but was: " + value.getValueType());
                }
                return JSONB.fromJsonStructure(value.asJsonObject(), Action.forName(key));
            } catch (ClassNotFoundException e) {
                throw new JsonException("Unknown action: " + key);
            } catch (JsonbException e) {
                throw new JsonException(e.getMessage(), e);
            }
        }
    }

    /**
     * Because the JSON-B implementation cannot serialize or deserialize a {@link Map} using a {@link javax.json.bind.adapter.JsonbAdapter} for the key.
     */
    private static class ElementsMapSerDes implements JsonbSerializer<Map<Corner, ElementType>>, JsonbDeserializer<Map<Corner, ElementType>> {
        @Override
        public void serialize(Map<Corner, ElementType> elements, JsonGenerator generator, SerializationContext ctx) {
            generator.writeStartObject();
            elements.forEach((key, value) -> generator.write(key.toString(), value.name()));
            generator.writeEnd();
        }

        @Override
        public Map<Corner, ElementType> deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            var map = new HashMap<Corner, ElementType>();

            JsonParser.Event event;
            while (parser.hasNext() && (event = parser.next()) != JsonParser.Event.END_OBJECT) {
                if (event == JsonParser.Event.KEY_NAME) {
                    var key = Corner.valueOf(parser.getString());
                    map.put(key, ctx.deserialize(ElementType.class, parser));
                }
            }

            return map;
        }
    }

    /**
     * Because the JSON-B implementation cannot serialize or deserialize a {@link Map} using a {@link javax.json.bind.adapter.JsonbAdapter} for the key.
     */
    private static class TilesMapSerDes implements JsonbSerializer<Map<Hex, Tile>>, JsonbDeserializer<Map<Hex, Tile>> {
        @Override
        public void serialize(Map<Hex, Tile> map, JsonGenerator generator, SerializationContext ctx) {
            generator.writeStartObject();
            map.forEach((key, value) -> {
                ctx.serialize(key.toString(), value, generator);
            });
            generator.writeEnd();
        }

        @Override
        public Map<Hex, Tile> deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            var map = new HashMap<Hex, Tile>();

            JsonParser.Event event;
            while (parser.hasNext() && (event = parser.next()) != JsonParser.Event.END_OBJECT) {
                if (event == JsonParser.Event.KEY_NAME) {
                    var key = Hex.valueOf(parser.getString());
                    map.put(key, ctx.deserialize(Tile.class, parser));
                }
            }

            return map;
        }
    }

    private static final class HexJsonbAdapter implements JsonbAdapter<Hex, String> {
        @Override
        public String adaptToJson(Hex obj) {
            if (obj == null) return null;
            return obj.toString();
        }

        @Override
        public Hex adaptFromJson(String obj) {
            if (obj == null) return null;
            return Hex.valueOf(obj);
        }
    }

    private static final class CornerJsonbAdapter implements JsonbAdapter<Corner, String> {

        @Override
        public String adaptToJson(Corner obj) {
            if (obj == null) return null;
            return obj.toString();
        }

        @Override
        public Corner adaptFromJson(String obj) {
            if (obj == null) return null;
            return Corner.valueOf(obj);
        }
    }

    private static final class ActionJsonbAdapter implements JsonbAdapter<Class<? extends Action>, String> {

        @Override
        public String adaptToJson(Class<? extends Action> action) {
            return Action.getName(action);
        }

        @Override
        public Class<? extends Action> adaptFromJson(String name) {
            try {
                return Action.forName(name);
            } catch (ClassNotFoundException e) {
                throw new JsonbException("Unknown action: " + name);
            }
        }
    }

    private static final class ActionsJsonbAdapter implements JsonbAdapter<List<Class<? extends Action>>, List<String>> {

        @Override
        public List<String> adaptToJson(List<Class<? extends Action>> actions) {
            return actions.stream().map(Action::getName).collect(Collectors.toList());
        }

        @Override
        public List<Class<? extends Action>> adaptFromJson(List<String> value) {
            return value.stream().map(name -> {
                try {
                    return Action.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new JsonbException("Unknown action: " + name);
                }
            }).collect(Collectors.toList());
        }
    }
}
