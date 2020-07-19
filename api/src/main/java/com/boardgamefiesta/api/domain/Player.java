package com.boardgamefiesta.api.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

@Getter
@ToString
@AllArgsConstructor
public class Player {

    String name;
    PlayerColor color;

    public JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("name", name)
                .add("color", color.name())
                .build();
    }

    public static Player deserialize(JsonObject jsonObject) {
        return new Player(
                jsonObject.getString("name"),
                PlayerColor.valueOf(jsonObject.getString("color")));
    }

}
