/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
