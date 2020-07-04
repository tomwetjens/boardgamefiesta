package com.tomsboardgames.server.repository.json;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonValue;

abstract class DynamoDbJsonValue implements JsonValue {

    public static JsonValue of(AttributeValue attributeValue) {
        if (attributeValue.hasM()) {
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
            return JsonValue.NULL;
        }
    }

    abstract AttributeValue getAttributeValue();

}
