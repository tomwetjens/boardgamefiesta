package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Score;
import com.boardgamefiesta.gwt.logic.ScoreCategory;
import lombok.Value;

import java.util.Map;

@Value
public class ScoreView {

    Map<ScoreCategory, Integer> categories;
    int total;

    ScoreView(Score score) {
        this.categories = score.getCategories();
        this.total = score.getTotal();
    }

}

