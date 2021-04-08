package com.boardgamefiesta.server.cognito;

import io.quarkus.oidc.TenantResolver;
import io.quarkus.oidc.runtime.OidcConfig;
import io.vertx.ext.web.RoutingContext;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import java.net.URI;

/**
 * Resolves a Cognito JWT to a Quarkus OIDC tenant, based on issuer.
 *
 * <p>To facilitate migrating users from an existing User Pool to a new User Pool, while still allowing
 * tokens from the old User Pool, we configure each User Pool as a tenant in Quarkus OIDC.</p>
 *
 * <p>In a Cognito, the <code>issuer</code> is always equal to the <code>quarkus.oidc.auth-server-url</code>.
 * So we can used that to match the correct tenant.
 */
@ApplicationScoped
@Slf4j
public class CognitoTenantResolver implements TenantResolver {

    private static final String BEARER = "Bearer";
    private static final String TOKEN = "token";

    private final OidcConfig oidcConfig;

    @Inject
    public CognitoTenantResolver(@NonNull OidcConfig oidcConfig) {
        this.oidcConfig = oidcConfig;
    }

    @SneakyThrows
    @Override
    public String resolve(RoutingContext context) {
        var token = extractTokenFromRequest(context);
        if (token == null) {
            log.debug("No token in request");
            return null;
        }

        JwtClaims jwt = parseJWT(token);
        if (jwt == null) {
            log.debug("No valid JWT in request");
            return null;
        }

        var issuer = jwt.getIssuer();
        if (!isCognito(URI.create(issuer))) {
            log.debug("Issuer is not Cognito: {}", issuer);
            return null;
        }

        return resolveTenantByAuthServerURL(issuer);
    }

    private boolean isCognito(URI issuer) {
        return issuer.getScheme().equals("https")
                && issuer.getHost().startsWith("cognito-idp.") && issuer.getHost().endsWith(".amazonaws.com")
                && issuer.getPath() != null;
    }

    private JwtClaims parseJWT(String jwt) {
        try {
            return new JwtConsumerBuilder()
                    .setSkipSignatureVerification()
                    .setSkipAllValidators()
                    .build().processToClaims(jwt);
        } catch (InvalidJwtException e) {
            log.debug("Invalid JWT: " + jwt, e);
            return null;
        }
    }

    private String extractTokenFromRequest(RoutingContext context) {
        var header = context.request().getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null) {
            var parts = header.split(" ", 2);
            var scheme = parts[0];

            if (scheme.equalsIgnoreCase(BEARER) && parts.length > 1) {
                return parts[1];
            }
            return null;
        } else {
            return context.request().getParam(TOKEN);
        }
    }

    private String resolveTenantByAuthServerURL(String url) {
        return oidcConfig.namedTenants.values()
                .stream()
                .filter(tenant -> tenant.getAuthServerUrl().map(url::equals).orElse(false))
                .flatMap(tenant -> tenant.getTenantId().stream())
                .findFirst()
                .orElse(null);
    }

}


