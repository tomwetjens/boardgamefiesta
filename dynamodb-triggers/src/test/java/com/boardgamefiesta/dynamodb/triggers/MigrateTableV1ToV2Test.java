package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class MigrateTableV1ToV2Test {

    @Test
    void test() {
        MigrateTableV1ToV2.main(new String[]{null, "gwt-games-test", "boardgamefiesta-dev"});
    }
}