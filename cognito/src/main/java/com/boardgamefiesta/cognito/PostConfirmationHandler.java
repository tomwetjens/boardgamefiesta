package com.boardgamefiesta.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;

import javax.inject.Inject;
import javax.inject.Named;

@Named("postConfirmation")
@Slf4j
public class PostConfirmationHandler implements RequestHandler<PostConfirmationEvent, PostConfirmationEvent> {

    private static final String DEFAULT_GROUP = "user";

    private final Users users;
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @Inject
    public PostConfirmationHandler(@NonNull Users users,
                                   @NonNull CognitoIdentityProviderClient cognitoIdentityProviderClient) {
        this.users = users;
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    }

    @Override
    public PostConfirmationEvent handleRequest(PostConfirmationEvent event, Context context) {
        try {
            log.info("Post Confirmation trigger: {}", event);

            var email = event.getRequest().getUserAttributes().get("email");

            if (users.findIdByCognitoUsername(event.getUserName()).isEmpty()) {
                User user = User.createAutomatically(event.getUserName(), email);

                users.add(user);

                log.info("Adding user '{}' to group '{}' in user pool: {}", event.getUserName(), DEFAULT_GROUP, event.getUserPoolId());
                cognitoIdentityProviderClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                        .userPoolId(event.getUserPoolId())
                        .username(event.getUserName())
                        .groupName(DEFAULT_GROUP)
                        .build());
            }

            return event;
        } catch (Users.EmailAlreadyInUse e) {
            // Do not hide useful exception
            throw e;
        } catch (RuntimeException e) {
            // Any other error, hide details from client
            log.error("Error occurred in Post Confirmation trigger: {}", e.getMessage(), e);
            throw new RuntimeException("Unknown error");
        }
    }
}
