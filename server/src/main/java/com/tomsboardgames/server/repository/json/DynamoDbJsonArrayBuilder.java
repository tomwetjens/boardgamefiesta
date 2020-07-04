package com.tomsboardgames.server.repository.json;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * {@link JsonArrayBuilder} that builds into a {@link AttributeValue}.
 */
@Slf4j
class DynamoDbJsonArrayBuilder implements JsonArrayBuilder {

    private final List<AttributeValue> attributeValues = new ArrayList<>();
    private final Set<String> strings = new LinkedHashSet<>();
    private final Set<String> numbers = new LinkedHashSet<>();

    @Override
    public JsonArrayBuilder add(JsonValue value) {
        attributeValues.add(((DynamoDbJsonValue) value).getAttributeValue());
        return this;
    }

    @Override
    public JsonArrayBuilder add(String value) {
        if (attributeValues.isEmpty() && !strings.contains(value)) {
            strings.add(value);
        } else {
            attributeValues.add(value != null ? AttributeValue.builder().s(value).build() : null);
        }
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
        if (attributeValues.isEmpty() && !numbers.contains(n)) {
            numbers.add(n);
        } else {
            attributeValues.add(n != null ? AttributeValue.builder().n(n).build() : null);
        }
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
        // Optimize into String Set or Number Set if possible
        if (attributeValues.isEmpty()) {
            if (numbers.isEmpty() && !strings.isEmpty()) {
                return new DynamoDbJsonArray(AttributeValue.builder().ss(strings).build());
            } else if (strings.isEmpty() && !numbers.isEmpty()) {
                return new DynamoDbJsonArray(AttributeValue.builder().ns(numbers).build());
            }
        }

        // Else add everything into a normal List
        if (!strings.isEmpty()) {
            strings.forEach(value -> attributeValues.add(AttributeValue.builder().s(value).build()));
        }
        if (!numbers.isEmpty()) {
            numbers.forEach(value -> attributeValues.add(AttributeValue.builder().n(value).build()));
        }

        return new DynamoDbJsonArray(AttributeValue.builder().l(attributeValues).build());
    }
}
