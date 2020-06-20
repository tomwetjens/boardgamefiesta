package com.tomsboardgames.api;

import lombok.Value;

import javax.json.JsonObject;
import java.io.InputStream;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

public interface Game<T extends State> {

    Set<PlayerColor> getSupportedColors();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    String getPublishers();

    String getDesigners();

    String getArtists();

    String getWebsite();

    T start(Set<Player> players, Options options, Random random);

    void executeAutoma(T state, Random random);

    Action toAction(JsonObject jsonObject, T state);

    Object toView(T state, Player viewer);

    Id getId();

    T deserialize(InputStream inputStream);

    boolean hasAutoma();

    Duration getTimeLimit(Options options);

    @Value(staticConstructor = "of")
    class Id {
        String id;
    }
}