package com.boardgamefiesta.server.cognito;

import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

@ApplicationScoped
class UserAttributesUpdater {

    private final Users users;
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final CognitoConfiguration cognitoConfiguration;

    @Inject
    public UserAttributesUpdater(
            @NonNull Users users,
            @NonNull CognitoIdentityProviderClient cognitoIdentityProviderClient,
            @NonNull CognitoConfiguration cognitoConfiguration) {
        this.users = users;
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
        this.cognitoConfiguration = cognitoConfiguration;
    }

    // Observe event during transaction, so if Cognito request fails, we fail the transaction as well
    void changeEmail(@Observes(during = TransactionPhase.IN_PROGRESS) User.EmailChanged event) {
        cognitoIdentityProviderClient.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                .userPoolId(cognitoConfiguration.getUserPoolId())
                .username(event.getUsername())
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
                .userPoolId(cognitoConfiguration.getUserPoolId())
                .username(event.getUsername())
                .password(event.getPassword())
                .permanent(true)
                .build());
    }

}
