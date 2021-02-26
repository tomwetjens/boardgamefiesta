package com.boardgamefiesta.domain.exception;

import lombok.Getter;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String errorCode) {
        this(errorCode, errorCode);
    }

    protected DomainException(String errorCode, String message) {
        super(message);

        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, Throwable cause) {
        this(errorCode, errorCode, cause);
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);

        this.errorCode = errorCode;
    }

    @Getter
    private final String errorCode;

}
