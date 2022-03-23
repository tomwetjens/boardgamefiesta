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

package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Ratings extends Repository {

    Stream<Rating> findHistoric(User.Id userId, Game.Id gameId, Instant from, Instant to);

    Optional<Rating> findByTable(User.Id userId, Table.Id tableId);

    Rating findLatest(User.Id userId, Game.Id gameId, Instant before);

    void addAll(Collection<Rating> ratings);

    Stream<Ranking> findRanking(Game.Id gameId, int maxResults);

}
