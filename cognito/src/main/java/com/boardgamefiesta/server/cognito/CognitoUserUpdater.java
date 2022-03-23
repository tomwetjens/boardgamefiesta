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

package com.boardgamefiesta.server.cognito;

import com.boardgamefiesta.domain.exception.DomainException;
import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import org.eclipse.microprofile.jwt.JsonWebToken;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.net.URI;

@ApplicationScoped
class CognitoUserUpdater {

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final JsonWebToken jsonWebToken;

    @Inject
    public CognitoUserUpdater(
            @NonNull CognitoIdentityProviderClient cognitoIdentityProviderClient,
            @NonNull JsonWebToken jsonWebToken) {
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
        this.jsonWebToken = jsonWebToken;
    }

    // Observe event during transaction, so if Cognito request fails, we fail the transaction as well
    void changeEmail(@Observes(during = TransactionPhase.IN_PROGRESS) User.EmailChanged event) {
        cognitoIdentityProviderClient.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                .userPoolId(getUserPoolId())
                .username(event.getCognitoUsername())
                .userAttributes(
                        AttributeType.builder()
                                .name("email")
                                .value(event.getEmail())
                                .build(),
                        AttributeType.builder()
                                .name("email_verified")
                                .value(Boolean.TRUE.toString())
                                .build())
                .build());
    }

    // Observe event during transaction, so if Cognito request fails, we fail the transaction as well
    void changePassword(@Observes(during = TransactionPhase.IN_PROGRESS) User.PasswordChanged event) {
        cognitoIdentityProviderClient.adminSetUserPassword(AdminSetUserPasswordRequest.builder()
                .userPoolId(getUserPoolId())
                .username(event.getCognitoUsername())
                .password(event.getPassword())
                .permanent(true)
                .build());
    }

    // Observe event during transaction, so if Cognito request fails, we fail the transaction as well
    void changeUsername(@Observes(during = TransactionPhase.IN_PROGRESS) User.UsernameChanged event) throws DomainException {
        try {
            cognitoIdentityProviderClient.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(getUserPoolId())
                    .username(event.getCognitoUsername())
                    .userAttributes(
                            AttributeType.builder()
                                    .name("preferred_username")
                                    .value(event.getUsername())
                                    .build())
                    .build());
        } catch (AliasExistsException e) {
            throw new User.UsernameAlreadyInUse();
        }
    }

    // Observe event during transaction, so if Cognito request fails, we fail the transaction as well
    void deleted(@Observes(during = TransactionPhase.IN_PROGRESS) User.Deleted event) {
        cognitoIdentityProviderClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                .userPoolId(getUserPoolId())
                .username(event.getCognitoUsername())
                .build());
    }

    String getUserPoolId() {
        var issuer = jsonWebToken.getIssuer();

        if (!issuer.startsWith("https://cognito-idp.")) {
            throw new IllegalArgumentException("Not Cognito URL: " + issuer);
        }

        return URI.create(issuer).getPath().replaceFirst("/", "");
    }

}
