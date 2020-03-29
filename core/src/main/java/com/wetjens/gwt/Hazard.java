package com.wetjens.gwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class Hazard {
    @NonNull
    private final HazardType type;

    @NonNull
    private final Fee fee;

    private final int points;

}
