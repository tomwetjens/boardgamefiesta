package com.boardgamefiesta.dynamodb;

import io.quarkus.arc.config.ConfigProperties;
import lombok.Data;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ConfigProperties(prefix = "gwt.dynamodb")
@Data
public class DynamoDbConfiguration {

    private String tableName = "boardgamefiesta-prod";

    private int writeGameIdShards = 2;
    private int readGameIdShards = 2;

}
