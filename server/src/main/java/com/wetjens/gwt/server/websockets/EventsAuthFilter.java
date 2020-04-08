package com.wetjens.gwt.server.websockets;

import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.runtime.ContextAwareTokenCredential;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
        try {
            IdTokenCredential idTokenCredential = new IdTokenCredential(httpServletRequest.getParameter("token"), null);
            TokenAuthenticationRequest tokenAuthenticationRequest = new TokenAuthenticationRequest(idTokenCredential);

            SecurityIdentity securityIdentity = oidcIdentityProvider.authenticate(tokenAuthenticationRequest, function -> CompletableFuture.completedFuture(function.get()))
                    .toCompletableFuture().get();

            filterChain.doFilter(new AuthenticatedRequest(httpServletRequest, securityIdentity.getPrincipal()), servletResponse);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Websockets auth filter error", e);
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

