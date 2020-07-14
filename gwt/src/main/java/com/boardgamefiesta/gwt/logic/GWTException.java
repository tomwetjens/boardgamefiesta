package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.InGameException;
import com.boardgamefiesta.gwt.GWT;
import lombok.Getter;

@Getter
public class GWTException extends InGameException {

    public GWTException(GWTError error) {
        super(GWT.ID, error.toString());
    }
}
