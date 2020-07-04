package com.tomsboardgames.istanbul;

import com.tomsboardgames.api.*;
import com.tomsboardgames.istanbul.logic.LayoutType;
import com.tomsboardgames.istanbul.view.ActionView;
import com.tomsboardgames.istanbul.view.IstanbulView;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import java.io.InputStream;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class Istanbul implements Game<com.tomsboardgames.istanbul.logic.Game> {

    public static final Id ID = Id.of("istanbul");

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(90);

    @Override
    public Id getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return com.tomsboardgames.istanbul.logic.Game.SUPPORTED_COLORS;
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
    public String getPublishers() {
        return "Pegasus Spiele";
    }

    @Override
    public String getDesigners() {
        return "Rüdiger Dorn";
    }

    @Override
    public String getArtists() {
        return "Andreas Resch, Hans-Georg Schneider";
    }

    @Override
    public String getWebsite() {
        return "https://boardgamegeek.com/boardgame/148949/istanbul/";
    }

    @Override
    public com.tomsboardgames.istanbul.logic.Game start(Set<Player> players, Options options, Random random) {
        var layoutType = options.getEnum("layoutType", LayoutType.class, LayoutType.RANDOM);
        return com.tomsboardgames.istanbul.logic.Game.start(players, layoutType, random);
    }

    @Override
    public void executeAutoma(com.tomsboardgames.istanbul.logic.Game state, Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object toView(com.tomsboardgames.istanbul.logic.Game state, Player viewer) {
        return new IstanbulView(state, viewer);
    }

    @Override
    public com.tomsboardgames.istanbul.logic.Game deserialize(JsonObject jsonObject) {
        return com.tomsboardgames.istanbul.logic.Game.deserialize(jsonObject);
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
    public com.tomsboardgames.istanbul.logic.Action toAction(JsonObject jsonObject, com.tomsboardgames.istanbul.logic.Game state) {
        var type = ActionView.valueOf(jsonObject.getString("type"));
        return type.toAction(jsonObject, state);
    }
}
