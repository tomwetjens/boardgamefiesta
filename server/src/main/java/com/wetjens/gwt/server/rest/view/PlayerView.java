package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import lombok.Value;

@Value
public class PlayerView {

    Player.Type type;
    UserView user;
    Player.Status status;
    com.wetjens.gwt.Player color;
    Integer score;
    Boolean winner;

    PlayerView(@NonNull Player player, User user) {
        type = player.getType();
        status = player.getStatus();
        this.user = player.getType() == Player.Type.USER ? new UserView(player.getUserId(), user) : null;
        color = player.getColor();
        score = player.getScore();
        winner = player.getWinner();
    }

}
