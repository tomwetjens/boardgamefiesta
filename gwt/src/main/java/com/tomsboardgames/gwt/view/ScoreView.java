package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.Score;
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

