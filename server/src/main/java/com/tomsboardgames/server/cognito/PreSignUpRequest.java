package com.boardgamefiesta.server.cognito;

import lombok.Data;

import java.util.Map;

@Data
public class PreSignUpRequest {

    private Map<String, String> userAttributes;
    private Map<String, String> validationData;
    private Map<String, String> clientMetadata;

}
