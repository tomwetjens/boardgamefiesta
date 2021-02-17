package com.boardgamefiesta.server.domain.rating;

// https://en.wikipedia.org/wiki/Elo_rating_system
public class EloRatingSystem implements RatingSystem {

    @Override
    public int getInitialRating() {
        return 1000;
    }

    @Override
    public int calc(float actualScore, Rating currentRating, Rating opponentRating, int numberOfPlayers) {
        var expectedScore = 1f / (1f + (float) Math.pow(10, ((opponentRating.getRating() - currentRating.getRating()) / 400f)));

        var kFactor = 64f / numberOfPlayers;

        return Math.round(currentRating.getRating() + kFactor * (actualScore - expectedScore));
    }

}
