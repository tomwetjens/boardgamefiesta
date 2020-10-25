package com.boardgamefiesta.api.domain;

import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public interface State {
    void perform(@NonNull Action action, @NonNull Random random);

    void addEventListener(EventListener eventListener);

    void removeEventListener(EventListener eventListener);

    void skip(@NonNull Random random);

    void endTurn(@NonNull Random random);

    List<Player> getPlayers();

    Optional<Integer> score(Player player);

    Set<Player> winners();

    boolean isEnded();

    boolean canUndo();

    Player getCurrentPlayer();

    default Optional<Player> getPlayerByName(@NonNull String name) {
        return getPlayers().stream().filter(player -> name.equals(player.getName())).findAny();
    }

    void leave(Player player);
}
