package com.wetjens.gwt.view;

import com.wetjens.gwt.api.Score;
import lombok.Value;

import java.util.Map;

@Value
public class ScoreView {

    Map<String, Integer> categories;
    int total;

    ScoreView(Score score) {
        this.categories = score.getCategories();
        this.total = score.getTotal();
    }

}

