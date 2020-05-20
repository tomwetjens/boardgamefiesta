package com.tomsboardgames.cognito;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PreSignUpEvent extends CognitoTriggerEvent {

    private PreSignUpRequest request;

}
