package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

@Value
public class PlayerView {

    UserView user;
    Player.Status status;

    PlayerView(Player player, User user) {
        this.status = player.getStatus();
        this.user = new UserView(user);
    }

}
