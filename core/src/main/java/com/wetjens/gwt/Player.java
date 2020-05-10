package com.wetjens.gwt;

import lombok.Value;

import java.io.Serializable;

@Value
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    String name;
    PlayerColor color;
}
