package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Game;
import com.wetjens.gwt.server.rest.view.state.ActionType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;

@JsonbTypeDeserializer(ActionRequest.Deserializer.class)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionRequest {

    private final JsonObject jsonObject;

    public Action toAction(Game game) {
        var actionType = ActionType.valueOf(jsonObject.getString("type"));
        return actionType.toAction(jsonObject, game);
    }

    public static final class Deserializer implements JsonbDeserializer<ActionRequest> {
        @Override
        public ActionRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
            return new ActionRequest(jsonParser.getObject());
        }
    }

}
