package com.wetjens.gwt.server.rest;

public enum APIError {
    NO_SUCH_USER,
    REACHED_MAX_GAMES,
    CANNOT_INVITE_YOURSELF,
    MUST_INVITE_AT_LEAST_1_USER,
    MAY_INVITE_AT_MOST_5_USERS,
    GAME_ALREADY_STARTED_OR_ENDED,
    GAME_NOT_STARTED_YET,
    NOT_INVITED,
    ALREADY_RESPONDED,
    MUST_BE_OWNER,
    NOT_PLAYER_IN_GAME,
    NOT_YOUR_TURN,
    MUST_SPECIFY_USERNAME_OR_EMAIL,
    NO_SUCH_ACTION
}
