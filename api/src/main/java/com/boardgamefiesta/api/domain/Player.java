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

package com.boardgamefiesta.api.domain;

import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization frameworks
@Setter // For deserialization frameworks
@EqualsAndHashCode(of = "name")
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
