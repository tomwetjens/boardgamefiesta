package com.boardgamefiesta.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Named("migrateUser")
@Slf4j
public class MigrateUserHandler implements RequestHandler<MigrateUserEvent, MigrateUserEvent> {

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final Optional<String> oldUserPoolId;
    private final Optional<String> oldClientId;

    @Inject
    public MigrateUserHandler(@NonNull CognitoIdentityProviderClient cognitoIdentityProviderClient,
                              @ConfigProperty(name = "oldUserPoolId") Optional<String> oldUserPoolId,
                              @ConfigProperty(name = "oldClientId") Optional<String> oldClientId) {
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
        this.oldUserPoolId = oldUserPoolId;
        this.oldClientId = oldClientId;
    }

    @Override
    public MigrateUserEvent handleRequest(MigrateUserEvent event, Context context) {
        try {
            if ("UserMigration_Authentication".equals(event.getTriggerSource())) {
                authenticate(event.getUserName(), event.getRequest().getPassword());

                event.setResponse(migrateUser(event.getUserName()));
            } else if ("UserMigration_ForgotPassword".equals(event.getTriggerSource())) {
                event.setResponse(migrateUser(event.getUserName()));
            }

            log.info("Response: {}", event.getResponse());

            return event;
        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Incorrect username or password.");
        } catch (RuntimeException e) {
            // Any other error, hide details from client
            log.error("Error occurred in Migrate User trigger: {}", e.getMessage(), e);
            throw new RuntimeException("Unknown error");
        }
    }

    private void authenticate(String userName, String password) {
        log.info("Authenticating user '{}' on user pool '{}' with client id '{}'", userName, oldUserPoolId, oldClientId);

        var response = cognitoIdentityProviderClient.adminInitiateAuth(AdminInitiateAuthRequest.builder()
                .userPoolId(oldUserPoolId.orElseThrow())
                .clientId(oldClientId.orElseThrow())
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .authParameters(Map.of(
                        "USERNAME", userName,
                        "PASSWORD", password
                ))
                .build());

        log.debug("Authentication response: {}", response);
    }

    private MigrateUserResponse migrateUser(String userName) {
        log.info("Looking up user '{}' in user pool '{}'", userName, oldUserPoolId);

        var userResponse = cognitoIdentityProviderClient.adminGetUser(AdminGetUserRequest.builder()
                .userPoolId(oldUserPoolId.orElseThrow())
                .username(userName)
                .build());

        var userAttributes = userResponse
                .userAttributes().stream()
                .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

        log.debug("User response: {}", userResponse);

        var response = new MigrateUserResponse();
        response.setUserAttributes(Map.of(
                "email", userAttributes.get("email"),
                "email_verified", userAttributes.get("email_verified"),
                "preferred_username", userName
        ));
        response.setFinalUserStatus(UserStatusType.CONFIRMED);
        response.setMessageAction(MessageActionType.SUPPRESS);

        return response;
    }
}
