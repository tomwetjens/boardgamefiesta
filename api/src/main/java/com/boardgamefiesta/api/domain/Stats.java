package com.boardgamefiesta.api.domain;

import lombok.Builder;
import lombok.Singular;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Builder(toBuilder = true)
public class Stats {

    @Singular
    private final Map<String, Object> values;

    public Set<String> keys() {
        return values.keySet();
    }

    public Optional<Object> value(String key) {
        return Optional.ofNullable(values.get(key));
    }

}
