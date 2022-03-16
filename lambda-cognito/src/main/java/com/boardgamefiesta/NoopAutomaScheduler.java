/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta;

import com.boardgamefiesta.domain.table.AutomaScheduler;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;

import javax.enterprise.context.ApplicationScoped;

// TODO Refactor so it is not necessary to provide this bean
@ApplicationScoped
public class NoopAutomaScheduler implements AutomaScheduler {
    @Override
    public void schedule(Table.Id tableId, Player.Id playerId) {
        throw new UnsupportedOperationException();
    }
}
