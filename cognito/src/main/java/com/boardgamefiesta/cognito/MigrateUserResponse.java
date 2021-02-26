package com.boardgamefiesta.cognito;

import lombok.Data;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

import java.util.Map;

@Data
public class MigrateUserResponse {

    private Map<String, String> userAttributes;
    private UserStatusType finalUserStatus;
    private MessageActionType messageAction;
    private boolean forceAliasCreation;

}
