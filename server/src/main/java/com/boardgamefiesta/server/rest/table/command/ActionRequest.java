package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.Game;
import com.boardgamefiesta.api.domain.State;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;

@JsonbTypeDeserializer(ActionRequest.Deserializer.class)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ActionRequest {

    private final JsonObject jsonObject;

    public static final class Deserializer implements JsonbDeserializer<ActionRequest> {
        @Override
        public ActionRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
            return new ActionRequest(jsonParser.getObject());
        }
    }

    public <T extends State> Action toAction(Game<T> game, T state) {
        return game.getActionMapper().toAction(jsonObject, state);
    }

}
