package com.boardgamefiesta.server.cognito;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.server.rest.exception.APIError;
import com.boardgamefiesta.server.rest.exception.APIException;
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
    void changeUsername(@Observes(during = TransactionPhase.IN_PROGRESS) User.UsernameChanged event) {
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
            throw APIException.badRequest(APIError.USERNAME_ALREADY_IN_USE);
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
