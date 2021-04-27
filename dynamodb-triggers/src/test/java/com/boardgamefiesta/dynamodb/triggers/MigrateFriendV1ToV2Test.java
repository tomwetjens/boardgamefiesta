package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MigrateFriendV1ToV2Test {

    @Disabled
    @Test
    void test() {
        MigrateFriendV1ToV2.main(new String[]{null, "gwt-friends-test", "boardgamefiesta-dev"});
    }
}