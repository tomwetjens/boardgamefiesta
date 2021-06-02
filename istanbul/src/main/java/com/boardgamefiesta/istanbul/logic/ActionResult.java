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
    boolean undo;
    boolean immediate;

    public static ActionResult none(boolean canUndo) {
        return new ActionResult(Collections.emptyList(), canUndo, false);
    }

    public static ActionResult immediate(PossibleAction possibleAction, boolean canUndo) {
        return new ActionResult(List.of(possibleAction), canUndo, true);
    }

    public static ActionResult followUp(PossibleAction possibleAction, boolean canUndo) {
        return new ActionResult(List.of(possibleAction), canUndo, false);
    }

    public ActionResult andThen(ActionResult actionResult) {
        return new ActionResult(
                Stream.concat(followUpActions.stream(), actionResult.followUpActions.stream())
                        .collect(Collectors.toList()),
                undo && actionResult.canUndo(),
                immediate || actionResult.isImmediate());
    }

    public boolean canUndo() {
        return undo;
    }
}
