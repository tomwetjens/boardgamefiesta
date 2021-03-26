package com.boardgamefiesta.server.rest.exception;

public enum APIError {
    NO_SUCH_USER,
    MUST_BE_OWNER,
    NOT_PLAYER_IN_GAME,
    NOT_YOUR_TURN,
    MUST_SPECIFY_USERNAME_OR_EMAIL,
    NOT_SUPPORTED,
    INVALID_ACTION,
    INVALID_TIME_ZONE,
    INTERNAL_ERROR,
    USERNAME_ALREADY_IN_USE
}
