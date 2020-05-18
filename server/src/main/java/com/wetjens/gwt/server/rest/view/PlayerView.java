package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.api.PlayerColor;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import lombok.Value;

@Value
public class PlayerView {

    String id;
    Player.Type type;
    UserView user;
    Player.Status status;
    ScoreView score;
    Boolean winner;
    PlayerColor color;

    PlayerView(@NonNull Player player, User user) {
        id = player.getId().getId();
        type = player.getType();
        status = player.getStatus();
        this.user = player.getType() == Player.Type.USER ? new UserView(player.getUserId(), user, null) : null;
        score = player.getScore() != null ? new ScoreView(player.getScore()) : null;
        winner = player.getWinner();
        color = player.getColor();
    }

}
