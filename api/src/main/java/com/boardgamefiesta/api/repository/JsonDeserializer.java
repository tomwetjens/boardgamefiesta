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

package com.boardgamefiesta.api.repository;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wraps {@link JsonObject} adding convenience methods for deserializing.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonDeserializer {

    private final JsonObject jsonObject;

    public static JsonDeserializer forObject(@NonNull JsonObject jsonObject) {
        return new JsonDeserializer(jsonObject);
    }

    public static Optional<Integer> getInt(String key, JsonObject jsonObject) {
        return jsonObject.containsKey(key) ? Optional.of(jsonObject.getInt(key)) : Optional.empty();
    }

    /**
     * Deserializes the wrapped {@link JsonObject} to a {@link Map}.
     */
    public <K, V> Map<K, V> asMap(Function<String, K> keyMapper, Function<JsonValue, V> valueMapper) {
        return jsonObject.keySet().stream().collect(Collectors.toMap(keyMapper, k -> valueMapper.apply(jsonObject.get(k))));
    }

    /**
     * Same as {@link #asMap(Function, Function)}, but also passes the key to the value mapper.
     */
    public <K, V> Map<K, V> asMap(Function<String, K> keyMapper, BiFunction<K, JsonValue, V> valueMapper) {
        return jsonObject.keySet().stream().collect(Collectors.toMap(keyMapper, k -> valueMapper.apply(keyMapper.apply(k), jsonObject.get(k))));
    }

    /**
     * Same as {@link #asMap(Function, Function)}, but assumes the values are {@link JsonObject}s.
     */
    public <K, V> Map<K, V> asObjectMap(Function<String, K> keyMapper, Function<JsonObject, V> valueMapper) {
        return jsonObject.keySet().stream().collect(Collectors.toMap(keyMapper, k -> valueMapper.apply(jsonObject.getJsonObject(k))));
    }

    /**
     * Same as {@link #asObjectMap(Function, Function)}, but also passes the key to the value mapper.
     */
    public <K, V> Map<K, V> asObjectMap(Function<String, K> keyMapper, BiFunction<K, JsonObject, V> valueMapper) {
        return jsonObject.keySet().stream().collect(Collectors.toMap(keyMapper, k -> valueMapper.apply(keyMapper.apply(k), jsonObject.getJsonObject(k))));
    }

    /**
     * Same as {@link #asMap(Function, Function)}, but assumes the values are Strings.
     */
    public <K, V> Map<K, V> asStringMap(Function<String, K> keyMapper, Function<String, V> valueMapper) {
        return jsonObject.keySet().stream().collect(Collectors.toMap(keyMapper, k -> valueMapper.apply(jsonObject.getString(k))));
    }

    /**
     * Same as {@link #asStringMap(Function, Function)}, but also passes the key to the value mapper.
     */
    public <K> Map<K, Integer> asIntegerMap(Function<String, K> keyMapper) {
        return jsonObject.keySet().stream().collect(Collectors.toMap(keyMapper, jsonObject::getInt));
    }

}
