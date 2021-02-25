package com.boardgamefiesta.domain;

import com.boardgamefiesta.domain.exception.DomainException;

public interface Repository extends DomainService {

    abstract class DuplicateException extends AggregateRoot.InvalidCommandException {
        protected DuplicateException(String errorCode) {
            super(errorCode);
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
