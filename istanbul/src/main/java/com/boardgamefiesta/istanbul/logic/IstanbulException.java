package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.InGameException;

public class IstanbulException extends InGameException {

    IstanbulException(IstanbulError error) {
        super(error.name());
    }

}
