package com.boardgamefiesta.cognito;

import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class PreTokenGenerationResponse {

    private ClaimsOverrideDetails claimsOverrideDetails;

    @Data
    public static class ClaimsOverrideDetails {

        private Map<String, String> claimsToAddOrOverride;
        private Set<String> claimsToSuppress;
        private GroupOverrideDetails groupOverrideDetails;

        @Data
        public static class GroupOverrideDetails {
            private Set<String> groupsToOverride;
            private Set<String> iamRolesToOverride;
            private String preferredRole;
        }
    }

}
