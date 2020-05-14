package com.wetjens.gwt;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GWTException extends com.wetjens.gwt.api.InGameException {

    public GWTException(GWTError error, Object... params) {
        super(error.toString(), params);
    }
}
