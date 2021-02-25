package com.boardgamefiesta.domain.rating;

public interface RatingSystem {
    int getInitialRating();

    int calculateNewRating(float outcome, Rating currentRating, Rating opponentRating, int numberOfPlayers);
}
