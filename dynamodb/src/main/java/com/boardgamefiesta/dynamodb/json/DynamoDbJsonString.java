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
