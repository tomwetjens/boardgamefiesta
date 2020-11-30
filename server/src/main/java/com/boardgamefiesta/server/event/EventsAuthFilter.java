package com.boardgamefiesta.server.event;

import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.CompletionException;

@WebFilter({"/events", "*/events"})
@Slf4j
public class EventsAuthFilter implements Filter {

    @Inject
    OidcIdentityProvider oidcIdentityProvider;

    @Inject
    RoutingContext routingContext;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        var token = httpServletRequest.getParameter("token");

        if (token == null || "".equals(token)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            IdTokenCredential idTokenCredential = new IdTokenCredential(token, routingContext);
            TokenAuthenticationRequest tokenAuthenticationRequest = new TokenAuthenticationRequest(idTokenCredential);

            SecurityIdentity securityIdentity = oidcIdentityProvider.authenticate(tokenAuthenticationRequest, function -> Uni.createFrom().item(function.get()))
                    .await()
                    .indefinitely();

            filterChain.doFilter(new AuthenticatedRequest(httpServletRequest, securityIdentity.getPrincipal()), servletResponse);
        } catch (CompletionException e) {
            if (e.getCause() instanceof AuthenticationFailedException) {
                log.debug("Authentication failed", e);
                httpServletResponse.sendError(401);
            } else {
                log.error("Unexpected error during authentication", e);
                httpServletResponse.sendError(500);
            }
        }
    }

    private static class AuthenticatedRequest extends HttpServletRequestWrapper {

        private final Principal principal;

        public AuthenticatedRequest(HttpServletRequest httpServletRequest, Principal principal) {
            super(httpServletRequest);
            this.principal = principal;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }
    }
}

