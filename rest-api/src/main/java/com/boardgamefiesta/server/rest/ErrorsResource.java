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

package com.boardgamefiesta.server.rest;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Optional;

@ApplicationScoped
@Path("/errors")
@PermitAll
@Slf4j
public class ErrorsResource {

    @POST
    public void logError(@Context SecurityContext securityContext,
                         @HeaderParam("User-Agent") String userAgent,
                         @NonNull Error error) {
        var principal = Optional.ofNullable(securityContext.getUserPrincipal()).map(Principal::getName).orElse("");

        log.error("{} URL={} Principal={}, User-Agent={}\nStack Trace:\n{}", error.getMessage(), error.getUrl(), principal, userAgent, error.getStack());
    }

    @Data
    public static class Error {
        String message;
        String url;
        String stack;
    }

}
