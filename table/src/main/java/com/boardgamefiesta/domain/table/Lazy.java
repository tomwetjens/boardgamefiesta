/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.domain.table;

import lombok.Getter;

import java.util.function.Supplier;

public abstract class Lazy<T> {

    private Lazy() {
    }

    public abstract T get();

    public abstract boolean isResolved();

    public static <T> Lazy<T> of(T value) {
        return new Just<>(value);
    }

    public static <T> Lazy<T> defer(Supplier<T> supplier) {
        return new Deferred<>(supplier);
    }

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
        @Getter
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

    }

}
