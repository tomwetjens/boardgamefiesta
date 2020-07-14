package com.boardgamefiesta.api;

import lombok.Value;

import javax.json.JsonObject;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

public interface Game<T extends State> {

    Set<PlayerColor> getSupportedColors();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    T start(Set<Player> players, Options options, Random random);

    void executeAutoma(T state, Random random);

    Action toAction(JsonObject jsonObject, T state);

    Object toView(T state, Player viewer);

    Id getId();

    T deserialize(JsonObject jsonObject);

    boolean hasAutoma();

    Duration getTimeLimit(Options options);

    @Value(staticConstructor = "of")
    class Id {
        String id;
    }
}