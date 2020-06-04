package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.InGameException;

public class IstanbulException extends InGameException {

    IstanbulException(IstanbulError error, Object... params) {
        super(error.name(), params);
    }

}
