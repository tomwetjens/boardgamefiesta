package com.boardgamefiesta.api.domain;

import lombok.NonNull;

import java.util.*;

public interface State {
    void perform(Player player, Action action, Random random);

    void addEventListener(EventListener eventListener);

    void removeEventListener(EventListener eventListener);

    void skip(Player player, Random random);

    void endTurn(Player player, Random random);

    /**
     * @return players in order (that are still playing, not including players that left)
     */
    List<Player> getPlayerOrder();

    /**
     * @return all original players (at start of the game, including that left during the game), in original order
     */
    List<Player> getPlayers();

    int score(Player player);

    /**
     * @return players (that haven't left) ranked by their scores. 0=1st place, 1=2nd place, etc. 1st place == winner
     */
    List<Player> ranking();

    boolean isEnded();

    boolean canUndo();

    Set<Player> getCurrentPlayers();

    default Optional<Player> getPlayerByName(@NonNull String name) {
        return getPlayers().stream().filter(player -> name.equals(player.getName())).findAny();
    }

    void leave(Player player, Random random);

    Stats stats(Player player);

    Optional<Integer> getTurn(Player player);
}
