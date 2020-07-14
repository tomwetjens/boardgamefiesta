package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
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
