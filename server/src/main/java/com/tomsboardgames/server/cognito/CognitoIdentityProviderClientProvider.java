package com.tomsboardgames.server.cognito;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
class CognitoIdentityProviderClientProvider {

    @Produces
    CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        return CognitoIdentityProviderClient.create();
    }

}
