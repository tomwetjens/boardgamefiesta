package com.boardgamefiesta.api.repository;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Wraps {@link JsonBuilderFactory} adding convenience methods for serializing.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonSerializer {

    private final JsonBuilderFactory jsonBuilderFactory;

    public static JsonSerializer forFactory(@NonNull JsonBuilderFactory jsonBuilderFactory) {
        return new JsonSerializer(jsonBuilderFactory);
    }

    /**
     * Serializes a {@link Map} to a {@link JsonObject}.
     */
    public <K, V> JsonObject fromMap(Map<K, V> map, Function<K, String> keyMapper, Function<V, JsonValue> valueMapper) {
        var builder = jsonBuilderFactory.createObjectBuilder();
        map.forEach((key, value) -> builder.add(keyMapper.apply(key), valueMapper.apply(value)));
        return builder.build();
    }

    /**
     * Same as {@link #fromMap(Map, Function, Function)}, but also passes the {@link JsonBuilderFactory} to the value mapper.
     */
    public <K, V> JsonObject fromMap(Map<K, V> map, Function<K, String> keyMapper, BiFunction<V, JsonBuilderFactory, JsonValue> valueMapper) {
        var builder = jsonBuilderFactory.createObjectBuilder();
        map.forEach((key, value) -> builder.add(keyMapper.apply(key), valueMapper.apply(value, jsonBuilderFactory)));
        return builder.build();
    }
    
    public <K, V> JsonObject fromStringMap(Map<K, String> map, Function<K, String> keyMapper) {
        var builder = jsonBuilderFactory.createObjectBuilder();
        map.forEach((key, value) -> builder.add(keyMapper.apply(key), value));
        return builder.build();
    }

    public <K, V> JsonObject fromStringMap(Map<K, V> map, Function<K, String> keyMapper, Function<V, String> valueMapper) {
        var builder = jsonBuilderFactory.createObjectBuilder();
        map.forEach((key, value) -> builder.add(keyMapper.apply(key), valueMapper.apply(value)));
        return builder.build();
    }

    public <K> JsonObject fromIntegerMap(Map<K, Integer> map, Function<K, String> keyMapper) {
        var builder = jsonBuilderFactory.createObjectBuilder();
        map.forEach((key, value) -> builder.add(keyMapper.apply(key), value));
        return builder.build();
    }

    public <K, V> JsonObject fromIntegerMap(Map<K, V> map, Function<K, String> keyMapper, Function<V, Integer> valueMapper) {
        var builder = jsonBuilderFactory.createObjectBuilder();
        map.forEach((key, value) -> builder.add(keyMapper.apply(key), valueMapper.apply(value)));
        return builder.build();
    }

    public <T> JsonArray fromStream(Stream<T> values, Function<T, JsonValue> mapper) {
        var builder = jsonBuilderFactory.createArrayBuilder();
        values.map(mapper).forEach(builder::add);
        return builder.build();
    }

    public <T> JsonArray fromCollection(Collection<T> values, Function<T, JsonValue> mapper) {
        return fromStream(values.stream(), mapper);
    }

    public <T> JsonArray fromCollection(Collection<T> values, BiFunction<T, JsonBuilderFactory, JsonValue> mapper) {
        return fromStream(values.stream(), mapper);
    }

    public <T> JsonArray fromStream(Stream<T> values, BiFunction<T, JsonBuilderFactory, JsonValue> mapper) {
        var builder = jsonBuilderFactory.createArrayBuilder();
        values.map(value -> mapper.apply(value, jsonBuilderFactory)).forEach(builder::add);
        return builder.build();
    }

    public JsonArray fromStrings(Stream<String> values) {
        var builder = jsonBuilderFactory.createArrayBuilder();
        values.forEach(builder::add);
        return builder.build();
    }

    public JsonArray fromStrings(Collection<String> values) {
        return fromStrings(values.stream());
    }

    public <T> JsonArray fromStrings(Stream<T> values, Function<T, String> mapper) {
        return fromStrings(values.map(mapper));
    }

    public <T> JsonArray fromStrings(Collection<T> values, Function<T, String> mapper) {
        return fromStrings(values.stream(), mapper);
    }

    public JsonArray fromIntegers(Stream<Integer> values) {
        var builder = jsonBuilderFactory.createArrayBuilder();
        values.forEach(builder::add);
        return builder.build();
    }

    public <T> JsonArray fromIntegers(Stream<T> values, Function<T, Integer> mapper) {
        return fromIntegers(values.map(mapper));
    }

}
