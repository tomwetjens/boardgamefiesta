package com.boardgamefiesta.gwt;

import com.boardgamefiesta.api.InGameException;
import lombok.Getter;

@Getter
public class GWTException extends InGameException {

    public GWTException(GWTError error) {
        super(GWT.ID, error.toString());
    }
}
