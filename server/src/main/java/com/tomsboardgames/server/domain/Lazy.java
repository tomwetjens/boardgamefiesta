package com.boardgamefiesta.server.domain;

import java.util.function.Supplier;

public abstract class Lazy<T> {

    private Lazy() {
    }

    public abstract T get();

    public static <T> Lazy<T> of(T value) {
        return new Just<>(value);
    }

    public static <T> Lazy<T> defer(Supplier<T> supplier) {
        return new Deferred<>(supplier);
    }

    public abstract boolean isResolved();

    private static final class Just<T> extends Lazy<T> {
        private final T value;

        private Just(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public boolean isResolved() {
            return true;
        }
    }

    private static final class Deferred<T> extends Lazy<T> {
        private final Supplier<T> supplier;
        private T value;
        private boolean resolved;

        public Deferred(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (!resolved) {
                value = supplier.get();
                resolved = true;
            }
            return value;
        }

        @Override
        public boolean isResolved() {
            return resolved;
        }
    }

}
