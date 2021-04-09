package com.boardgamefiesta.server.rest;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.security.Principal;
import java.util.Optional;

@Path("/errors")
@PermitAll
@Slf4j
public class ErrorsResource {

    @POST
    public void logError(@Context HttpServletRequest httpServletRequest, @NonNull ErrorsResource.Error error) {
        var principal = Optional.ofNullable(httpServletRequest.getUserPrincipal()).map(Principal::getName).orElse("");
        var userAgent = httpServletRequest.getHeader("User-Agent");

        log.error("{} URL={} Principal={}, User-Agent={}\nStack Trace:\n{}", error.getMessage(), error.getUrl(), principal, userAgent, error.getStack());
    }

    @Data
    public static class Error {
        String message;
        String url;
        String stack;
    }

}
