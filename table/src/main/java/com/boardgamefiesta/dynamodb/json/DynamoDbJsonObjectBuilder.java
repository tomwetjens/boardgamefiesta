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

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.boardgamefiesta.dynamodb.json.DynamoDbJsonValue.NUL;

/**
 * {@link JsonObjectBuilder} that builds into a {@link AttributeValue}.
 */
@Slf4j
class DynamoDbJsonObjectBuilder implements JsonObjectBuilder, DynamoDbJsonStructureBuilder {

    private final Map<String, AttributeValue> attributeValues = new HashMap<>();

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public JsonObjectBuilder add(String key, JsonValue value) {
        attributeValues.put(key, DynamoDbJsonValue.getAttributeValue(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, String value) {
        attributeValues.put(key, value != null ? AttributeValue.builder().s(value).build() : NUL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, BigInteger value) {
        attributeValues.put(key, value != null ? AttributeValue.builder().n(value.toString()).build() : NUL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, BigDecimal value) {
        attributeValues.put(key, value != null ? AttributeValue.builder().n(value.toString()).build() : NUL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, int value) {
        attributeValues.put(key, AttributeValue.builder().n(Integer.toString(value)).build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, long value) {
        attributeValues.put(key, AttributeValue.builder().n(Long.toString(value)).build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, double value) {
        attributeValues.put(key, AttributeValue.builder().n(Double.toString(value)).build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, boolean value) {
        attributeValues.put(key, AttributeValue.builder().bool(value).build());
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(String key) {
        attributeValues.put(key, NUL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, JsonObjectBuilder jsonObjectBuilder) {
        return add(key, (DynamoDbJsonStructureBuilder) jsonObjectBuilder);
    }

    @Override
    public JsonObjectBuilder add(String key, JsonArrayBuilder jsonArrayBuilder) {
        return add(key, (DynamoDbJsonStructureBuilder) jsonArrayBuilder);
    }

    JsonObjectBuilder add(String key, DynamoDbJsonStructureBuilder jsonStructureBuilder) {
        attributeValues.put(key, jsonStructureBuilder != null ? jsonStructureBuilder.build().getAttributeValue() : NUL);
        return this;
    }

    @Override
    public DynamoDbJsonObject build() {
        return new DynamoDbJsonObject(AttributeValue.builder().m(attributeValues).build());
    }
}
