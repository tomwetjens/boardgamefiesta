package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class Hazard {
    @NonNull
    private final HazardType type;

    @NonNull
    private final Hand hand;

    private final int points;
}
