package com.boardgamefiesta.cognito;

import lombok.Data;

import java.util.Map;

@Data
public class PostConfirmationRequest {

    private Map<String, String> userAttributes;

}
