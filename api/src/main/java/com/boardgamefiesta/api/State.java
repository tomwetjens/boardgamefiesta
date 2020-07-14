package com.boardgamefiesta.api;

import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.List;
import java.util.Random;
import java.util.Set;

public interface State {
    void perform(@NonNull Action action, @NonNull Random random);

    void addEventListener(EventListener eventListener);

    void removeEventListener(EventListener eventListener);

    void skip(@NonNull Random random);

    void endTurn(@NonNull Random random);

    List<Player> getPlayers();

    int score(Player player);

    Set<Player> winners();

    boolean isEnded();

    Player getCurrentPlayer();

    default Player getPlayerByName(@NonNull String name) {
        return getPlayers().stream().filter(player -> name.equals(player.getName())).findAny().orElseThrow();
    }

    void leave(Player player);
}
