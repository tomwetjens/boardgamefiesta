package com.boardgamefiesta.server.rest.game.view;

import com.boardgamefiesta.api.Game;
import lombok.Value;

@Value
public class GameView {

    String id;
    int minNumberOfPlayers;
    int maxNumberOfPlayers;

    public GameView(Game game) {
        id = game.getId().getId();
        minNumberOfPlayers = game.getMinNumberOfPlayers();
        maxNumberOfPlayers = game.getMaxNumberOfPlayers();
    }

}
