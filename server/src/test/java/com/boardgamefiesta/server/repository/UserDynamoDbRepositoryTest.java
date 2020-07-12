package com.boardgamefiesta.server.repository;

import com.boardgamefiesta.server.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class UserDynamoDbRepositoryTest {

    @Mock
    DynamoDbConfiguration config;

    UserDynamoDbRepository userDynamoDbRepository;

    @BeforeEach
    void setUp() {
        when(config.getTableSuffix()).thenReturn(Optional.of("-test"));

        userDynamoDbRepository = new UserDynamoDbRepository(DynamoDbClient.create(), config);
    }


    @Test
    void findByUsernameStartsWith() {
        List<User> users = userDynamoDbRepository.findByUsernameStartsWith("tom")
                .collect(Collectors.toList());

        assertThat(users).isNotEmpty();
        assertThat(users.get(0).getUsername()).isEqualTo("tom");
    }

    @Test
    void findByEmail() {
        User user = userDynamoDbRepository.findByEmail("tomwetjens@gmail.com").get();

        assertThat(user.getUsername()).isEqualTo("tom");
    }
}
