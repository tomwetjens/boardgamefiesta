package com.boardgamefiesta.server.rest.exception;

import com.boardgamefiesta.domain.AggregateRoot;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotAllowedExceptionMapper implements ExceptionMapper<AggregateRoot.NotAllowedException> {
    @Override
    public Response toResponse(AggregateRoot.NotAllowedException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Error(exception.getErrorCode(), null, null))
                .build();
    }
}
