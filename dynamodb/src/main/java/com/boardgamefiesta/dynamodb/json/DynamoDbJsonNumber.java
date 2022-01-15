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
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonNumber;
import java.math.BigDecimal;
import java.math.BigInteger;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DynamoDbJsonNumber extends DynamoDbJsonValue implements JsonNumber {

    @Getter(value = AccessLevel.PACKAGE)
    private final AttributeValue attributeValue;

    @Override
    public boolean isIntegral() {
        return attributeValue.n().contains(".");
    }

    @Override
    public int intValue() {
        return Integer.parseInt(attributeValue.n());
    }

    @Override
    public int intValueExact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long longValue() {
        return Long.parseLong(attributeValue.n());
    }

    @Override
    public long longValueExact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return new BigInteger(attributeValue.n());
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(attributeValue.n());
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(attributeValue.n());
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NUMBER;
    }

    @Override
    public String toString() {
        return attributeValue.n();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return attributeValue.n().equals(((DynamoDbJsonNumber) o).attributeValue.n());
    }

    @Override
    public int hashCode() {
        return attributeValue.n().hashCode();
    }
}
