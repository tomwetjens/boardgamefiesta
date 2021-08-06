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

package com.boardgamefiesta.domain.featuretoggle;

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.user.User;
import lombok.Builder;

import java.util.Optional;
import java.util.Set;

@Builder
public class FeatureToggle implements AggregateRoot {

    public void throwIfNotContains(User.Id userId) {
        if (!isEnabled(userId)) {
            throw new NotEnabledException();
        }
    }

    public boolean isEnabled(User.Id userId) {
        return userIds.contains(userId);
    }

    public enum Id {

        POWER_GRID,
        GWT2;

        public static Optional<Id> forGameId(Game.Id gameId) {
            // TODO Make this smarter
            if (gameId.getId().equals("gwt2")) {
                return Optional.of(GWT2);
            } else if (gameId.getId().equals("power-grid")) {
                return Optional.of(POWER_GRID);
            }
            return Optional.empty();
        }
    }

    private final Id id;

    private final Set<User.Id> userIds;

    public static final class NotEnabledException extends NotAllowedException {
        public NotEnabledException() {
            super("FEATURE_NOT_ENABLED");
        }
    }

}
