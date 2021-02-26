package com.boardgamefiesta.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.boardgamefiesta.domain.exception.DomainException;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;

@Named("preSignUp")
@Slf4j
public class PreSignUpHandler implements RequestHandler<PreSignUpEvent, PreSignUpEvent> {

    private final Users users;

    @Inject
    public PreSignUpHandler(@NonNull Users users) {
        this.users = users;
    }

    @Override
    public PreSignUpEvent handleRequest(@NonNull PreSignUpEvent event, Context context) {
        try {
            log.info("Pre Sign-up trigger: {}", event);
            var preferredUsername = event.getRequest().getUserAttributes().get("preferred_username");
            var username = preferredUsername != null ? preferredUsername : event.getUserName();
            var email = event.getRequest().getUserAttributes().get("email");

            if (!"PreSignUp_AdminCreateUser".equals(event.getTriggerSource())) {
                User.validateUsername(username);

                users.validateBeforeAdd(email);
            }

            var response = new PreSignUpResponse();
            log.info("Returning from Pre Sign-up trigger: {}", response);

            event.setResponse(response);

            return event;
        } catch (DomainException e) {
            throw e;
        } catch (RuntimeException e) {
            // Any other error, hide details from client
            log.error("Error occurred in Pre Sign-up trigger: {}", e.getMessage(), e);
            throw new RuntimeException("Unknown error");
        }
    }
}
