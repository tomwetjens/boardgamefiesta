package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.user.User;
import org.assertj.core.api.Assertions;
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

        Assertions.assertThat(users).isNotEmpty();
        Assertions.assertThat(users.get(0).getCognitoUsername()).isEqualTo("tom");
    }

    @Test
    void findByEmail() {
        User user = userDynamoDbRepository.findByEmail("tomwetjens@gmail.com").get();

        Assertions.assertThat(user.getCognitoUsername()).isEqualTo("tom");
    }
}
