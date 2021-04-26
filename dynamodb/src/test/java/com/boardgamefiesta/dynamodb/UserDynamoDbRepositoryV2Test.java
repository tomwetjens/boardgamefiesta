package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDynamoDbRepositoryV2Test extends BaseDynamoDbRepositoryTest {

    static final User.Id USER_ID_A = User.Id.of(UUID.randomUUID().toString());
    static final User.Id USER_ID_B = User.Id.of(UUID.randomUUID().toString());

    UserDynamoDbRepositoryV2 repository;

    @BeforeEach
    void setUp() {
        super.setUp();

        repository = new UserDynamoDbRepositoryV2(dynamoDbClient, config);
    }

    @Test
    void findById() {
        repository.add(User.builder().id(USER_ID_A)
                .username("wEtGoS")
                .cognitoUsername("cognitoUser1")
                .email("wetgos@boardgamefiesta.com")
                .build());

        repository.add(User.builder().id(USER_ID_B)
                .username("sJeRoNnEkE")
                .cognitoUsername("cognitoUser2")
                .email("sjeronneke@boardgamefiesta.com")
                .build());

        var user = repository.findById(USER_ID_A).get();
        assertThat(user.getId()).isEqualTo(USER_ID_A);
    }

    @Test
    void update() {
        repository.add(User.builder().id(USER_ID_A)
                .username("wEtGoS")
                .cognitoUsername("cognitoUser1")
                .email("wetgos@boardgamefiesta.com")
                .build());

        var user = repository.findById(USER_ID_A).get();

        repository.update(user);

        user = repository.findById(USER_ID_A).get();
        assertThat(user.getVersion()).isEqualTo(2);
    }

    @Test
    void concurrentModification() {
        repository.add(User.builder().id(USER_ID_A)
                .username("wEtGoS")
                .cognitoUsername("cognitoUser1")
                .email("wetgos@boardgamefiesta.com")
                .build());

        var user = repository.findById(USER_ID_A).get();

        repository.update(user);
        assertThatThrownBy(() -> repository.update(user)).isInstanceOf(Repository.ConcurrentModificationException.class);
    }

    @Test
    void findByUsername() {
        repository.add(User.builder().id(USER_ID_A)
                .username("wEtGoS")
                .cognitoUsername("cognitoUser1")
                .email("wetgos@boardgamefiesta.com")
                .build());

        repository.add(User.builder().id(USER_ID_B)
                .username("sJeRoNnEkE")
                .cognitoUsername("cognitoUser2")
                .email("sjeronneke@boardgamefiesta.com")
                .build());

        var user = repository.findByUsername("wetgos").get();
        assertThat(user.getId()).isEqualTo(USER_ID_A);
    }

    @Test
    void findByEmail() {
        repository.add(User.builder().id(USER_ID_A)
                .username("wEtGoS")
                .cognitoUsername("cognitoUser1")
                .email("wetgos@boardgamefiesta.com")
                .build());

        repository.add(User.builder().id(USER_ID_B)
                .username("sJeRoNnEkE")
                .cognitoUsername("cognitoUser2")
                .email("sjeronneke@boardgamefiesta.com")
                .build());

        var user = repository.findByEmail("wetgos@boardgamefiesta.com").get();
        assertThat(user.getId()).isEqualTo(USER_ID_A);
    }

    @Test
    void findByUsernameStartsWith() {
        repository.add(User.builder().id(USER_ID_A)
                .username("wEtGoS")
                .cognitoUsername("cognitoUser1")
                .email("wetgos@boardgamefiesta.com")
                .build());

        repository.add(User.builder().id(USER_ID_B)
                .username("wEtJeNs")
                .cognitoUsername("cognitoUser2")
                .email("wetjens@boardgamefiesta.com")
                .build());

        var users = repository.findByUsernameStartsWith("wet", 5).collect(Collectors.toList());

        assertThat(users).hasSize(2);
    }
}