package com.tomsboardgames.gwt;

import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@ToString
public class Hazard {

    @NonNull
    private final HazardType type;

    @NonNull
    private final Hand hand;

    private final int points;

    static Hazard deserialize(JsonObject jsonObject) {
        return new Hazard(HazardType.valueOf(jsonObject.getString("type")),
                Hand.valueOf(jsonObject.getString("hand")),
                jsonObject.getInt("points"));
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("type", type.name())
                .add("hand", hand.name())
                .add("points", points)
                .build();
    }
}
