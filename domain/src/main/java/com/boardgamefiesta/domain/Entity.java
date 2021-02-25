package com.boardgamefiesta.domain;

import com.boardgamefiesta.domain.exception.DomainException;

public interface Entity {

    abstract class NotAllowedException extends DomainException {
        protected NotAllowedException(String errorCode) {
            super(errorCode);
        }
    }

    abstract class InsufficientDataException extends DomainException {
        protected InsufficientDataException(String errorCode) {
            super(errorCode);
        }
    }

    abstract class InvalidCommandException extends DomainException {
        protected InvalidCommandException(String errorCode) {
            super(errorCode);
        }

        protected InvalidCommandException(String errorCode, Throwable cause) {
            super(errorCode, cause);
        }
    }

}
