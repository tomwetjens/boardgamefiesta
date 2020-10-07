package com.boardgamefiesta.server.domain;

import com.boardgamefiesta.api.domain.InGameException;
import com.boardgamefiesta.server.domain.game.Game;
import lombok.Value;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class APIException extends WebApplicationException {

    public static APIException inGame(InGameException e, Game.Id gameId) {
        return new APIException(Response.Status.BAD_REQUEST, new Error(APIError.IN_GAME_ERROR, gameId.getId(), e.getErrorCode()));
    }

    public static APIException badRequest(APIError apiError) {
        return new APIException(Response.Status.BAD_REQUEST, new Error(apiError, null, null));
    }

    public static APIException forbidden(APIError apiError) {
        return new APIException(Response.Status.FORBIDDEN, new Error(apiError, null, null));
    }

    public static APIException internalError(APIError apiError) {
        return new APIException(Response.Status.INTERNAL_SERVER_ERROR, new Error(apiError, null, null));
    }

    public static APIException conflict(APIError apiError) {
        return new APIException(Response.Status.CONFLICT, new Error(apiError, null, null));
    }

    private APIException(Response.Status status, Error error) {
        super(error.getErrorCode().name(), Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build());
    }

    @Value
    public static class Error {
        APIError errorCode;
        String gameId;
        String reasonCode;
    }

}
