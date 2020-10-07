package com.boardgamefiesta.server.cognito;

import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.user.Users;
import lombok.NonNull;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
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

    void emailChanged(@Observes User.EmailChanged event) {
        var user = users.findById(event.getUserId(), false);

        cognitoIdentityProviderClient.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                .userPoolId(cognitoConfiguration.getUserPoolId())
                .username(user.getUsername())
                .userAttributes(AttributeType.builder()
                        .name("email")
                        .value(event.getEmail())
                        .build())
                .build());
    }

}
