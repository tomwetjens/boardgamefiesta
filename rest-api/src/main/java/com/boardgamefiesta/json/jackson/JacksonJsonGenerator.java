package com.boardgamefiesta.json.jackson;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Deque;
import java.util.LinkedList;

@RequiredArgsConstructor
public class JacksonJsonGenerator implements JsonGenerator {
    private final com.fasterxml.jackson.core.JsonGenerator jacksonJsonGenerator;

    private Deque<JsonValue.ValueType> currentContext = new LinkedList<>();

    @Override
    @SneakyThrows
    public JsonGenerator writeStartObject() {
        jacksonJsonGenerator.writeStartObject();
        currentContext.push(JsonValue.ValueType.OBJECT);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeStartObject(String name) {
        jacksonJsonGenerator.writeObjectFieldStart(name);
        currentContext.push(JsonValue.ValueType.OBJECT);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeKey(String name) {
        jacksonJsonGenerator.writeFieldName(name);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeStartArray() {
        jacksonJsonGenerator.writeStartArray();
        currentContext.push(JsonValue.ValueType.ARRAY);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeStartArray(String name) {
        jacksonJsonGenerator.writeArrayFieldStart(name);
        currentContext.push(JsonValue.ValueType.ARRAY);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, JsonValue jsonValue) {
        jacksonJsonGenerator.writeFieldName(name);
        return write(jsonValue);
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, String value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeString(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, BigInteger value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, BigDecimal value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, int value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, long value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, double value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String name, boolean value) {
        jacksonJsonGenerator.writeFieldName(name);
        jacksonJsonGenerator.writeBoolean(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeNull(String name) {
        jacksonJsonGenerator.writeNullField(name);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeEnd() {
        var contextType = currentContext.pop();
        switch (contextType) {
            case OBJECT:
                jacksonJsonGenerator.writeEndObject();
                break;
            case ARRAY:
                jacksonJsonGenerator.writeEndArray();
                break;
            default:
                throw new JsonGenerationException("Unsupported context: " + contextType);
        }
        return this;
    }

    @Override
    public JsonGenerator write(JsonValue jsonValue) {
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(String value) {
        jacksonJsonGenerator.writeString(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(BigDecimal value) {
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(BigInteger value) {
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(int value) {
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(long value) {
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(double value) {
        jacksonJsonGenerator.writeNumber(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator write(boolean value) {
        jacksonJsonGenerator.writeBoolean(value);
        return this;
    }

    @Override
    @SneakyThrows
    public JsonGenerator writeNull() {
        jacksonJsonGenerator.writeNull();
        return this;
    }

    @Override
    @SneakyThrows
    public void close() {
        jacksonJsonGenerator.close();
    }

    @Override
    @SneakyThrows
    public void flush() {
        jacksonJsonGenerator.flush();
    }
}
