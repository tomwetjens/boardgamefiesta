package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.InGameException;
import lombok.Getter;

@Getter
public class GWTException extends InGameException {

    public GWTException(GWTError error) {
        super(error.name());
    }
}
