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

package com.boardgamefiesta.domain;

import com.boardgamefiesta.domain.exception.DomainException;

import java.util.stream.Stream;

public interface Repository extends DomainService {

    interface Page<T> {
        Stream<T> stream();

        String getContinuationToken();
    }

    abstract class DuplicateException extends AggregateRoot.InvalidCommandException {
        protected DuplicateException(String errorCode) {
            super(errorCode);
        }

        protected DuplicateException(String errorCode, String message) {
            super(errorCode, message);
        }
    }

    final class NotFoundException extends DomainException {
        public NotFoundException() {
            super("NOT_FOUND");
        }
    }

    final class ConcurrentModificationException extends DomainException {
        public ConcurrentModificationException(Throwable cause) {
            super("CONCURRENT_MODIFICATION", cause);
        }
    }

}
