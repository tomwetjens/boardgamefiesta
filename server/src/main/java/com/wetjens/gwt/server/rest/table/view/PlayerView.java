package com.wetjens.gwt.server.rest.table.view;

import com.wetjens.gwt.api.PlayerColor;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.user.view.UserView;
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
    Instant turnLimit;
    Integer score;
    Boolean winner;
    PlayerColor color;

    PlayerView(@NonNull Player player, @NonNull Map<User.Id, User> userMap) {
        id = player.getId().getId();
        type = player.getType();
        status = player.getStatus();
        this.user = player.getUserId().map(userId -> new UserView(userId, userMap.get(userId), null)).orElse(null);
        turnLimit = player.getTurnLimit().orElse(null);
        score = player.getScore().orElse(null);
        winner = player.getWinner().orElse(null);
        color = player.getColor();
    }

}
