package com.boardgamefiesta.server.repository;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.gwt.GWT;
import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.game.Games;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.table.Tables;
import com.boardgamefiesta.server.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
class TableDynamoDbRepositoryTest {

    static final Game.Id GAME_ID = Game.Id.of("gwt");

    @Mock
    Games games;

    GameProvider<State> gameProvider = (GameProvider) new GWT();
    Game game = Game.builder().id(GAME_ID).provider(gameProvider).build();

    @Mock
    DynamoDbConfiguration config;

    @Mock
    CDI<Object> cdi;

    @Mock
    BeanManager beanManager;

    TableDynamoDbRepository tableDynamodbRepository;

    @BeforeEach
    void setUp() {
        lenient().when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);

        lenient().when(config.getTableSuffix()).thenReturn(Optional.of("-test"));

        lenient().when(games.get(GAME_ID)).thenReturn(game);

        tableDynamodbRepository = new TableDynamoDbRepository(games, DynamoDbClient.create(), config);
    }

    @Test
    void add() {
        var tableId = tableDynamodbRepository.findAll()
                .findFirst()
                .map(Table::getId)
                .orElseGet(() -> {
                    User tom = mock(User.class);
                    when(tom.getId()).thenReturn(User.Id.of("348413c8-3484-432c-ae1c-d02d1e010222"));

                    User sharon = mock(User.class);
                    when(sharon.getId()).thenReturn(User.Id.of("34efb2e1-8ef6-47e3-a1d1-3f986d2d7c1d"));

                    Table table = Table.create(this.game, Table.Mode.NORMAL, tom, new Options(Collections.emptyMap()));
                    table.invite(sharon);
                    table.acceptInvite(sharon.getId());

                    table.start();

                    tableDynamodbRepository.add(table);

                    return table.getId();
                });

        int n = 300;
        Instant startTime = Instant.now();
        for (int i = 0; i < n; i++) {
            try {
                tableDynamodbRepository.update(tableDynamodbRepository.findById(tableId, true));
            } catch (Tables.TableConcurrentlyModifiedException e) {
                e.printStackTrace();
            }
        }
        Instant endTime = Instant.now();
        System.out.println(Duration.between(startTime, endTime).dividedBy(n) + " per find");
    }

}
