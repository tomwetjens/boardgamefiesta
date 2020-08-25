package com.boardgamefiesta.gwt.logic;

import lombok.Value;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;

@Value
public class Bid {

    int position;
    int points;

    static Bid deserialize(JsonValue jsonValue) {
        if (jsonValue == null || jsonValue.getValueType() != JsonValue.ValueType.OBJECT) {
            return null;
        }

        var jsonObject = jsonValue.asJsonObject();

        return new Bid(jsonObject.getInt("position"), jsonObject.getInt("points"));
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("position", position)
                .add("points", points)
                .build();
    }
}
