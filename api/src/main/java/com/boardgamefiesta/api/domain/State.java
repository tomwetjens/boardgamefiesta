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

/**
 * Represents the actual game implementation.
 */
public interface State {
    void perform(@NonNull Player player, @NonNull Action action, @NonNull Random random);

    void addEventListener(@NonNull EventListener eventListener);

    void removeEventListener(@NonNull EventListener eventListener);

    void skip(@NonNull Player player, @NonNull Random random);

    /**
     * Ends turn of a player.
     * <p>The implementation may skip any remaining actions automatically before ending the turn.</p>
     *
     * @param player Player whose turn to end.
     * @param random Pseudo random number generator.
     */
    void endTurn(@NonNull Player player, @NonNull Random random);

    /**
     * Forces an unresponsive player's turn to be ended.
     * <p>The implementation may choose to perform actions automatically before ending the turn.</p>
     *
     * @param player Player whose turn to forcibly end.
     * @param random Pseudo random number generator.
     */
    void forceEndTurn(@NonNull Player player, @NonNull Random random);

    /**
     * Removes a player from the game after it was started.
     * <p>This happens when a player leaves or is kicked from the game.</p>
     * <p>It is up to the implementation to decide how to handle this and whether or not the game can continue.</p>
     *
     * @param player Player who left the game.
     * @param random Pseudo random number generator.
     */
    void leave(@NonNull Player player, @NonNull Random random);

    Set<Player> getCurrentPlayers();

    /**
     * Estimated progress. This is used to determine whether a game has progressed enough to keep when it's abandoned
     * and to determine whether a game has progressed enough to count as 'played'.
     *
     * @return 0-100 percent
     */
    int getProgress();

    /**
     * Can the action that led to this State be undone?
     *
     * @return <code>true</code> if the action can be undone to the previous State, <code>false</code> otherwise.
     */
    boolean canUndo();

    /**
     * Has the game ended?
     *
     * @return <code>true</code> if the game has ended, <code>false</code> otherwise.
     */
    boolean isEnded();

    int getScore(@NonNull Player player);

    /**
     * Gets a ranking of the players based on their game result, because games may have different tiebreakers.
     *
     * @return players (that haven't left) ranked by their scores. In order 1st place, then 2nd place, then 3rd place, etc.
     */
    List<Player> getRanking();

    Stats getStats(@NonNull Player player);

}
