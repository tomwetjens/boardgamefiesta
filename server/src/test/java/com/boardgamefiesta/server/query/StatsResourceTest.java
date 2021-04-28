package com.boardgamefiesta.server.query;

import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.dynamodb.*;
import com.boardgamefiesta.server.rest.game.StatsResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.regex.Pattern;

@ExtendWith(MockitoExtension.class)
@Disabled
class StatsResourceTest {

    static final Pattern FILE_NAME_PATTERN = Pattern.compile("filename=\"([^\"]+)\"");

    DynamoDbConfiguration config = new DynamoDbConfiguration();

    Games games = new Games();

    StatsResource statsResource;

    @BeforeEach
    void setUp() {
        config.setTableName("boardgamefiesta-prod");
        config.setReadGameIdShards(2);
        config.setWriteGameIdShards(2);

        var dynamoDbClient = DynamoDbClient.create();
        var tables = new TableDynamoDbRepositoryV2(games, dynamoDbClient, config);
        var users = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
        var ratings = new RatingDynamoDbRepositoryV2(dynamoDbClient, config);

        statsResource = new StatsResource(tables, users, ratings);
    }

    @Test
    void gwt() throws Exception {
        var from = Instant.parse("2021-04-21T00:00:00.000Z");
        var response = statsResource.get("gwt", from);
        var streamingOutput = (StreamingOutput) response.getEntity();

        var fileName = extractFileName(response);

        try (var outputStream = Files.newOutputStream(Paths.get(fileName))) {
            streamingOutput.write(outputStream);
        }
    }

    private String extractFileName(Response response) {
        var contentDisposition = response.getHeaderString("Content-Disposition");
        var matcher = FILE_NAME_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}