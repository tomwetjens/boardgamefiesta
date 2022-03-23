/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

abstract class DynamoDbJsonStructure extends DynamoDbJsonValue {

    public static DynamoDbJsonStructure of(AttributeValue attributeValue) {
        if (attributeValue.hasM()) {
            return new DynamoDbJsonObject(attributeValue);
        } else if (attributeValue.hasL()) {
            return new DynamoDbJsonArray(attributeValue);
        } else {
            throw new IllegalArgumentException("Attribute value is not a JSON structure: " + attributeValue);
        }
    }

}
