package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.Player;
import com.boardgamefiesta.api.PlayerColor;
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
