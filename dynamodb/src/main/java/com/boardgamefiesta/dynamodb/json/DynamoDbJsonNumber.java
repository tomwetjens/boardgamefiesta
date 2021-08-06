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

import lombok.AccessLevel;
import lombok.Getter;
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
