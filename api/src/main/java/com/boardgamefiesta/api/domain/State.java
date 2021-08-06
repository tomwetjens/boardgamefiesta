/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
