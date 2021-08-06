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

package com.boardgamefiesta.dynamodb.json;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DynamoDbJson {

    public static AttributeValue toJson(Function<JsonBuilderFactory, JsonObject> serializer) {
        return ((DynamoDbJsonObject) serializer.apply(new DynamoDbJsonBuilderFactory())).getAttributeValue();
    }

    public static <T> T fromJson(AttributeValue attributeValue, Function<JsonObject, T> deserializer) {
        return deserializer.apply(new DynamoDbJsonObject(attributeValue));
    }

}
