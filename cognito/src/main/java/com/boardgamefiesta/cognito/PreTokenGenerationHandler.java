package com.boardgamefiesta.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.boardgamefiesta.domain.exception.DomainException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.util.Set;

@Named("preTokenGeneration")
@Slf4j
public class PreTokenGenerationHandler implements RequestHandler<PreTokenGenerationEvent, PreTokenGenerationEvent> {

    private static final String DEFAULT_GROUP = "user";

    @Override
    public PreTokenGenerationEvent handleRequest(@NonNull PreTokenGenerationEvent event, Context context) {
        try {
            log.info("Pre Token Generation trigger: {}", event);

            var groupOverrideDetails = new PreTokenGenerationResponse.ClaimsOverrideDetails.GroupOverrideDetails();
            groupOverrideDetails.setGroupsToOverride(Set.of(DEFAULT_GROUP));

            var claimsOverrideDetails = new PreTokenGenerationResponse.ClaimsOverrideDetails();
            claimsOverrideDetails.setGroupOverrideDetails(groupOverrideDetails);

            var response = new PreTokenGenerationResponse();
            response.setClaimsOverrideDetails(claimsOverrideDetails);

            event.setResponse(response);

            return event;
        } catch (DomainException e) {
            throw e;
        } catch (RuntimeException e) {
            // Any other error, hide details from client
            log.error("Error occurred in Pre Token Generation trigger: {}", e.getMessage(), e);
            throw new RuntimeException("Unknown error");
        }
    }
}
