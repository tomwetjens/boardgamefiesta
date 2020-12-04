package com.boardgamefiesta.server.repository.json;

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
