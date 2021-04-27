package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MigrateRatingV1ToV2Test {

    @Disabled
    @Test
    void test() {
        MigrateRatingV1ToV2.main(new String[]{null, "gwt-ratings-test", "boardgamefiesta-dev"});
    }
}