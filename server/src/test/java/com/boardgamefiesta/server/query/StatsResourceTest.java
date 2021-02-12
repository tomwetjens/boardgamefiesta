package com.boardgamefiesta.server.query;

import com.boardgamefiesta.server.domain.game.Games;
import com.boardgamefiesta.server.repository.DynamoDbConfiguration;
import com.boardgamefiesta.server.repository.TableDynamoDbRepository;
import com.boardgamefiesta.server.repository.UserDynamoDbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class StatsResourceTest {

    static final Pattern FILE_NAME_PATTERN = Pattern.compile("filename=\"([^\"]+)\"");

    @Mock
    DynamoDbConfiguration config;

    Games games = new Games();

    StatsResource statsResource;

    @BeforeEach
    void setUp() {
        when(config.getTableSuffix()).thenReturn(Optional.of(""));

        var dynamoDbClient = DynamoDbClient.create();
        var tables = new TableDynamoDbRepository(games, dynamoDbClient, config);
        var users = new UserDynamoDbRepository(dynamoDbClient, config);

        statsResource = new StatsResource(tables, users);
    }

    @Test
    void gwt() throws Exception{
        var response = statsResource.get("gwt");
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