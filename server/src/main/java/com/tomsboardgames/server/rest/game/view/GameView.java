package com.boardgamefiesta.server.rest.game.view;

import com.boardgamefiesta.api.Game;
import lombok.Value;

@Value
public class GameView {

    String id;
    String designers;
    String artists;
    String publishers;
    String website;
    int minNumberOfPlayers;
    int maxNumberOfPlayers;

    public GameView(Game game) {
        id = game.getId().getId();
        designers = game.getDesigners();
        artists = game.getArtists();
        publishers = game.getPublishers();
        website = game.getWebsite();
        minNumberOfPlayers = game.getMinNumberOfPlayers();
        maxNumberOfPlayers = game.getMaxNumberOfPlayers();
    }

}
