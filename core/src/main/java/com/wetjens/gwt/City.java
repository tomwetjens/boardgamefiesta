package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum City {
    KANSAS_CITY(true, 0, 1),
    TOPEKA(false, 2,2),
    WICHITA(false, 4,4),
    COLORADO_SPRINGS(false, 6,5),
    ALBEQUERQUE(false,8,6),
    SAN_DIEGO(false, 10,7),
    SAN_FRANCISCO(false, 18,8);

    private boolean multipleDeliveries;
    private int value;
    private int signals;
}
