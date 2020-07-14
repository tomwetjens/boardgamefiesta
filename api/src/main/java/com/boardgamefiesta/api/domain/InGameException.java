package com.boardgamefiesta.api.domain;

import lombok.Getter;

public class InGameException extends RuntimeException {

    @Getter
    private final String errorCode;

    protected InGameException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

}
