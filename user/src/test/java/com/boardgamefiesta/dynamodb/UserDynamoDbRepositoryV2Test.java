/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.Repository;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled // Disabled because 'too many requests' pulling Docker image on AWS CodeBuild
class UserDynamoDbRepositoryV2Test extends BaseDynamoDbRepositoryTest {

    static final User.Id USER_ID_A = User.Id.of(UUID.randomUUID().toString());
    static final User.Id USER_ID_B = User.Id.of(UUID.randomUUID().toString());

    UserDynamoDbRepositoryV2 repository;

    @BeforeEach
    void setUp() {
        super.setUp();

        repository = new UserDynamoDbRepositoryV2(BaseDynamoDbRepositoryTest.dynamoDbClient, BaseDynamoDbRepositoryTest.config);
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