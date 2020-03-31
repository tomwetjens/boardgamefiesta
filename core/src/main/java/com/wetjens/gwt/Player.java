package com.wetjens.gwt;

import lombok.Value;

@Value
public class Player {

    String name;
    Color color;

    public enum Color {
        YELLOW,
        RED,
        BLUE,
        WHITE;
    }
}
