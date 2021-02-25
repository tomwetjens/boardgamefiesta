package com.boardgamefiesta.server.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class APIException extends WebApplicationException {

    public static APIException badRequest(APIError apiError) {
        return new APIException(Response.Status.BAD_REQUEST, new Error(apiError.name(), null, null));
    }

    public static APIException forbidden(APIError apiError) {
        return new APIException(Response.Status.FORBIDDEN, new Error(apiError.name(), null, null));
    }

    public static APIException internalError(APIError apiError) {
        return new APIException(Response.Status.INTERNAL_SERVER_ERROR, new Error(apiError.name(), null, null));
    }

    private APIException(Response.Status status, Error error) {
        super(error.getErrorCode(), Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build());
    }

}
