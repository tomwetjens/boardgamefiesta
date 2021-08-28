/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
        int value;

        @Override
        JsonObject serialize(JsonBuilderFactory factory) {
            return factory.createObjectBuilder()
                    .add("type", type.name())
                    .add("points", points)
                    .add("value", value)
                    .build();
        }

        static CattleCard deserialize(JsonObject jsonObject) {
            var cattleType = CattleType.valueOf(jsonObject.getString("type"));
            return new CattleCard(
                    cattleType,
                    jsonObject.getInt("points"),
                    jsonObject.getInt("value", cattleType.getDefaultValue()));
        }
    }
}
