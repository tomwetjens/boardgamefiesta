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

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DynamoDbJsonGeneratorTest {

    @Test
    void arrayRoot() {
        var generator = new DynamoDbJsonGenerator();

        generator.writeStartArray();
        basicArray(generator, 2);
        generator.writeEnd();

        var attributeValue = generator.getAttributeValue();
        assertThat(attributeValue.hasL()).isTrue();

        assertThat(attributeValue.l()).hasSize(11);
        assertThat(attributeValue.l().get(0).l()).hasSize(11);
        assertThat(attributeValue.l().get(9).m()).hasSize(19);
    }

    @Test
    void objectRoot() {
        var generator = new DynamoDbJsonGenerator();

        generator.writeStartObject();
        basicObject(generator, 2);
        generator.writeEnd();

        var attributeValue = generator.getAttributeValue();
        assertThat(attributeValue.hasM()).isTrue();

        assertThat(attributeValue.m()).hasSize(19);
        assertThat(attributeValue.m()).containsKeys("array", "object");
        assertThat(attributeValue.m().get("object").m()).containsKeys("array", "object");
        assertThat(attributeValue.m().get("array").l()).hasSize(11);
    }

    static void basicObject(JsonGenerator jsonGenerator, int depth) {
        jsonGenerator.writeKey("int");
        jsonGenerator.write(1);
        jsonGenerator.writeKey("boolean");
        jsonGenerator.write(true);
        jsonGenerator.writeKey("JsonValue");
        jsonGenerator.write(JsonValue.TRUE);
        jsonGenerator.writeKey("long");
        jsonGenerator.write(1L);
        jsonGenerator.writeKey("double");
        jsonGenerator.write(1.0);
        jsonGenerator.writeKey("String");
        jsonGenerator.write("bar");
        jsonGenerator.writeKey("BigDecimal");
        jsonGenerator.write(BigDecimal.ONE);
        jsonGenerator.writeKey("BigInteger");
        jsonGenerator.write(BigInteger.ONE);
        jsonGenerator.writeKey("array");
        jsonGenerator.writeStartArray();
        if (depth > 0) basicArray(jsonGenerator, depth - 1);
        jsonGenerator.writeEnd();
        jsonGenerator.writeKey("object");
        jsonGenerator.writeStartObject();
        if (depth > 0) basicObject(jsonGenerator, depth - 1);
        jsonGenerator.writeEnd();
        jsonGenerator.writeKey("null");
        jsonGenerator.writeNull();

        jsonGenerator.write("int2", 1);
        jsonGenerator.write("boolean", true);
        jsonGenerator.write("JsonValue2", JsonValue.TRUE);
        jsonGenerator.write("long2", 1L);
        jsonGenerator.write("double", 1.0);
        jsonGenerator.write("String", "bar");
        jsonGenerator.write("BigDecimal2", BigDecimal.ONE);
        jsonGenerator.write("BigInteger2", BigInteger.ONE);
        jsonGenerator.writeStartArray("array2");
        if (depth > 0) basicArray(jsonGenerator, depth - 1);
        jsonGenerator.writeEnd();
        jsonGenerator.writeStartObject("object2");
        if (depth > 0) basicObject(jsonGenerator, depth - 1);
        jsonGenerator.writeEnd();
        jsonGenerator.writeNull("null2");
    }

    static void basicArray(JsonGenerator jsonGenerator, int depth) {
        jsonGenerator.writeStartArray();
        if (depth > 0) basicArray(jsonGenerator, depth - 1);
        jsonGenerator.writeEnd();

        jsonGenerator.write(1);
        jsonGenerator.write(true);
        jsonGenerator.write(JsonValue.TRUE);
        jsonGenerator.write(1L);
        jsonGenerator.write(1.0);
        jsonGenerator.write("bar");
        jsonGenerator.write(BigDecimal.ONE);
        jsonGenerator.write(BigInteger.ONE);

        jsonGenerator.writeStartObject();
        if (depth > 0) basicObject(jsonGenerator, depth - 1);
        jsonGenerator.writeEnd();

        jsonGenerator.writeNull();
    }

}