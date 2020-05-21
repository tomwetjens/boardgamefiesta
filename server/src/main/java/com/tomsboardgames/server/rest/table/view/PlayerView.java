package com.tomsboardgames.server.rest.table.view;

import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.server.domain.Player;
import com.tomsboardgames.server.domain.User;
import com.tomsboardgames.server.domain.rating.Rating;
import com.tomsboardgames.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

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
               @NonNull Map<User.Id, User> userMap,
               @NonNull Map<User.Id, Rating> ratingMap) {
        id = player.getId().getId();
        type = player.getType();
        status = player.getStatus();
        rating = player.getUserId().map(ratingMap::get).map(Rating::getRating).orElse(null);
        this.user = player.getUserId().map(userId -> new UserView(userId, userMap.get(userId), null)).orElse(null);
        turnLimit = player.getTurnLimit().orElse(null);
        score = player.getScore().orElse(null);
        winner = player.getWinner().orElse(null);
        color = player.getColor();
    }

}
