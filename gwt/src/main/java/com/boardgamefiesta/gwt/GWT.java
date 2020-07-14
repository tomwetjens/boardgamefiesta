package com.boardgamefiesta.gwt;

import com.boardgamefiesta.api.Action;
import com.boardgamefiesta.api.Game;
import com.boardgamefiesta.api.*;
import com.boardgamefiesta.gwt.view.ActionType;
import com.boardgamefiesta.gwt.view.StateView;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class GWT implements Game<com.boardgamefiesta.gwt.Game> {

    public static final Id ID = Id.of("gwt");

    private static final Duration DEFAULT_TIME_LIMIT = Duration.of(3, ChronoUnit.MINUTES);

    @Override
    public Game.Id getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return EnumSet.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.WHITE);
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 4;
    }

    @Override
    public com.boardgamefiesta.gwt.Game start(Set<Player> players, Options options, Random random) {
        return com.boardgamefiesta.gwt.Game.start(players, options.getBoolean("beginner", false), random);
    }

    @Override
    public void executeAutoma(com.boardgamefiesta.gwt.Game state, Random random) {
        new Automa().execute(state, random);
    }

    @Override
    public Action toAction(JsonObject jsonObject, com.boardgamefiesta.gwt.Game state) {
        return ActionType.toAction(jsonObject, state);
    }

    @Override
    public Object toView(com.boardgamefiesta.gwt.Game state, Player viewer) {
        return new StateView(state, viewer);
    }

    @Override
    public com.boardgamefiesta.gwt.Game deserialize(JsonObject jsonObject) {
        return com.boardgamefiesta.gwt.Game.deserialize(jsonObject);
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

    @Override
    public Duration getTimeLimit(Options options) {
        return DEFAULT_TIME_LIMIT;
    }
}
