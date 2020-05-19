package com.tomsboardgames.server.rest.game.view;

import com.tomsboardgames.api.Game;
import lombok.Value;

@Value
public class GameView {

    String id;

    public GameView(Game game) {
        id = game.getId();
    }

}
