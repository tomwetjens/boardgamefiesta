package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum City {
    KANSAS_CITY(true, 0),
    SAN_FRANCISCO(false, 18);

    @Getter
    private boolean multipleDeliveries;
    @Getter
    private int value;
}
