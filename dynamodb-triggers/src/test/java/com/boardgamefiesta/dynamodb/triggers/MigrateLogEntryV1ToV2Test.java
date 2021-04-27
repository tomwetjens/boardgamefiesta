package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MigrateLogEntryV1ToV2Test {

    @Disabled
    @Test
    void test() {
        MigrateLogEntryV1ToV2.main(new String[]{null, "gwt-log-test", "boardgamefiesta-dev"});
    }
}