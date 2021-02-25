package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Ranking {

    Game.Id gameId;
    User.Id userId;
    int rating;

}
