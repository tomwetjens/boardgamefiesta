package com.wetjens.gwt.api;

import javax.json.JsonObject;
import java.io.InputStream;
import java.util.Random;
import java.util.Set;

public interface Game {

    Set<PlayerColor> getAvailableColors();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    State start(Set<Player> players, Options options, Random random);

    void executeAutoma(State state, Random random);

    Action toAction(JsonObject jsonObject, State state);

    Object toView(State state, Player viewer);

    String toView(Class<? extends Action> action);

    String getId();

    State deserialize(InputStream inputStream);

}