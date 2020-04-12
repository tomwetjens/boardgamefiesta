package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.view.UserView;
import lombok.Value;

@Value
public class PlayerView {

    UserView user;
    Player color;

    PlayerView(Player player, User user) {
        this.user = new UserView(user.getId(), user);
        this.color = player;
    }
}
