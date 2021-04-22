package com.boardgamefiesta.dynamodb;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkedTest {

    @Test
    void collect() {
        var chunks = IntStream.range(0, 8)
                .boxed()
                .collect(Chunked.chunked(3));

        assertThat(chunks).containsExactly(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7));
    }
}