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

import java.util.List;
import java.util.Random;
import java.util.Set;

public interface State {
    void perform(@NonNull Player player, @NonNull Action action, @NonNull Random random);

    void addEventListener(@NonNull EventListener eventListener);

    void removeEventListener(@NonNull EventListener eventListener);

    void skip(@NonNull Player player, @NonNull Random random);

    void endTurn(@NonNull Player player, @NonNull Random random);

    void forceEndTurn(@NonNull Player player, @NonNull Random random);

    void leave(@NonNull Player player, @NonNull Random random);

    Set<Player> getCurrentPlayers();

    /**
     * Estimated progress. This is used to determine whether a game has progressed enough to keep when it's abandoned
     * and to determine whether a game has progressed enough to count as 'played'.
     *
     * @return 0-100 percent
     */
    int getProgress();

    boolean canUndo();

    boolean isEnded();

    int getScore(@NonNull Player player);

    /**
     * @return players (that haven't left) ranked by their scores. 0=1st place, 1=2nd place, etc. 1st place == winner
     */
    List<Player> getRanking();

    Stats stats(@NonNull Player player);

}
