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

import lombok.RequiredArgsConstructor;
import lombok.Value;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Deque;
import java.util.LinkedList;

/**
 * {@link JsonGenerator} that builds a DynamoDB {@link AttributeValue} instead of writing JSON.
 */
@RequiredArgsConstructor
public class DynamoDbJsonGenerator implements JsonGenerator {

    @Value
    private static class Context {
        DynamoDbJsonStructureBuilder builder;
        String key;
    }

    private final Deque<Context> stack = new LinkedList<>();
    private DynamoDbJsonStructureBuilder root;
    private String key;

    @Override
    public JsonGenerator writeStartObject() {
        stack.push(new Context(new DynamoDbJsonObjectBuilder(), key));

        key = null;

        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        stack.push(new Context(new DynamoDbJsonArrayBuilder(), key));

        key = null;

        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        if (stack.isEmpty()) throw new JsonGenerationException("not in context");
        var current = stack.pop();

        if (!stack.isEmpty()) {
            var parent = stack.peek().builder;
            if (parent.isObject()) {
                parent.asObject().add(current.key, current.builder);
            } else {
                parent.asArray().add(current.builder);
            }
        } else {
            root = current.builder;
        }

        key = null;

        return this;
    }

    @Override
    public JsonGenerator writeKey(String key) {
        currentObject();
        this.key = key;
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String key) {
        return writeKey(key).writeStartObject();
    }

    @Override
    public JsonGenerator writeStartArray(String key) {
        return writeKey(key).writeStartArray();
    }

    @Override
    public JsonGenerator write(String key, JsonValue value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, String value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, BigInteger value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, BigDecimal value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, int value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, long value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, double value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator write(String key, boolean value) {
        currentObject().add(key, value);
        return this;
    }

    @Override
    public JsonGenerator writeNull(String key) {
        currentObject().addNull(key);
        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(String value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(BigDecimal value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(BigInteger value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(int value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(long value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(double value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(boolean value) {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().add(key, value);
            key = null;
        } else {
            builder.asArray().add(value);
        }

        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        var builder = current();

        if (builder.isObject()) {
            builder.asObject().addNull(key);
            key = null;
        } else {
            builder.asArray().addNull();
        }

        return this;
    }

    @Override
    public void close() {
        // No op
    }

    @Override
    public void flush() {
        // No op
    }

    public AttributeValue getAttributeValue() {
        return root.build().getAttributeValue();
    }

    private DynamoDbJsonObjectBuilder currentObject() {
        var current = stack.peek();
        if (current == null || !current.builder.isObject() || key != null)
            throw new JsonGenerationException("not in object context");
        return (DynamoDbJsonObjectBuilder) current.builder;
    }

    private DynamoDbJsonStructureBuilder current() {
        var builder = stack.peek();
        if (builder == null) throw new JsonGenerationException("not in context");
        return builder.builder;
    }

}
