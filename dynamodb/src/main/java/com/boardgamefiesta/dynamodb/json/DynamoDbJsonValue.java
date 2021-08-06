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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.Collections;

abstract class DynamoDbJsonValue implements JsonValue {

    static final AttributeValue EMPTY_LIST = AttributeValue.builder().l(Collections.emptyList()).build();
    static final AttributeValue EMPTY_MAP = AttributeValue.builder().m(Collections.emptyMap()).build();
    static final AttributeValue FALSE = AttributeValue.builder().bool(false).build();
    static final AttributeValue TRUE = AttributeValue.builder().bool(true).build();
    static final AttributeValue NUL = AttributeValue.builder().nul(true).build();

    public static JsonValue of(AttributeValue attributeValue) {
        if (attributeValue == null || Boolean.TRUE.equals(attributeValue.nul())) {
            return JsonValue.NULL;
        } else if (attributeValue.hasM()) {
            return new DynamoDbJsonObject(attributeValue);
        } else if (attributeValue.hasL()) {
            return new DynamoDbJsonArray(attributeValue);
        } else if (attributeValue.s() != null) {
            return new DynamoDbJsonString(attributeValue);
        } else if (attributeValue.n() != null) {
            return new DynamoDbJsonNumber(attributeValue);
        } else if (attributeValue.bool() != null) {
            return attributeValue.bool() ? JsonValue.TRUE : JsonValue.FALSE;
        } else {
            throw new IllegalArgumentException("Unsupported attribute value: " + attributeValue);
        }
    }

    static AttributeValue getAttributeValue(JsonValue value) {
        return (value == NULL || value == null) ? NUL
                : value == JsonValue.TRUE ? TRUE
                : value == JsonValue.FALSE ? FALSE
                : value == EMPTY_JSON_ARRAY ? EMPTY_LIST
                : value == EMPTY_JSON_OBJECT ? EMPTY_MAP
                : value.getValueType() == ValueType.STRING ? AttributeValue.builder().s(((JsonString) value).getString()).build()
                : value.getValueType() == ValueType.NUMBER ? AttributeValue.builder().n(((JsonNumber) value).bigDecimalValue().toString()).build()
                : ((DynamoDbJsonValue) value).getAttributeValue();
    }

    abstract AttributeValue getAttributeValue();

}
