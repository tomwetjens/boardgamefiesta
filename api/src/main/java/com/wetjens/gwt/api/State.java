package com.wetjens.gwt.api;

import lombok.NonNull;

import java.io.OutputStream;
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

    void serialize(OutputStream outputStream);

    Set<Player> winners();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    boolean isEnded();

    Player getCurrentPlayer();

    default Player getPlayerByName(@NonNull String name) {
        return getPlayers().stream().filter(player -> name.equals(player.getName())).findAny().orElseThrow();
    }

    void leave(Player player);
}
