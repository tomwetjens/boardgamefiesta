package com.boardgamefiesta.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

final class Chunked {

    /**
     * Collector for Java Stream API that divides the stream into chunks.
     *
     * <p>
     * Usage example:
     * <code><pre>
     *      stream
     *        .collect(chunked(25))
     *        .forEach(chunk -> {
     *           // ...
     *        });
     * </pre></code>
     * </p>
     */
    static <T> Collector<T, List<List<T>>, List<List<T>>> chunked(int chunkSize) {
        return Collector.of(ArrayList::new, (chunks, elem) -> {
            List<T> chunk;
            if (chunks.isEmpty() || chunks.get(chunks.size() - 1).size() >= chunkSize) {
                chunk = new ArrayList<>(chunkSize);
                chunks.add(chunk);
            } else {
                chunk = chunks.get(chunks.size() - 1);
            }

            chunk.add(elem);
        }, (a1, a2) -> {
            throw new UnsupportedOperationException();
        });
    }

}
