package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.view.UserView;
import lombok.Value;

@Value
public class PlayerView {

    String name;
    UserView user;
    Player.Color color;

    PlayerView(Player player, User user) {
        this.name = player.getName();
        this.user = new UserView(user.getId(), user);
        this.color = player.getColor();
    }
}
