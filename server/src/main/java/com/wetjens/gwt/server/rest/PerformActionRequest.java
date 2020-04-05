package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Game;
import com.wetjens.gwt.server.rest.view.ActionView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

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

@JsonbTypeDeserializer(PerformActionRequest.Deserializer.class)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PerformActionRequest {

    private final ActionView type;
    private final JsonObject jsonObject;

    public Action toAction(Game game) {
        return type.deserialize(jsonObject, game);
    }

    public static class Deserializer implements JsonbDeserializer<PerformActionRequest> {
        @Override
        public PerformActionRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
            JsonObject jsonObject = jsonParser.getObject();
            ActionView type = ActionView.valueOf(jsonObject.getString("type"));
            return new PerformActionRequest(type, jsonObject);
        }
    }

}
