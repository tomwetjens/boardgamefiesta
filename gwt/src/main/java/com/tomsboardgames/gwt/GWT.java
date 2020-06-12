package com.tomsboardgames.gwt;

import com.tomsboardgames.api.Action;
import com.tomsboardgames.api.Game;
import com.tomsboardgames.api.*;
import com.tomsboardgames.gwt.view.ActionType;
import com.tomsboardgames.gwt.view.StateView;

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
    public String getPublishers() {
        return "eggertspiele";
    }

    @Override
    public String getDesigners() {
        return "Alexander Pfister";
    }

    @Override
    public String getArtists() {
        return "Alexander Pfister, Andreas Resch";
    }

    @Override
    public String getWebsite() {
        return "https://boardgamegeek.com/boardgame/193738/great-western-trail/";
    }

    @Override
    public State start(Set<Player> players, Options options, Random random) {
        return new com.tomsboardgames.gwt.Game(players, options.getBoolean("beginner", false), random);
    }

    @Override
    public void executeAutoma(State state, Random random) {
        new Automa().execute((com.tomsboardgames.gwt.Game) state, random);
    }

    @Override
    public Action toAction(JsonObject jsonObject, State state) {
        return ActionType.toAction(jsonObject, (com.tomsboardgames.gwt.Game) state);
    }

    @Override
    public Object toView(State state, Player viewer) {
        return new StateView((com.tomsboardgames.gwt.Game) state, viewer);
    }

    @Override
    public State deserialize(InputStream inputStream) {
        return com.tomsboardgames.gwt.Game.deserialize(inputStream);
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
