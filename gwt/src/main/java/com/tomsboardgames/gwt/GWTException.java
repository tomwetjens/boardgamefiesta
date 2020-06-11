package com.tomsboardgames.gwt;

import com.tomsboardgames.api.InGameException;
import lombok.Getter;

@Getter
public class GWTException extends InGameException {

    public GWTException(GWTError error) {
        super(GWT.ID, error.toString());
    }
}
