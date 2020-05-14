package com.wetjens.gwt.view;

import com.wetjens.gwt.api.Player;
import com.wetjens.gwt.api.PlayerColor;
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
