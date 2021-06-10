package com.boardgamefiesta.dynamodb;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor(staticName = "of")
public class Item {

    private static final AttributeValue NUL = AttributeValue.builder().nul(true).build();

    private final Map<String, AttributeValue> map;

    public Item() {
        this(new HashMap<>());
    }

    public static AttributeValue n(int value) {
        return AttributeValue.builder().n(Integer.toString(value)).build();
    }

    public static <E extends Enum<E>> AttributeValue s(E value) {
        return value != null ? s(value.name()) : NUL;
    }

    public static AttributeValue s(Instant value) {
        return value != null ? s(value.toString()) : NUL;
    }

    public static AttributeValue s(String value) {
        return value != null ? AttributeValue.builder().s(value).build() : NUL;
    }

    public static AttributeValue bool(Boolean value) {
        return value != null ? AttributeValue.builder().bool(value).build() : NUL;
    }

    public static AttributeValue map(Map<String, AttributeValue> value) {
        return value != null ? AttributeValue.builder().m(value).build() : NUL;
    }

    public static AttributeValue ttl(Instant value) {
        return value != null ? n(value.getEpochSecond()) : NUL;
    }

    public static AttributeValue n(long value) {
        return AttributeValue.builder().n(Long.toString(value)).build();
    }

    public static AttributeValue l(List<AttributeValue> values) {
        return values != null ? AttributeValue.builder().l(values).build() : NUL;
    }

    private static AttributeValue ss(Collection<String> value) {
        return AttributeValue.builder().ss(value).build();
    }

    public Map<String, AttributeValue> asMap() {
        return map;
    }

    public AttributeValue asAttributeValue() {
        return map(map);
    }

    public String getString(String attributeName) {
        var attributeValue = map.get(attributeName);
        return attributeValue != null && !Boolean.TRUE.equals(attributeValue.nul())
                ? attributeValue.s() : null;
    }

    public boolean getBoolean(String attributeName) {
        var attributeValue = map.get(attributeName);
        return attributeValue != null && attributeValue.bool();
    }

    public Instant getInstant(String attributeName) {
        var attributeValue = map.get(attributeName);
        return attributeValue != null && !Boolean.TRUE.equals(attributeValue.nul())
                ? Instant.parse(attributeValue.s()) : null;
    }

    public Optional<Instant> getOptionalInstant(String attributeName) {
        return getOptionalString(attributeName)
                .map(Instant::parse);
    }

    public Optional<String> getOptionalString(String attributeName) {
        return getOptionalNotNull(attributeName)
                .map(AttributeValue::s);
    }

    public <E extends Enum<E>> E getEnum(String attributeName, Class<E> enumClass) {
        var attributeValue = map.get(attributeName);
        return attributeValue != null && !Boolean.TRUE.equals(attributeValue.nul())
                ? Enum.valueOf(enumClass, attributeValue.s()) : null;
    }

    public <E extends Enum<E>> Optional<E> getOptionalEnum(String attributeName, Class<E> enumClass) {
        return getOptionalNotNull(attributeName)
                .map(AttributeValue::s)
                .map(str -> Enum.valueOf(enumClass, str));
    }

    private Optional<AttributeValue> getOptionalNotNull(String attributeName) {
        return Optional.ofNullable(map.get(attributeName))
                .filter(attributeValue -> !Boolean.TRUE.equals(attributeValue.nul()));
    }

    public Optional<Integer> getOptionalInt(String attributeName) {
        return getOptionalNotNull(attributeName)
                .map(AttributeValue::n)
                .map(Integer::valueOf);
    }

    public Optional<Boolean> getOptionalBoolean(String attributeName) {
        return getOptionalNotNull(attributeName)
                .map(AttributeValue::bool);
    }

    public int getInt(String attributeName) {
        return getOptionalInt(attributeName).orElseThrow(() -> new NoSuchAttributeException(attributeName));
    }

    public Instant getTTL(String attributeName) {
        return getOptionalTTL(attributeName).orElseThrow(() -> new NoSuchAttributeException(attributeName));
    }

    private Optional<Instant> getOptionalTTL(String attributeName) {
        return getOptionalNotNull(attributeName)
                .map(AttributeValue::n)
                .map(Long::parseLong)
                .map(Instant::ofEpochSecond);
    }

    public <E extends Enum<E>> Item setEnum(String attributeName, E value) {
        map.put(attributeName, s(value));
        return this;
    }

    public Item setInstant(String attributeName, Instant value) {
        map.put(attributeName, s(value));
        return this;
    }

    public Item setInt(String attributeName, int value) {
        map.put(attributeName, n(value));
        return this;
    }

    public Item setTTL(String attributeName, Instant value) {
        map.put(attributeName, ttl(value));
        return this;
    }

    public Item setString(String attributeName, String value) {
        map.put(attributeName, s(value));
        return this;
    }

    public Item set(String attributeName, AttributeValue attributeValue) {
        map.put(attributeName, attributeValue);
        return this;
    }

    public AttributeValue get(String attributeName) {
        return map.get(attributeName);
    }

    public List<String> getStrings(String attributeName) {
        return map.get(attributeName).ss();
    }

    public Optional<List<String>> getOptionalStrings(String attributeName) {
        return getOptionalNotNull(attributeName)
                .map(AttributeValue::ss);
    }

    public Item setStrings(String attributeName, Collection<String> value) {
        map.put(attributeName, ss(value));
        return this;
    }

    public Map<String, AttributeValue> getMap(String attributeName) {
        return map.get(attributeName).m();
    }

    public Optional<Map<String, AttributeValue>> getOptionalMap(String attributeName) {
        return getOptionalNotNull(attributeName).map(AttributeValue::m);
    }

    public Item setBoolean(String attributeName, boolean value) {
        map.put(attributeName, bool(value));
        return this;
    }

    public static final class NoSuchAttributeException extends RuntimeException {
        private NoSuchAttributeException(String attributeName) {
            super("Attribute '" + attributeName + "' not present or empty");
        }
    }
}
