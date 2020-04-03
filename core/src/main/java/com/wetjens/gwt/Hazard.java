package com.wetjens.gwt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@ToString
public class Hazard {
    @NonNull
    private final HazardType type;

    @NonNull
    private final Hand hand;

    private final int points;
}
