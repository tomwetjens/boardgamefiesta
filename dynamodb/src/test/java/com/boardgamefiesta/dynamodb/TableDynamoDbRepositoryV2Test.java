package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.game.Games;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.gwt.GWTProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@Disabled // Disabled because 'too many requests' pulling Docker image on AWS CodeBuild
@ExtendWith(MockitoExtension.class)
class TableDynamoDbRepositoryV2Test extends BaseDynamoDbRepositoryTest {

    static final Game.Id GAME_ID = Game.Id.of(GWTProvider.ID);

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
        super.setUp();

        when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);

        lenient().when(userA.getId()).thenReturn(User.Id.of(UUID.randomUUID().toString()));
        lenient().when(userB.getId()).thenReturn(User.Id.of(UUID.randomUUID().toString()));

        repository = new TableDynamoDbRepositoryV2(games, dynamoDbClient, config);
    }

    @Nested
    class Add {
        @Test
        void created() {
            var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
            repository.add(table);

            var actual = repository.findById(table.getId()).orElseThrow();
            assertThat(actual).isEqualToComparingOnlyGivenFields(table, "id", "type", "mode", "options", "created", "started", "updated", "ended", "status");
            assertThat(actual.getPlayers()).hasSize(1);
            assertThat(actual.getLog().stream().collect(Collectors.toList())).hasSize(1);
            assertThat(actual.getCurrentState().get()).isEmpty();

            assertThat(repository.findActive(userA.getId())).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findAll(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findAll(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findEnded(GAME_ID, 10, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false)).extracting(Table::getId).doesNotContain(table.getId());
        }
    }

    @Nested
    class Update {

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
            assertThat(repository.findAll(userB.getId(), 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findAll(userB.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findEnded(GAME_ID, 10, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false)).extracting(Table::getId).doesNotContain(table.getId());
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
            assertThat(repository.findAll(userB.getId(), 10)).extracting(Table::getId).doesNotContain(table.getId());
            assertThat(repository.findAll(userB.getId(), GAME_ID, 10)).extracting(Table::getId).doesNotContain(table.getId());
            assertThat(repository.findEnded(GAME_ID, 10, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false)).extracting(Table::getId).doesNotContain(table.getId());
        }

        @Test
        void abandoned() {
            var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
            repository.add(table);

            table = repository.findById(table.getId()).orElseThrow();
            table.abandon();
            repository.update(table);

            assertThat(repository.findActive(userA.getId())).extracting(Table::getId).doesNotContain(table.getId());
            assertThat(repository.findAll(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findAll(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findEnded(GAME_ID, 10, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false)).extracting(Table::getId).doesNotContain(table.getId());
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
            assertThat(repository.findAll(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findAll(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findEnded(GAME_ID, 10, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false)).extracting(Table::getId).doesNotContain(table.getId());
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
            assertThat(repository.findAll(userA.getId(), 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findAll(userA.getId(), GAME_ID, 10)).extracting(Table::getId).contains(table.getId());
            assertThat(repository.findEnded(GAME_ID, 10, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false)).extracting(Table::getId).contains(table.getId());
        }

        // TODO Abandoned tables should only be visible in user's recent tables
    }

    @Nested
    class FindEndedByGameId {
        @Test
        void order() {
            repository.add(table().status(Table.Status.ENDED).ended(Instant.parse("2021-04-21T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.ENDED).ended(Instant.parse("2021-04-25T00:00:00.000Z")).build());

            var tables = repository.findEnded(GAME_ID, 6, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false).collect(Collectors.toList());

            assertThat(tables).hasSize(2);
            assertThat(tables)
                    .extracting(Table::getEnded)
                    .containsExactly(
                            Instant.parse("2021-04-25T00:00:00.000Z"),
                            Instant.parse("2021-04-21T00:00:00.000Z"));
        }

        @Test
        void many() {
            IntStream.range(0, 50).forEach(i ->
                    repository.put(table().status(Table.Status.ENDED).ended(Instant.now()).build()));

            var tables = repository.findEnded(GAME_ID, 100, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false).collect(Collectors.toList());

            assertThat(tables).hasSize(50);
        }

        @Test
        void pagination() {
            IntStream.range(0, 45).forEach(i ->
                    repository.put(table()
                            .status(Table.Status.ENDED)
                            .ended(Instant.ofEpochSecond(i))
                            .build()));

            var tables1 = repository.findEnded(GAME_ID, 20, Tables.MIN_TIMESTAMP, Tables.MAX_TIMESTAMP, false).collect(Collectors.toList());
            assertThat(tables1).hasSize(20);
            var firstTable1 = tables1.get(0);
            assertThat(firstTable1.getEnded()).isEqualTo(Instant.ofEpochSecond(44));
            var lastTable1 = tables1.get(tables1.size() - 1);
            assertThat(lastTable1.getEnded()).isEqualTo(Instant.ofEpochSecond(25));

            var tableIds1 = tables1.stream().map(Table::getId).collect(Collectors.toList());

            var tables2 = repository.findEnded(GAME_ID, 20, Instant.ofEpochSecond(0), lastTable1.getEnded(), false, lastTable1.getId()).collect(Collectors.toList());
            assertThat(tables2).hasSize(20);
            var firstTable2 = tables2.get(0);
            assertThat(firstTable2.getEnded()).isEqualTo(Instant.ofEpochSecond(24));
            var lastTable2 = tables2.get(tables2.size() - 1);
            assertThat(lastTable2.getEnded()).isEqualTo(Instant.ofEpochSecond(5));

            var tableIds2 = tables2.stream().map(Table::getId).collect(Collectors.toList());
            assertThat(tableIds2).doesNotContainAnyElementsOf(tableIds1);

            var tables3 = repository.findEnded(GAME_ID, 20, Instant.ofEpochSecond(0), lastTable2.getEnded(), false, lastTable2.getId()).collect(Collectors.toList());
            assertThat(tables3).hasSize(5);
            var firstTable3 = tables3.get(0);
            assertThat(firstTable3.getEnded()).isEqualTo(Instant.ofEpochSecond(4));
            var lastTable3 = tables3.get(tables3.size() - 1);
            assertThat(lastTable3.getEnded()).isEqualTo(Instant.ofEpochSecond(0));

            var tableIds3 = tables3.stream().map(Table::getId).collect(Collectors.toList());
            assertThat(tableIds3).doesNotContainAnyElementsOf(tableIds1).doesNotContainAnyElementsOf(tableIds2);
        }
    }

    @Nested
    class FindAllByUserId {
        @Test
        void ordering() {
            repository.add(table().status(Table.Status.ENDED).ended(Instant.parse("2021-04-21T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.NEW).created(Instant.parse("2021-04-22T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.STARTED).started(Instant.parse("2021-04-23T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.NEW).created(Instant.parse("2021-04-24T00:06:00.000Z")).build());
            repository.add(table().status(Table.Status.STARTED).started(Instant.parse("2021-04-21T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.ENDED).ended(Instant.parse("2021-04-25T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.ABANDONED).created(Instant.parse("2021-04-26T00:00:00.000Z")).build());

            var tables = repository.findAll(userA.getId(), 7).collect(Collectors.toList());

            assertThat(tables).extracting(Table::getStatus).containsExactly(
                    Table.Status.NEW,
                    Table.Status.NEW,
                    Table.Status.STARTED,
                    Table.Status.STARTED,
                    Table.Status.ABANDONED,
                    Table.Status.ENDED,
                    Table.Status.ENDED);

            assertThat(tables)
                    .filteredOn(table -> table.getStatus() == Table.Status.NEW)
                    .extracting(Table::getCreated)
                    .containsExactly(
                            Instant.parse("2021-04-24T00:06:00.000Z"),
                            Instant.parse("2021-04-22T00:00:00.000Z"));

            assertThat(tables)
                    .filteredOn(table -> table.getStatus() == Table.Status.STARTED)
                    .extracting(Table::getStarted)
                    .containsExactly(
                            Instant.parse("2021-04-23T00:00:00.000Z"),
                            Instant.parse("2021-04-21T00:00:00.000Z"));

            assertThat(tables)
                    .filteredOn(table -> table.getStatus() == Table.Status.ENDED)
                    .extracting(Table::getEnded)
                    .containsExactly(
                            Instant.parse("2021-04-25T00:00:00.000Z"),
                            Instant.parse("2021-04-21T00:00:00.000Z"));
        }
    }

    @Nested
    class FindAllByUserIdAndGameId {
        @Test
        void ordering() {
            repository.add(table().status(Table.Status.ENDED).ended(Instant.parse("2021-04-21T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.NEW).created(Instant.parse("2021-04-22T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.STARTED).started(Instant.parse("2021-04-23T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.NEW).created(Instant.parse("2021-04-24T00:06:00.000Z")).build());
            repository.add(table().status(Table.Status.STARTED).started(Instant.parse("2021-04-21T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.ENDED).ended(Instant.parse("2021-04-25T00:00:00.000Z")).build());
            repository.add(table().status(Table.Status.ABANDONED).created(Instant.parse("2021-04-26T00:00:00.000Z")).build());

            var tables = repository.findAll(userA.getId(), GAME_ID, 7).collect(Collectors.toList());

            assertThat(tables).extracting(Table::getStatus).containsExactly(
                    Table.Status.NEW,
                    Table.Status.NEW,
                    Table.Status.STARTED,
                    Table.Status.STARTED,
                    Table.Status.ABANDONED,
                    Table.Status.ENDED,
                    Table.Status.ENDED);

            assertThat(tables)
                    .filteredOn(table -> table.getStatus() == Table.Status.NEW)
                    .extracting(Table::getCreated)
                    .containsExactly(
                            Instant.parse("2021-04-24T00:06:00.000Z"),
                            Instant.parse("2021-04-22T00:00:00.000Z"));

            assertThat(tables)
                    .filteredOn(table -> table.getStatus() == Table.Status.STARTED)
                    .extracting(Table::getStarted)
                    .containsExactly(
                            Instant.parse("2021-04-23T00:00:00.000Z"),
                            Instant.parse("2021-04-21T00:00:00.000Z"));

            assertThat(tables)
                    .filteredOn(table -> table.getStatus() == Table.Status.ENDED)
                    .extracting(Table::getEnded)
                    .containsExactly(
                            Instant.parse("2021-04-25T00:00:00.000Z"),
                            Instant.parse("2021-04-21T00:00:00.000Z"));
        }

    }

    private Table.TableBuilder table() {
        var table = Table.create(game, Table.Mode.NORMAL, userA.getId(), new Options(Collections.emptyMap()));
        table.invite(userB);
        return table.toBuilder();
    }
}