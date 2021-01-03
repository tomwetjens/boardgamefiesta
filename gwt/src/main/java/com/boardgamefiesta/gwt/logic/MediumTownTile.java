package com.boardgamefiesta.gwt.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum MediumTownTile {

    GAIN_5_DOLLARS_OR_TAKE_CATTLE_CARD(PossibleAction.choice(
            Action.Gain5Dollars.class,
            Action.TakeBreedingValue3CattleCard.class)),
    HIRE_WORKER_PLUS_2(PossibleAction.optional(Action.HireWorkerMinus2.class)),
    REMOVE_2_CARDS(PossibleAction.repeat(0, 2, Action.RemoveCard.class)),
    MOVE_ENGINE_3_FORWARD(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class)),
    PLACE_BUILDING_FOR_FREE(PossibleAction.optional(Action.PlaceBuildingForFree.class));

    private final PossibleAction possibleAction;

    static Queue<MediumTownTile> shuffledPile(@NonNull Random random) {
        var list = Arrays.stream(values())
                .flatMap(tile -> IntStream.range(0, 2).mapToObj(i -> tile))
                .collect(Collectors.toList());
        Collections.shuffle(list, random);
        return new LinkedList<>(list);
    }

    ImmediateActions activate(Game game) {
        return ImmediateActions.of(possibleAction.clone());
    }
}
