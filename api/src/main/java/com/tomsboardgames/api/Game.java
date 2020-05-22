package com.tomsboardgames.api;

import lombok.Value;

import javax.json.JsonObject;
import java.io.InputStream;
import java.time.Duration;
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

    Id getId();

    State deserialize(InputStream inputStream);

    boolean hasAutoma();

    Duration getTimeLimit(Options options);

    @Value(staticConstructor = "of")
    class Id {
        String id;
    }
}