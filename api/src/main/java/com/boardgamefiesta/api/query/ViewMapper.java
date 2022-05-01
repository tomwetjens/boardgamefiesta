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

package com.boardgamefiesta.api.query;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import lombok.NonNull;

import javax.json.stream.JsonGenerator;

public interface ViewMapper<T extends State> {
    /**
     * @deprecated Implement and use {@link #serialize(State, Player, JsonGenerator)} instead.
     */
    @Deprecated
    Object toView(@NonNull T state, Player viewer);

    default boolean isJsonGeneratorSupported() {
        return false;
    }

    default void serialize(@NonNull T state, Player viewer, @NonNull JsonGenerator jsonGenerator) {
        throw new UnsupportedOperationException();
    }
}
