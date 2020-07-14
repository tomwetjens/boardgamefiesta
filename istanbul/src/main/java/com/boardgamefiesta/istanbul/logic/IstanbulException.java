package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.InGameException;
import com.boardgamefiesta.istanbul.Istanbul;

public class IstanbulException extends InGameException {

    IstanbulException(IstanbulError error) {
        super(Istanbul.ID, error.name());
    }

}
