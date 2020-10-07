package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.rating.Rating;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.Value;

@Value
public class RankingView {

    UserView user;
    float rating;

    RankingView(User user, Rating latest) {
        this.user = new UserView(user.getId(), user, null);
        this.rating = latest.getRating();
    }
}
