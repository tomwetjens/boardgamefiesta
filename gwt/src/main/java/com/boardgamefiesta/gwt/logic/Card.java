package com.boardgamefiesta.gwt.logic;

import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Card {

    static Card deserialize(JsonValue jsonValue) {
        if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT
                && jsonValue.asJsonObject().containsKey("type")) {
            return CattleCard.deserialize(jsonValue.asJsonObject());
        } else {
            return ObjectiveCard.deserialize(jsonValue);
        }
    }

    abstract JsonValue serialize(JsonBuilderFactory factory);

    // Not a @Value because each instance is unique
    @AllArgsConstructor
    @Getter
    @ToString
    public static final class CattleCard extends Card {

        CattleType type;
        int points;

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("type", type.name())
                    .add("points", points)
                    .build();
        }

        static CattleCard deserialize(JsonObject jsonObject) {
            return new CattleCard(
                    CattleType.valueOf(jsonObject.getString("type")),
                    jsonObject.getInt("points"));
        }
    }
}
