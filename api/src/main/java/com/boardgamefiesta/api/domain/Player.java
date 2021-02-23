package com.boardgamefiesta.api.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;

@Getter
@ToString
@AllArgsConstructor
public class Player {

    String name;
    PlayerColor color;
    Type type;

    public JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("name", name)
                .add("color", color.name())
                .add("type", type != null ? type.name() : null)
                .build();
    }

    public static Player deserialize(JsonObject jsonObject) {
        return new Player(
                jsonObject.getString("name"),
                PlayerColor.valueOf(jsonObject.getString("color")),
                jsonObject.containsKey("type") && jsonObject.get("type") != JsonValue.NULL ? Type.valueOf(jsonObject.getString("type")) : null);
    }

    public enum Type {
        HUMAN,
        COMPUTER
    }

}
