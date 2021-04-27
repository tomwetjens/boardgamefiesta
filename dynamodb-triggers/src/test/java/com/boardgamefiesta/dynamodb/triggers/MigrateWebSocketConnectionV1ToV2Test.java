package com.boardgamefiesta.dynamodb.triggers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class MigrateWebSocketConnectionV1ToV2Test {

    @Test
    void test() {
        MigrateWebSocketConnectionV1ToV2.main(new String[]{null, "gwt-ws-connections-test", "boardgamefiesta-dev"});
    }
}