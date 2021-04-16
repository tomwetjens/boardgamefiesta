package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.gwt.GWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableDynamoDbRepositoryV2Test extends BaseDynamoDbRepositoryTest {

    static final Game.Id GAME_ID = Game.Id.of(GWT.ID);

    static Games games = new Games();

    static Game game = games.get(GAME_ID);

    @Mock
    CDI<Object> cdi;
    @Mock
    BeanManager beanManager;

    @Mock
    User userA;
    @Mock
    User userB;

    TableDynamoDbRepositoryV2 repository;

    @Override
    @BeforeEach
    void setUp() {
        when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);

        lenient().when(userA.getId()).thenReturn(User.Id.of(UUID.randomUUID().toString()));
        lenient().when(userB.getId()).thenReturn(User.Id.of(UUID.randomUUID().toString()));

        repository = new TableDynamoDbRepositoryV2(games, dynamoDbClient, config);
    }

    @Test
    void add() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        repository.add(table);

        var actual = repository.findById(table.getId()).orElseThrow();
        assertThat(actual).isEqualToComparingOnlyGivenFields(table, "id", "type", "mode", "options", "created", "started", "updated", "ended", "status");
        assertThat(actual.getPlayers()).hasSize(1);
        assertThat(actual.getLog().stream().collect(Collectors.toList())).hasSize(1);
        assertThat(actual.getCurrentState().get()).isEmpty();

        assertThat(repository.findActive(userA.getId())).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
    }

    @Test
    void playerAdded() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        repository.add(table);

        table = repository.findById(table.getId()).orElseThrow();
        table.invite(userB);
        repository.update(table);

        var actual = repository.findById(table.getId()).orElseThrow();
        assertThat(actual).isEqualToComparingOnlyGivenFields(table, "id", "type", "mode", "options", "created", "started", "updated", "ended", "status");
        assertThat(actual.getPlayers()).hasSize(2);
        assertThat(actual.getLog().stream().collect(Collectors.toList())).hasSize(2);
        assertThat(actual.getCurrentState().get()).isEmpty();

        assertThat(repository.findActive(userB.getId())).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userB.getId(), 10)).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userB.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
    }

    @Test
    void playerRemoved() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        table.invite(userB);
        repository.add(table);

        table = repository.findById(table.getId()).orElseThrow();
        table.rejectInvite(userB.getId());
        repository.update(table);

        var actual = repository.findById(table.getId()).orElseThrow();
        assertThat(actual).isEqualToComparingOnlyGivenFields(table, "id", "type", "mode", "options", "created", "started", "updated", "ended", "status");
        assertThat(actual.getPlayers()).hasSize(1);
        assertThat(actual.getLog().stream().collect(Collectors.toList())).hasSize(3);
        assertThat(actual.getCurrentState().get()).isEmpty();

        assertThat(repository.findActive(userB.getId())).extracting(Table::getId).doesNotContain(table.getId());
        assertThat(repository.findRecent(userB.getId(), 10)).extracting(Table::getId).doesNotContain(table.getId());
        assertThat(repository.findRecent(userB.getId(), GAME_ID, 10)).extracting(Table::getId).doesNotContain(table.getId());
    }

    @Test
    void abandoned() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        repository.add(table);

        table = repository.findById(table.getId()).orElseThrow();
        table.abandon();
        repository.update(table);

        assertThat(repository.findActive(userA.getId())).extracting(Table::getId).doesNotContain(table.getId());
        assertThat(repository.findRecent(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
    }

    @Test
    void started() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        repository.add(table);

        table = repository.findById(table.getId()).orElseThrow();
        table.invite(userB);
        table.acceptInvite(userB.getId());
        repository.update(table);

        table = repository.findById(table.getId()).orElseThrow();
        table.start();
        repository.update(table);

        assertThat(repository.findActive(userA.getId())).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findActive(userB.getId())).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
    }

    @Test
    void ended() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        repository.add(table);

        table = repository.findById(table.getId()).orElseThrow();
        table.invite(userB);
        table.acceptInvite(userB.getId());
        table.start();
        repository.update(table = table.toBuilder()
                .status(Table.Status.ENDED)
                .ended(Instant.now())
                .build());

        assertThat(repository.findActive(userA.getId())).extracting(Table::getId).doesNotContain(table.getId());
        assertThat(repository.findActive(userB.getId())).extracting(Table::getId).doesNotContain(table.getId());
        assertThat(repository.findRecent(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
        assertThat(repository.findRecent(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
    }

}