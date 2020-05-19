package com.wetjens.gwt.server.rest.table.view;

import com.wetjens.gwt.server.domain.Score;
import lombok.Value;

import java.util.Map;

@Value
public class ScoreView {

    Map<String, Integer> categories;
    int total;

    ScoreView(Score score) {
        categories = score.getCategories();
        total = score.getTotal();
    }

}
