package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.api.domain.InGameException;

public class PowerGridException extends InGameException {
    PowerGridException(PowerGridError powerGridError) {
        super(powerGridError.name());
    }
}
