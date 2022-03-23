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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static org.assertj.core.api.Assertions.assertThat;

class DynamoDbJsonNumberTest {

    @Test
    void testToString() {
        var dynamoDbJsonNumber = new DynamoDbJsonNumber(AttributeValue.builder().n("1337").build());
        assertThat(dynamoDbJsonNumber.toString()).isEqualTo("1337");
    }

    @Test
    void testEquals() {
        var a = new DynamoDbJsonNumber(AttributeValue.builder().n("1337").build());
        var b = new DynamoDbJsonNumber(AttributeValue.builder().n("1337").build());
        assertThat(a.equals(b)).isTrue();
    }

}