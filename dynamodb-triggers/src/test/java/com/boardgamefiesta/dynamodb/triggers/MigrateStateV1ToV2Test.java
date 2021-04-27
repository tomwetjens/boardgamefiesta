package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class MigrateStateV1ToV2Test {

    @Test
    void test() {
        MigrateStateV1ToV2.main(new String[]{null, "gwt-state", "boardgamefiesta-dev"});
    }
}