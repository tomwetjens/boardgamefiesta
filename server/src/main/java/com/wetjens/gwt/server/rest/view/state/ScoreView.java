package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Score;
import lombok.Value;

import java.util.Map;

@Value
public class ScoreView {

    Map<Score.Category, Integer> categories;
    int total;
    boolean winner;

    ScoreView(Score score, boolean winner) {
        this.categories = score.getCategories();
        this.total = score.getTotal();
        this.winner = winner;
    }

}

