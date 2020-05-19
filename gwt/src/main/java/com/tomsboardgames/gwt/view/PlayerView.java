package com.tomsboardgames.gwt.view;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
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
