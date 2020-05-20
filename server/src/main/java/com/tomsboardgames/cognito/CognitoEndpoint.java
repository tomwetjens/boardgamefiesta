package com.tomsboardgames.cognito;

import com.tomsboardgames.server.domain.APIError;
import com.tomsboardgames.server.domain.APIException;
import com.tomsboardgames.server.domain.User;
import com.tomsboardgames.server.domain.Users;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

// TODO Secure Cognito trigger endpoint
@Path("/cognito")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class CognitoEndpoint {

    private final Users users;
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @Inject
    public CognitoEndpoint(
            @NonNull Users users,
            @NonNull CognitoIdentityProviderClient cognitoIdentityProviderClient) {
        this.users = users;
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    }

    @POST
    @Path("presignup")
    public PreSignUpResponse preSignUp(@NotNull @Valid PreSignUpEvent event) {
        try {
            log.info("Pre Sign-up trigger: {}", event);

            var username = event.getRequest().getUserAttributes().get("cognito:username");
            var email = event.getRequest().getUserAttributes().get("email");

            log.info("Validating username: {}", username);
            User.validateUsername(username);

            log.info("Checking if e-mail address already in use: {}", email);
            users.findByEmail(email).ifPresent(user -> {
                log.info("E-mail address already in use: {}", email);
                throw APIException.badRequest(APIError.EMAIL_ALREADY_IN_USE);
            });

            var response = new PreSignUpResponse();

            log.info("Returning from Pre Sign-up trigger: {}", response);

            return response;
        } catch (RuntimeException e) {
            log.error("Error occurrend in Pre Sign-up trigger: {}", e.getMessage(), e);
            throw e;
        }
    }

    @POST
    @Path("postconfirmation")
    public void postConfirmation(@NotNull @Valid PostConfirmationEvent event) {
        try {
            log.info("Post Confirmation trigger: {}", event);

            log.info("Adding user '{}' to group '{}' in user pool: {}", event.getUserName(), "users", event.getUserPoolId());
            cognitoIdentityProviderClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(event.getUserPoolId())
                    .username(event.getUserName())
                    .groupName("users")
                    .build());
        } catch (RuntimeException e) {
            log.error("Error occurrend in Post Confirmation trigger: {}", e.getMessage(), e);
            throw e;
        }
    }

}
