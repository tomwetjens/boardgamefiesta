package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MigrateUserV1ToV2Test {

    @Disabled
    @Test
    void test() {
        MigrateUserV1ToV2.main(new String[]{null, "gwt-users-test", "boardgamefiesta-dev"});
    }
}