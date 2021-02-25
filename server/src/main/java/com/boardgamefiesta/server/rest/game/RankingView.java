package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.Value;

@Value
public class RankingView {

    UserView user;
    int rating;

    RankingView(User user, int rating) {
        this.user = new UserView(user.getId(), user, null);
        this.rating = rating;
    }
}
