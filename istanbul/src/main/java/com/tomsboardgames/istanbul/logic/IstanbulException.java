package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.InGameException;
import com.tomsboardgames.istanbul.Istanbul;

public class IstanbulException extends InGameException {

    IstanbulException(IstanbulError error) {
        super(Istanbul.ID, error.name());
    }

}
