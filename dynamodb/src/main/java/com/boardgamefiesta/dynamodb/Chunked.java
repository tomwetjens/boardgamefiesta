package com.boardgamefiesta.dynamodb;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Chunked {

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

    public static <T> Stream<Stream<T>> stream(Stream<T> source, int chunkSize) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new ChunkingIterator<T>(source.iterator(), chunkSize), Spliterator.ORDERED), false);
    }

    private static final class ChunkingIterator<T> implements Iterator<Stream<T>> {

        private final Iterator<T> source;
        private final int chunkSize;

        private int chunksCount;
        private int count;

        ChunkingIterator(Iterator<T> source, int chunkSize) {
            this.source = source;
            this.chunkSize = chunkSize;
        }

        @Override
        public boolean hasNext() {
            skipToNextChunk();
            return source.hasNext();
        }

        @Override
        public Stream<T> next() {
            skipToNextChunk();

            if (!source.hasNext()) {
                return Stream.empty();
            }

            count = 0;
            chunksCount++;

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return count < chunkSize && source.hasNext();
                }

                @Override
                public T next() {
                    var value = source.next();
                    count++;
                    return value;
                }
            }, Spliterator.ORDERED), false);
        }

        private void skipToNextChunk() {
            // Skip any that were not consumed in the nested iterator
            if (chunksCount > 0) {
                while (count < chunkSize && source.hasNext()) {
                    source.next();
                    count++;
                }
            }
        }

    }

}
