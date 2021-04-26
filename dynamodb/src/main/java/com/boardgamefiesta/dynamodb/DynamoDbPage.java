package com.boardgamefiesta.dynamodb;

import com.boardgamefiesta.domain.Repository;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Value
class DynamoDbPage<T> implements Repository.Page<T> {
    List<T> items;
    String continuationToken;

    @Override
    public Stream<T> stream() {
        return items.stream();
    }
}
