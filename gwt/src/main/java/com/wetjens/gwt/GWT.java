package com.wetjens.gwt;

import com.wetjens.gwt.api.Action;
import com.wetjens.gwt.api.Game;
import com.wetjens.gwt.api.*;
import com.wetjens.gwt.view.ActionType;
import com.wetjens.gwt.view.StateView;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class GWT implements Game {

    private static final Duration DEFAULT_TIME_LIMIT = Duration.of(3, ChronoUnit.MINUTES);

    @Override
    public String getId() {
        return "gwt";
    }

    @Override
    public Set<PlayerColor> getAvailableColors() {
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
    public State start(Set<Player> players, Options options, Random random) {
        return new com.wetjens.gwt.Game(players, options.getBoolean("beginner", false), random);
    }

    @Override
    public void executeAutoma(State state, Random random) {
        new Automa().execute((com.wetjens.gwt.Game) state, random);
    }

    @Override
    public Action toAction(JsonObject jsonObject, State state) {
        return ActionType.toAction(jsonObject, (com.wetjens.gwt.Game) state);
    }

    @Override
    public Object toView(State state, Player viewer) {
        return new StateView((com.wetjens.gwt.Game) state, viewer);
    }

    @Override
    public State deserialize(InputStream inputStream) {
        return com.wetjens.gwt.Game.deserialize(inputStream);
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
