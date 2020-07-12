package com.boardgamefiesta.server.repository.json;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link JsonObjectBuilder} that builds into a {@link AttributeValue}.
 */
@Slf4j
class DynamoDbJsonObjectBuilder implements JsonObjectBuilder {

    private final Map<String, AttributeValue> attributeValues = new HashMap<>();

    @Override
    public JsonObjectBuilder add(String key, JsonValue value) {
        attributeValues.put(key, value != null ? ((DynamoDbJsonValue) value).getAttributeValue() : null);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, String value) {
        attributeValues.put(key, value != null ? AttributeValue.builder().s(value).build() : null);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, BigInteger value) {
        attributeValues.put(key, value != null ? AttributeValue.builder().n(value.toString()).build() : null);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, BigDecimal value) {
        attributeValues.put(key, value != null ? AttributeValue.builder().n(value.toString()).build() : null);
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
        attributeValues.put(key, AttributeValue.builder().nul(true).build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, JsonObjectBuilder jsonObjectBuilder) {
        attributeValues.put(key, jsonObjectBuilder != null ? ((DynamoDbJsonObject) jsonObjectBuilder.build()).getAttributeValue() : null);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String key, JsonArrayBuilder jsonArrayBuilder) {
        attributeValues.put(key, jsonArrayBuilder != null ? ((DynamoDbJsonArray) jsonArrayBuilder.build()).getAttributeValue() : null);
        return this;
    }

    @Override
    public JsonObject build() {
        return new DynamoDbJsonObject(AttributeValue.builder().m(attributeValues).build());
    }
}
