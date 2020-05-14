package com.wetjens.gwt.api;

import lombok.Getter;

public class InGameException extends RuntimeException {

    @Getter
    private final String error;

    @Getter
    private final transient Object[] params;

    protected InGameException(String error, Object... params) {
        super(error);
        this.error = error;
        this.params = params;
    }

}
