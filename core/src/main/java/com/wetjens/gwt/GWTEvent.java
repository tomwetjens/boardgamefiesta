package com.wetjens.gwt;

import lombok.Value;

import java.util.List;

@Value
public class GWTEvent {

    Player player;
    Type type;
    List<Object> values;

    public enum Type {
        ACTION,
        PAY_DOLLARS,
        MAY_DRAW_CATTLE_CARDS;
    }
}
