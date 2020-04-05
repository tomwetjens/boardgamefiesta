package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import lombok.Value;

@Value
public class PlayerView {

    String name;
    Player.Color color;

    PlayerView(Player player) {
        name = player.getName();
        color = player.getColor();
    }
}
