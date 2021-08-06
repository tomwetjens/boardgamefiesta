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

package com.boardgamefiesta.api.command;

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.State;

import javax.json.JsonObject;

public interface ActionMapper<T extends State> {
    /**
     * @throws javax.json.JsonException when JSON could not be parsed into an action.
     */
    Action toAction(JsonObject jsonObject, T state);
}
