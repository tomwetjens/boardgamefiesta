package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.api.Action;
import com.wetjens.gwt.server.domain.Table;
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

    Action toAction(Table table) {
        return table.getGame().toAction(jsonObject, table.getState().get());
    }

}
