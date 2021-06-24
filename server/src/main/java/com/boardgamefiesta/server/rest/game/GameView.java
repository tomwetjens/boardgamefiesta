package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.domain.game.Game;
import lombok.Value;

@Value
public class GameView {

    String id;
    int minNumberOfPlayers;
    int maxNumberOfPlayers;
    boolean computerSupport;

    GameView(Game game) {
        id = game.getId().getId();
        minNumberOfPlayers = game.getMinNumberOfPlayers();
        maxNumberOfPlayers = game.getMaxNumberOfPlayers();
        computerSupport = game.hasAutoma();
    }
}
