package com.boardgamefiesta.server.rest.exception;

import com.boardgamefiesta.domain.Repository;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ConcurrentModificationExceptionMapper implements ExceptionMapper<Repository.ConcurrentModificationException> {
    @Override
    public Response toResponse(Repository.ConcurrentModificationException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Error(exception.getErrorCode(), null, null))
                .build();
    }
}
