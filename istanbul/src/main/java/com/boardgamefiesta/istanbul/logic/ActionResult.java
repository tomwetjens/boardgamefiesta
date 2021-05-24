package com.boardgamefiesta.istanbul.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionResult {
    List<PossibleAction> followUpActions;
    List<PossibleAction> finalActions;
    boolean undo;

    public static ActionResult none(boolean canUndo) {
        return new ActionResult(Collections.emptyList(), Collections.emptyList(), canUndo);
    }

    public static ActionResult followUp(PossibleAction possibleAction, boolean canUndo) {
        return new ActionResult(List.of(possibleAction), Collections.emptyList(), canUndo);
    }

    public ActionResult andThen(ActionResult actionResult) {
        return new ActionResult(
                Stream.concat(followUpActions.stream(), actionResult.followUpActions.stream())
                        .collect(Collectors.toList()),
                Stream.concat(finalActions.stream(), actionResult.finalActions.stream())
                        .collect(Collectors.toList()),
                undo && actionResult.canUndo());
    }

    public boolean canUndo() {
        return undo;
    }
}
