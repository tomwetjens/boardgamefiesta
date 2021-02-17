package com.boardgamefiesta.server.domain.rating;

import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.user.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Ranking {

    Game.Id gameId;
    User.Id userId;
    int rating;

}
