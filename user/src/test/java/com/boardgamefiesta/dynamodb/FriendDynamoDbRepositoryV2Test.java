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

import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Disabled // Disabled because 'too many requests' pulling Docker image on AWS CodeBuild
@ExtendWith(MockitoExtension.class)
class FriendDynamoDbRepositoryV2Test extends BaseDynamoDbRepositoryTest {

    static final User.Id USER_ID_A = User.Id.of(UUID.randomUUID().toString());
    static final User.Id USER_ID_B = User.Id.of(UUID.randomUUID().toString());
    static final User.Id USER_ID_C = User.Id.of(UUID.randomUUID().toString());

    @Mock
    CDI<Object> cdi;
    @Mock
    BeanManager beanManager;

    FriendDynamoDbRepositoryV2 repository;

    @BeforeEach
    void setUp() {
        when(cdi.getBeanManager()).thenReturn(beanManager);
        CDI.setCDIProvider(() -> cdi);

        repository = new FriendDynamoDbRepositoryV2(BaseDynamoDbRepositoryTest.dynamoDbClient, BaseDynamoDbRepositoryTest.config);
    }

    @Test
    void findById() {
        repository.add(Friend.start(USER_ID_A, USER_ID_B));
        repository.add(Friend.start(USER_ID_A, USER_ID_C));
        repository.add(Friend.start(USER_ID_B, USER_ID_C));

        assertThat(repository.findById(Friend.Id.of(USER_ID_A, USER_ID_B))).isNotEmpty();
    }

    @Test
    void findById_NotFound() {
        repository.add(Friend.start(USER_ID_A, USER_ID_B));
        repository.add(Friend.start(USER_ID_A, USER_ID_C));
        repository.add(Friend.start(USER_ID_B, USER_ID_C));

        assertThat(repository.findById(Friend.Id.of(USER_ID_B, USER_ID_A))).isEmpty();
    }

    @Test
    void findByUserId() {
        repository.add(Friend.start(USER_ID_A, USER_ID_B));
        repository.add(Friend.start(USER_ID_A, USER_ID_C));
        repository.add(Friend.start(USER_ID_B, USER_ID_C));

        var friends = repository.findByUserId(USER_ID_A, 10).collect(Collectors.toList());

        assertThat(friends).hasSize(2);
    }
}