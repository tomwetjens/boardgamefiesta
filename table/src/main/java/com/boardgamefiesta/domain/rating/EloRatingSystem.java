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

// https://en.wikipedia.org/wiki/Elo_rating_system
public class EloRatingSystem implements RatingSystem {

    @Override
    public int getInitialRating() {
        return 1000;
    }

    @Override
    public int calculateNewRating(float actualScore, Rating currentRating, Rating opponentRating, int numberOfPlayers) {
        var expectedScore = 1f / (1f + (float) Math.pow(10, ((opponentRating.getRating() - currentRating.getRating()) / 400f)));

        var kFactor = 64f / numberOfPlayers;

        return Math.round(currentRating.getRating() + kFactor * (actualScore - expectedScore));
    }

}
