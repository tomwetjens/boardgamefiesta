package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

@Value
public class PlayerView {

    String userId;
    Player.Status status;

    PlayerView(Player player, User user) {
        userId = player.getUserId().getId();
        status = player.getStatus();
    }

}
