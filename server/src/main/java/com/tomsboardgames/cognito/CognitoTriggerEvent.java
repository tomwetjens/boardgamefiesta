package com.tomsboardgames.cognito;

import lombok.Data;

@Data
public abstract class CognitoTriggerEvent {

    private String version;
    private String triggerSource;
    private String region;
    private String userPoolId;
    private String userName;

}
