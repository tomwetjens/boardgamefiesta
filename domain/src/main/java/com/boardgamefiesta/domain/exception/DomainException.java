package com.boardgamefiesta.domain.exception;

import lombok.Getter;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String errorCode) {
        super(errorCode);

        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, Throwable cause) {
        super(errorCode, cause);

        this.errorCode = errorCode;
    }

    @Getter
    private final String errorCode;

}
