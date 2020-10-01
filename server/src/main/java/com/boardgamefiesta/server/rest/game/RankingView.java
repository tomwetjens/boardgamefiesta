package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.server.domain.User;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.Value;

@Value
public class RankingView {

    UserView user;

    RankingView(User user) {
        this.user = new UserView(user.getId(), user, null);
    }
}
