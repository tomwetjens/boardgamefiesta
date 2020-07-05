package com.tomsboardgames.server.repository;

import com.tomsboardgames.api.Game;
import com.tomsboardgames.api.Options;
import com.tomsboardgames.api.State;
import com.tomsboardgames.server.domain.Games;
import com.tomsboardgames.server.domain.Table;
import com.tomsboardgames.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class TableDynamoDbRepositoryTest {

    @Mock
    Games games;

    @Mock
    Game<State> game;

    @Mock
    DynamoDbConfiguration config;

    TableDynamoDbRepository tableDynamodbRepository;

    @BeforeEach
    void setUp() {
        when(config.getTableSuffix()).thenReturn(Optional.of("-test"));

        tableDynamodbRepository = new TableDynamoDbRepository(games, DynamoDbClient.create(), config);
    }

    @Test
    void add() {
        User tom = mock(User.class);
        when(tom.getId()).thenReturn(User.Id.of("348413c8-3484-432c-ae1c-d02d1e010222"));
        User sharon = mock(User.class);
        when(sharon.getId()).thenReturn(User.Id.of("34efb2e1-8ef6-47e3-a1d1-3f986d2d7c1d"));

        Table table = Table.create(game, Table.Mode.NORMAL, tom, Collections.singleton(sharon), new Options(Collections.emptyMap()));
        table.acceptInvite(sharon.getId());
        table.start();

        tableDynamodbRepository.add(table);

        tableDynamodbRepository.findById(table.getId());
    }

}
