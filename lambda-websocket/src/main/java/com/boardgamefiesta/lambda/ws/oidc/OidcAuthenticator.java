/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.lambda.ws.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;

@ApplicationScoped
public class OidcAuthenticator {

    private final String principalClaim;

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    // Is compatible with the configuration of the quarkus-oidc module
    @SneakyThrows
    @Inject
    public OidcAuthenticator(@ConfigProperty(name = "quarkus.oidc.auth-server-url") String authServerUrl,
                             @ConfigProperty(name = "quarkus.oidc.client-id") String clientId,
                             @ConfigProperty(name = "quarkus.oidc.token.principal-claim") String principalClaim) {
        this.principalClaim = principalClaim;

        jwtProcessor = new DefaultJWTProcessor<>();

        jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256),
                new RemoteJWKSet<>(new URL(authServerUrl + "/.well-known/jwks.json"))));

        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder()
                        .audience(clientId)
                        .issuer(authServerUrl)
                        .build(),
                Set.of("sub", "iss", "aud", "iat", "exp", principalClaim)
        ));
    }

    public OidcPrincipal authenticate(String token) throws OidcAuthenticationException {
        try {
            var claims = jwtProcessor.process(token, null);
            return new OidcPrincipal(claims.getStringClaim(principalClaim), claims);
        } catch (ParseException | BadJOSEException | JOSEException e) {
            throw new OidcAuthenticationException("Invalid token", e);
        }
    }


}
