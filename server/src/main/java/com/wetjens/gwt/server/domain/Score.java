package com.wetjens.gwt.server.domain;

import lombok.Value;

import java.util.Collections;
import java.util.Map;

@Value
public class Score {

    Map<String, Integer> categories;

    public Map<String, Integer> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public int getTotal() {
        return categories.values().stream().mapToInt(Integer::intValue).sum();
    }

}
