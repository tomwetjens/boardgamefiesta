package com.tomsboardgames.server.repository.json;

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
        attributeValues.add(((DynamoDbJsonValue) value).getAttributeValue());
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
        attributeValues.add(AttributeValue.builder().nul(true).build());
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
