package com.tomsboardgames.server.cognito;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostConfirmationEvent extends CognitoTriggerEvent {

    private PostConfirmationRequest request;

}
