package com.tomsboardgames.server.domain;

public enum APIError {
    NO_SUCH_USER,
    EXCEEDS_MAX_REALTIME_GAMES,
    CANNOT_INVITE_YOURSELF,
    GAME_ALREADY_STARTED_OR_ENDED,
    GAME_NOT_STARTED,
    NOT_INVITED,
    ALREADY_RESPONDED,
    MUST_BE_OWNER,
    NOT_PLAYER_IN_GAME,
    NOT_YOUR_TURN,
    MUST_SPECIFY_USERNAME_OR_EMAIL,
    MIN_PLAYERS,
    EXCEEDS_MAX_PLAYERS,
    GAME_ALREADY_ENDED,
    NOT_ACCEPTED,
    CANNOT_ABANDON,
    GAME_ABANDONED,
    ALREADY_INVITED,
    NOT_SUPPORTED,
    USERNAME_TOO_SHORT,
    USERNAME_TOO_LONG,
    USERNAME_INVALID_CHARS,
    USERNAME_FORBIDDEN,
    EMAIL_ALREADY_IN_USE,
    IN_GAME_ERROR,
    COMPUTER_NOT_SUPPORTED,
    NOT_TRAINING_MODE,
    HISTORY_NOT_AVAILABLE,
    INTERNAL_ERROR
}
