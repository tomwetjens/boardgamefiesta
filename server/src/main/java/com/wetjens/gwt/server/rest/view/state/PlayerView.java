package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerColor;
import lombok.NonNull;
import lombok.Value;

@Value
public class PlayerView {

    String name;
    PlayerColor color;

    PlayerView(@NonNull Player player) {
        this.name = player.getName();
        this.color = player.getColor();
    }
}
