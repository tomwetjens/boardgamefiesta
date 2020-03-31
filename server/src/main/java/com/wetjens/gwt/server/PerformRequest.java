package com.wetjens.gwt.server;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Game;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

@JsonbTypeDeserializer(PerformRequest.Deserializer.class)
public abstract class PerformRequest {

    abstract Action toAction(Game game);

    public static class Deserializer implements JsonbDeserializer<PerformRequest> {
        @Override
        public PerformRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
            JsonObject jsonObject = jsonParser.getObject();
            ActionView actionType = ActionView.valueOf(jsonObject.getString("type"));

            switch (actionType) {
                case MOVE:
                    return new MoveActionRequest(jsonObject);
                default:
                    throw new IllegalArgumentException("Unsupported action: " + type);
            }
        }
    }

    static class MoveActionRequest extends PerformRequest{

        private final List<String> steps;

        MoveActionRequest(JsonObject jsonObject) {
            steps = getJsonStrings(jsonObject, "steps").stream().map(JsonString::getString).collect(Collectors.toList());
        }

        @Override
        Action toAction(Game game) {
            return new Action.Move(steps.stream().map(game.getTrail()::getLocation).collect(Collectors.toList()));
        }
    }

    private static List<JsonString> getJsonStrings(JsonObject jsonObject, String key) {
        JsonArray jsonArray = jsonObject.getJsonArray(key);
        if (jsonArray == null) {
            throw new JsonException("Property missing: " + key);
        }
        return jsonArray.getValuesAs(JsonString.class);
    }
}
