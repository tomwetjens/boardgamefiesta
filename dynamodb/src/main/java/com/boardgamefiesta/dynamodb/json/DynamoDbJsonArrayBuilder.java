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

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link JsonArrayBuilder} that builds into a {@link AttributeValue}.
 */
@Slf4j
class DynamoDbJsonArrayBuilder implements JsonArrayBuilder {

    private final List<AttributeValue> attributeValues = new ArrayList<>();

    @Override
    public JsonArrayBuilder add(JsonValue value) {
        attributeValues.add(DynamoDbJsonValue.getAttributeValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(String value) {
        attributeValues.add(value != null ? AttributeValue.builder().s(value).build() : null);
        return this;
    }

    @Override
    public JsonArrayBuilder add(BigDecimal value) {
        return addNumber(value != null ? value.toString() : null);
    }

    @Override
    public JsonArrayBuilder add(BigInteger value) {
        return addNumber(value != null ? value.toString() : null);
    }

    private DynamoDbJsonArrayBuilder addNumber(String n) {
        attributeValues.add(n != null ? AttributeValue.builder().n(n).build() : null);
        return this;
    }

    @Override
    public JsonArrayBuilder add(int value) {
        return addNumber(Integer.toString(value));
    }

    @Override
    public JsonArrayBuilder add(long value) {
        return addNumber(Long.toString(value));
    }

    @Override
    public JsonArrayBuilder add(double value) {
        return addNumber(Double.toString(value));
    }

    @Override
    public JsonArrayBuilder add(boolean value) {
        attributeValues.add(AttributeValue.builder().bool(value).build());
        return this;
    }

    @Override
    public JsonArrayBuilder addNull() {
        attributeValues.add(DynamoDbJsonValue.NUL);
        return this;
    }

    @Override
    public JsonArrayBuilder add(JsonObjectBuilder jsonObjectBuilder) {
        attributeValues.add(((DynamoDbJsonObject) jsonObjectBuilder.build()).getAttributeValue());
        return this;
    }

    @Override
    public JsonArrayBuilder add(JsonArrayBuilder jsonArrayBuilder) {
        attributeValues.add(((DynamoDbJsonArray) jsonArrayBuilder.build()).getAttributeValue());
        return this;
    }

    @Override
    public JsonArray build() {
        return new DynamoDbJsonArray(AttributeValue.builder().l(attributeValues).build());
    }
}
