package com.tomsboardgames.server.repository.json;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonNumber;
import java.math.BigDecimal;
import java.math.BigInteger;

class DynamoDbJsonNumber extends DynamoDbJsonValue implements JsonNumber {

    @Getter(value = AccessLevel.PACKAGE)
    private final AttributeValue attributeValue;

    @Getter(value = AccessLevel.PACKAGE)
    private final String value;

    DynamoDbJsonNumber(AttributeValue attributeValue) {
        this.attributeValue = attributeValue;
        this.value = attributeValue.n();
    }

    DynamoDbJsonNumber(String value) {
        this.attributeValue = null;
        this.value = value;
    }

    @Override
    public boolean isIntegral() {
        return value.contains(".");
    }

    @Override
    public int intValue() {
        return Integer.parseInt(value);
    }

    @Override
    public int intValueExact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long longValue() {
        return Long.parseLong(value);
    }

    @Override
    public long longValueExact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return new BigInteger(value);
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(value);
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(value);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NUMBER;
    }
}
