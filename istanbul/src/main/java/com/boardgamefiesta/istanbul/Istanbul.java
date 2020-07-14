package com.boardgamefiesta.istanbul;

import com.boardgamefiesta.api.*;
import com.boardgamefiesta.istanbul.logic.LayoutType;
import com.boardgamefiesta.istanbul.view.ActionView;
import com.boardgamefiesta.istanbul.view.IstanbulView;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class Istanbul implements Game<com.boardgamefiesta.istanbul.logic.Game> {

    public static final Id ID = Id.of("istanbul");

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(90);

    @Override
    public Id getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return com.boardgamefiesta.istanbul.logic.Game.SUPPORTED_COLORS;
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 5;
    }

    @Override
    public com.boardgamefiesta.istanbul.logic.Game start(Set<Player> players, Options options, Random random) {
        var layoutType = options.getEnum("layoutType", LayoutType.class, LayoutType.RANDOM);
        return com.boardgamefiesta.istanbul.logic.Game.start(players, layoutType, random);
    }

    @Override
    public void executeAutoma(com.boardgamefiesta.istanbul.logic.Game state, Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object toView(com.boardgamefiesta.istanbul.logic.Game state, Player viewer) {
        return new IstanbulView(state, viewer);
    }

    @Override
    public StateDeserializer<com.boardgamefiesta.istanbul.logic.Game> getStateDeserializer() {
        return com.boardgamefiesta.istanbul.logic.Game::deserialize;
    }

    @Override
    public boolean hasAutoma() {
        return false;
    }

    @Override
    public Duration getTimeLimit(Options options) {
        return DEFAULT_TIME_LIMIT;
    }

    @Override
    public com.boardgamefiesta.istanbul.logic.Action toAction(JsonObject jsonObject, com.boardgamefiesta.istanbul.logic.Game state) {
        var type = ActionView.valueOf(jsonObject.getString("type"));
        return type.toAction(jsonObject, state);
    }
}
