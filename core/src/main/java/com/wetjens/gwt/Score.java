package com.wetjens.gwt;

import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Value
public class Score {

    Map<Category, Integer> categories;

    Score add(Score score) {
        var map = new HashMap<>(categories);
        map.putAll(score.categories);
        return new Score(map);
    }

    public Map<Category, Integer> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public int getTotal() {
        return categories.values().stream().mapToInt(Integer::intValue).sum();
    }

    public enum Category {
        DOLLARS,
        CATTLE_CARDS,
        OBJECTIVE_CARDS,
        STATION_MASTERS,
        WORKERS,
        HAZARDS,
        EXTRA_STEP_POINTS,
        JOB_MARKET_TOKEN,
        BUILDINGS,
        CITIES,
        STATIONS
    }

}
