package com.boardgamefiesta.api.domain;

import lombok.Getter;

public class InGameException extends RuntimeException {

    @Getter
    private final Game.Id gameId;

    @Getter
    private final String error;

    protected InGameException(Game.Id gameId, String error) {
        super(error);

        this.gameId = gameId;
        this.error = error;
    }

}
