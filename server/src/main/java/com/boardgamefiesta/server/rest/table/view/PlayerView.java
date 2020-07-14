package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.server.domain.Player;
import com.boardgamefiesta.server.domain.User;
import com.boardgamefiesta.server.domain.rating.Rating;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

@Value
public class PlayerView {

    String id;
    Player.Type type;
    UserView user;
    Player.Status status;
    Float rating;
    Instant turnLimit;
    Integer score;
    Boolean winner;
    PlayerColor color;

    PlayerView(@NonNull Player player,
               @NonNull Function<User.Id, User> userFunction,
               @NonNull Map<User.Id, Rating> ratingMap) {
        id = player.getId().getId();
        type = player.getType();
        status = player.getStatus();
        rating = player.getUserId().map(ratingMap::get).map(Rating::getRating).orElse(null);
        this.user = player.getUserId().map(userId -> new UserView(userId, userFunction.apply(userId), null)).orElse(null);
        turnLimit = player.getTurnLimit().orElse(null);
        score = player.getScore().orElse(null);
        winner = player.getWinner().orElse(null);
        color = player.getColor();
    }

}
