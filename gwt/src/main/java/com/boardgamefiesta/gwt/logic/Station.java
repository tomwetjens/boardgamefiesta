package com.boardgamefiesta.gwt.logic;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

// Not @Value because each instance must be unique
@AllArgsConstructor
@Getter
@FieldDefaults(makeFinal = true)
@ToString
public class Station {

    @Getter
    int cost;

    @Getter
    int points;

    @Getter
    @NonNull
    Set<DiscColor> discColors;

}
