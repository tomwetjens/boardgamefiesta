package com.wetjens.gwt;

import lombok.Getter;

@Getter
public class GWTException extends RuntimeException {

    GWTError error;
    Object[] params;

    public GWTException(GWTError error, Object... params) {
        this.error = error;
        this.params = params;
    }
}
