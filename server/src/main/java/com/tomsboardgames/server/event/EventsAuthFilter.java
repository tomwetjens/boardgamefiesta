package com.tomsboardgames.server.event;

import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@WebFilter("/events")
@Slf4j
public class EventsAuthFilter implements Filter {

    @Inject
    private OidcIdentityProvider oidcIdentityProvider;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        var token = httpServletRequest.getParameter("token");

        if (token != null && !"".equals(token)) {
            try {

                IdTokenCredential idTokenCredential = new IdTokenCredential(token, null);
                TokenAuthenticationRequest tokenAuthenticationRequest = new TokenAuthenticationRequest(idTokenCredential);

                SecurityIdentity securityIdentity = oidcIdentityProvider.authenticate(tokenAuthenticationRequest, function -> CompletableFuture.completedFuture(function.get()))
                        .toCompletableFuture().get();

                filterChain.doFilter(new AuthenticatedRequest(httpServletRequest, securityIdentity.getPrincipal()), servletResponse);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Websockets auth filter error", e);
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
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

