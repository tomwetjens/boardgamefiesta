package com.boardgamefiesta.dynamodb.json;

import lombok.AccessLevel;
import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonString;

class DynamoDbJsonString extends DynamoDbJsonValue implements JsonString {

    @Getter(value = AccessLevel.PACKAGE)
    private final AttributeValue attributeValue;

    @Getter(value = AccessLevel.PACKAGE)
    private final String value;

    DynamoDbJsonString(AttributeValue attributeValue) {
        this.attributeValue = attributeValue;
        this.value = attributeValue.s();
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public CharSequence getChars() {
        return getString();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.STRING;
    }
}
