package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDynamoDbRepositoryTest {

    @Mock
    DynamoDbConfig config;

    GameDynamoDbRepository gameDynamodbRepository;

    @BeforeEach
    void setUp() {
        when(config.getTableSuffix()).thenReturn("-test");

        gameDynamodbRepository = new GameDynamoDbRepository(DynamoDbClient.create(), config);
    }

    @Test
    void add() {
        User tom = mock(User.class);
        when(tom.getId()).thenReturn(User.Id.of("348413c8-3484-432c-ae1c-d02d1e010222"));
        User sharon = mock(User.class);
        when(sharon.getId()).thenReturn(User.Id.of("34efb2e1-8ef6-47e3-a1d1-3f986d2d7c1d"));

        Game game = Game.create(tom, Collections.singleton(sharon));
        game.acceptInvite(sharon.getId());
        game.start();

        gameDynamodbRepository.add(game);

        gameDynamodbRepository.findById(game.getId());
    }

}
