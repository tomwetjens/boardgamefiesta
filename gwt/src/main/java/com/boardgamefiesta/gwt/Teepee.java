package com.boardgamefiesta.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Teepee {
    BLUE(Hand.BLACK),
    GREEN(Hand.GREEN);

    Hand hand;
}
