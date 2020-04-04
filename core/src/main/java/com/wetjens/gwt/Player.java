package com.wetjens.gwt;

import lombok.Value;

import java.io.Serializable;

@Value
public class Player implements Serializable {

    String name;
    Color color;

    public enum Color {
        YELLOW,
        RED,
        BLUE,
        WHITE;
    }
}
