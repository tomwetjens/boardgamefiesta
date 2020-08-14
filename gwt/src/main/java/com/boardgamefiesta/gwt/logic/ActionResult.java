package com.boardgamefiesta.gwt.logic;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class ActionResult {
    List<PossibleAction> newActions;
    ImmediateActions immediateActions;
    boolean canUndo;

    static ActionResult undoAllowed(PossibleAction followUpAction) {
        return new ActionResult(Collections.singletonList(followUpAction), ImmediateActions.none(), true);
    }

    static ActionResult undoAllowed(ImmediateActions immediateActions) {
        return new ActionResult(Collections.emptyList(), immediateActions, true);
    }

    static ActionResult undoNotAllowed(ImmediateActions immediateActions) {
        return new ActionResult(Collections.emptyList(), immediateActions, false);
    }

    public boolean canUndo() {
        return canUndo;
    }
}
