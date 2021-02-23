package com.boardgamefiesta.api.domain;

import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public interface State {
    void perform(Player player, Action action, Random random);

    void addEventListener(EventListener eventListener);

    void removeEventListener(EventListener eventListener);

    void skip(Player player, Random random);

    void endTurn(Player player, Random random);

    List<Player> getPlayers();

    Optional<Integer> score(Player player);

    Set<Player> winners();

    boolean isEnded();

    boolean canUndo();

    Set<Player> getCurrentPlayers();

    default Optional<Player> getPlayerByName(@NonNull String name) {
        return getPlayers().stream().filter(player -> name.equals(player.getName())).findAny();
    }

    void leave(Player player, Random random);

    Stats stats(Player player);
}
