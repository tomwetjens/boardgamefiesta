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
