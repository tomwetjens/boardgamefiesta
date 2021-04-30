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

        repository = new FriendDynamoDbRepositoryV2(dynamoDbClient, config);
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