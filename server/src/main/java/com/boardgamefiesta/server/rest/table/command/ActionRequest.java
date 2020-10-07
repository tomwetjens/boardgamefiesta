package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.server.domain.APIError;
import com.boardgamefiesta.server.domain.APIException;
import com.boardgamefiesta.server.domain.game.Game;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.json.JsonException;
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

    public <T extends State> Action toAction(Game game, T state) {
        try {
            return game.getProvider().getActionMapper().toAction(jsonObject, state);
        } catch (JsonException e) {
            throw APIException.badRequest(APIError.INVALID_ACTION);
        }
    }

}
