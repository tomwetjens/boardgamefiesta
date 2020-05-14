package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.api.Game;
import lombok.Value;

@Value
public class GameView {

    String id;

    public GameView(Game game) {
        id = game.getId();
    }

}
