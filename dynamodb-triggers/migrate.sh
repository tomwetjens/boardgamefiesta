#!/bin/sh

set -e

# DEV -> DEV
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateUserV1ToV2 -- gwt-users-test boardgamefiesta-dev
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateFriendV1ToV2 -- gwt-friends-test boardgamefiesta-dev
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateWebSocketConnectionV1ToV2 -- gwt-ws-connections-test boardgamefiesta-dev
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateTableV1ToV2 -- gwt-games-test boardgamefiesta-dev
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateStateV1ToV2 -- gwt-state-test boardgamefiesta-dev
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateLogEntryV1ToV2 -- gwt-log-test boardgamefiesta-dev
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateRatingV1ToV2 -- gwt-ratings-test boardgamefiesta-dev

# PROD -> DEV
java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateUserV1ToV2 -- gwt-users boardgamefiesta-dev
java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateFriendV1ToV2 -- gwt-friends boardgamefiesta-dev
java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateWebSocketConnectionV1ToV2 -- gwt-ws-connections boardgamefiesta-dev
java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateTableV1ToV2 -- gwt-games boardgamefiesta-dev
java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateStateV1ToV2 -- gwt-state boardgamefiesta-dev
java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateRatingV1ToV2 -- gwt-ratings boardgamefiesta-dev

# PROD -> PROD
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateUserV1ToV2 -- gwt-users boardgamefiesta-prod
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateFriendV1ToV2 -- gwt-friends boardgamefiesta-prod
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateWebSocketConnectionV1ToV2 -- gwt-ws-connections boardgamefiesta-prod
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateTableV1ToV2 -- gwt-games boardgamefiesta-prod
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateStateV1ToV2 -- gwt-state boardgamefiesta-prod
#java -cp target/dynamodb-triggers-1.0-SNAPSHOT-jar-with-dependencies.jar com.boardgamefiesta.dynamodb.triggers.MigrateRatingV1ToV2 -- gwt-ratings boardgamefiesta-prod