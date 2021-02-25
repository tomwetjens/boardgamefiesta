package com.boardgamefiesta.server.rest.exception;

import com.boardgamefiesta.domain.AggregateRoot;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidCommandExceptionMapper implements ExceptionMapper<AggregateRoot.InvalidCommandException> {
    @Override
    public Response toResponse(AggregateRoot.InvalidCommandException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Error(exception.getErrorCode(), null, null))
                .build();
    }
}
