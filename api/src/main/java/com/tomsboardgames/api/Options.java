package com.tomsboardgames.api;

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

}
