package com.wetjens.gwt.server.game.repository;

import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameRepositoryTest {

    GameRepository gameRepository;

    @BeforeEach
    void setUp() {
        gameRepository = new GameRepository(DynamoDbClient.builder()
                .region(Region.EU_WEST_1)
                .build());
    }

    @Test
    void findById() {
        gameRepository.findById(Game.Id.of("1"));
    }

    @Test
    void findByUserId() {
        gameRepository.findByUserId(User.Id.of("1"));
    }

    @Test
    void add() throws InterruptedException {
        User playerA = mock(User.class);
        when(playerA.getId()).thenReturn(User.Id.of("1"));
        User playerB = mock(User.class);
        when(playerB.getId()).thenReturn(User.Id.of("2"));

        Game game = Game.create(playerA, Collections.singleton(playerB));
        game.acceptInvite(playerB.getId());
        game.start();

        gameRepository.add(game);

        gameRepository.findById(game.getId());
    }

    @Test
    void update() {
    }
}
