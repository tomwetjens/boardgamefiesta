package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.view.UserView;
import lombok.NonNull;
import lombok.Value;

@Value
public class PlayerView {

    UserView user;
    Player color;

    PlayerView(@NonNull Player player, User user) {
        this.user = user != null ? new UserView(user.getId(), user) : null;
        this.color = player;
    }
}
