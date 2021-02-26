package com.boardgamefiesta.cognito;

import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class PreTokenGenerationRequest {

    private Map<String, String> userAttributes;
    private GroupConfiguration groupConfiguration;
    private Map<String, String> clientMetadata;

    @Data
    public static class GroupConfiguration {
        private Set<String> groupsToOverride;
        private Set<String> iamRolesToOverride;
        private String preferredRole;
    }

}
