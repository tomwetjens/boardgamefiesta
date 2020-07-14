package com.boardgamefiesta.api;

import lombok.Value;

import java.time.Duration;
import java.util.Random;
import java.util.Set;

public interface Game<T extends State> {

    Set<PlayerColor> getSupportedColors();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    T start(Set<Player> players, Options options, Random random);

    void executeAutoma(T state, Random random);

    ActionMapper<T> getActionMapper();

    ViewMapper<T> getViewMapper();

    Id getId();

    boolean hasAutoma();

    Duration getTimeLimit(Options options);

    StateSerializer<T> getStateSerializer();

    StateDeserializer<T> getStateDeserializer();

    @Value(staticConstructor = "of")
    class Id {
        String id;
    }
}