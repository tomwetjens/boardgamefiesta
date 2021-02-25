package com.boardgamefiesta.server.rest.exception;

import com.boardgamefiesta.api.domain.InGameException;
import com.boardgamefiesta.domain.table.Table;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InGameErrorExceptionMapper implements ExceptionMapper<Table.InGameError> {
    @Override
    public Response toResponse(Table.InGameError exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new Error(exception.getErrorCode(), exception.getGameId().getId(), ((InGameException) exception.getCause()).getErrorCode()))
                .build();
    }
}
