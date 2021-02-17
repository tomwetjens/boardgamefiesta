package com.boardgamefiesta.server.domain.rating;

public interface RatingSystem {
    int calc(float outcome, Rating currentRating, Rating opponentRating, int numberOfPlayers);

    int getInitialRating();
}
