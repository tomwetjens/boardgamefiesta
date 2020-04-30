package com.wetjens.gwt;

import lombok.Getter;

@Getter
public class GWTException extends RuntimeException {

    private final GWTError error;
    private final transient Object[] params;

    public GWTException(GWTError error, Object... params) {
        super(error.toString());

        this.error = error;
        this.params = params;
    }
}
