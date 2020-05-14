package com.wetjens.gwt.server.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wetjens.gwt.api.InGameException;
import lombok.Value;

public class APIException extends WebApplicationException {

    public APIException(InGameException cause) {
        this(Response.Status.BAD_REQUEST, cause.getError(), cause.getParams());
    }

    public static APIException badRequest(APIError e, Object... params) {
        return new APIException(Response.Status.BAD_REQUEST, e.name(), params);
    }

    public static APIException forbidden(APIError e, Object... params) {
        return new APIException(Response.Status.FORBIDDEN, e.name(), params);
    }

    public static APIException serverError(APIError e, Object... params) {
        return new APIException(Response.Status.INTERNAL_SERVER_ERROR, e.name(), params);
    }

    private APIException(Response.Status status, String errorCode, Object... params) {
        super(errorCode, Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Error(errorCode, params))
                .build());
    }

    @Value
    public static final class Error {
        String errorCode;
        Object[] params;
    }
}
