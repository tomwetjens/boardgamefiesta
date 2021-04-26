package com.boardgamefiesta.dynamodb;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkedTest {

    @Test
    void collect() {
        var chunks = IntStream.range(0, 8)
                .boxed()
                .collect(Chunked.chunked(3));

        assertThat(chunks).containsExactly(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7));
    }

    @Test
    void stream() {
        var chunks = Chunked.stream(IntStream.range(0, 8).boxed(), 3)
                .map(chunk -> chunk.collect(Collectors.toList()))
                .collect(Collectors.toList());

        assertThat(chunks).containsExactly(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7));
    }

    @Test
    void stream2() {
        assertThat(Chunked.stream(IntStream.range(0, 8).boxed(), 3)
                .map(Stream::count)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())))
                .containsKeys(3L, 2L)
                .containsEntry(3L, 2L)
                .containsEntry(2L, 1L);
    }

    @Test
    void stream3() {
        assertThat(Chunked.stream(IntStream.range(0, 8).boxed(), 3)
                // Do not consume chunk itself
                .count()).isEqualTo(3);
    }

    @Test
    void stream4() {
        assertThat(Chunked.stream(IntStream.range(0, 9999999).boxed(), 13)
                // Do not consume chunk itself
                .count()).isEqualTo(769231L);
    }

    @Test
    void stream5() {
        assertThat(Chunked.stream(IntStream.range(0, 9999999).boxed(), 13)
                .map(Stream::count)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())))
                .containsKeys(13L, 9L)
                .containsEntry(13L, 769230L)
                .containsEntry(9L, 1L);
    }
}