package com.tomsboardgames.server.cognito;

import io.quarkus.arc.config.ConfigProperties;
import lombok.Data;

import java.util.Optional;

@ConfigProperties(prefix = "gwt.cognito")
@Data
public class CognitoConfiguration {

    private String userPoolId;

}
