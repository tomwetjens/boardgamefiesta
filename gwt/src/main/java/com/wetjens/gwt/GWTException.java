package com.wetjens.gwt;

import com.wetjens.gwt.api.InGameException;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GWTException extends InGameException {

    public GWTException(GWTError error, Object... params) {
        super(error.toString(), params);
    }
}
