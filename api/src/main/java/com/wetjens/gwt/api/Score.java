package com.wetjens.gwt.api;

import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Value
public class Score {

    Map<String, Integer> categories;

    public Score add(Score other) {
        var map = new HashMap<>(categories);
        other.getCategories().forEach((category, score) ->
                map.compute(category, (key, value) -> value != null ? value + score : score));
        return new Score(map);
    }

    public Map<String, Integer> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public int getTotal() {
        return categories.values().stream().mapToInt(Integer::intValue).sum();
    }

}

