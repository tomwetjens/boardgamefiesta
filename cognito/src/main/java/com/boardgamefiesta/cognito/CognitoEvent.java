package com.boardgamefiesta.cognito;

import lombok.Data;

@Data
public abstract class CognitoEvent<Req,Res> {

    private String version;
    private String triggerSource;
    private String region;
    private String userPoolId;
    private String userName;

    private Req request;
    private Res response;

}
