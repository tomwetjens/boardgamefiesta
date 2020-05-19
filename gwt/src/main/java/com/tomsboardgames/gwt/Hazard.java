package com.tomsboardgames.gwt;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@ToString
public class Hazard implements Serializable {

    private static final long serialVersionUID = 1L;

    @NonNull
    private final HazardType type;

    @NonNull
    private final Hand hand;

    private final int points;
}
