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

package com.boardgamefiesta.api.domain;

import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Value
public class Options {

    Map<String, Object> values;

    public Options(@NonNull Map<String, Object> values) {
        this.values = new HashMap<>(values);
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public String getString(String key, String defaultValue) {
        return (String) values.getOrDefault(key, defaultValue);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return (Boolean) values.getOrDefault(key, defaultValue);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        var number = getNumber(key);
        return number != null ? number.intValue() : defaultValue;
    }

    public Float getFloat(String key, Float defaultValue) {
        var number = getNumber(key);
        return number != null ? number.floatValue() : defaultValue;
    }

    private Number getNumber(String key) {
        return (Number) values.get(key);
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> enumType, E defaultValue) {
        return values.containsKey(key) ? Enum.valueOf(enumType, (String) values.get(key)) : defaultValue;
    }
}
