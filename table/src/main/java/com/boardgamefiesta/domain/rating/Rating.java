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

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rating {

    private static final RatingSystem RATING_SYSTEM = new EloRatingSystem();
    public static final float TIE = 0.5f;
    public static final float WIN = 1f;
    public static final float LOSS = 0f;

    @NonNull
    User.Id userId;

    @NonNull
    Instant timestamp;

    @NonNull
    Game.Id gameId;

    Table.Id tableId;

    int rating;

    @NonNull
    Map<User.Id, Integer> deltas;

    public static Rating initial(User.Id userId, Game.Id gameId) {
        return initial(userId, gameId, RATING_SYSTEM.getInitialRating());
    }

    static Rating initial(User.Id userId, Game.Id gameId, int rating) {
        return new Rating(userId, Instant.now(), gameId, null, rating, Collections.emptyMap());
    }

    public Optional<Table.Id> getTableId() {
        return Optional.ofNullable(tableId);
    }

    public Rating adjust(Map<User.Id, Rating> currentRatings, Table table) {
        var ranking = table.getUserRanking();
        var numberOfPlayers = table.getPlayers().size();

        var player = table.getPlayerByUserId(userId).orElseThrow();

        // Assume 1v1 sub matches for now, no teams
        // So each player's score is considered against each other player's score individually
        var deltas = table.getPlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(Player::isUser)
                .collect(Collectors.toMap(opponentPlayer -> opponentPlayer.getUserId().get(), opponentPlayer -> {
                    var opponentUserId = opponentPlayer.getUserId().get();
                    var actualScore = player.isPlaying() && !opponentPlayer.isPlaying() ? WIN
                            : !player.isPlaying() && opponentPlayer.isPlaying() ? LOSS
                            : actualScore(ranking.indexOf(userId), ranking.indexOf(opponentUserId));
                    return RATING_SYSTEM.calculateNewRating(actualScore, this, currentRatings.get(opponentUserId), numberOfPlayers) - rating;
                }));

        var rating = this.rating + deltas.values().stream().reduce(Integer::sum).orElse(0);

        return new Rating(userId, table.getEnded(), table.getGame().getId(), table.getId(), rating, deltas);
    }

    private float actualScore(int rank, int opponentRank) {
        return rank == opponentRank ? TIE : rank < opponentRank ? WIN : LOSS;
    }

}
