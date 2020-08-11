package com.boardgamefiesta.gwt.logic;

import lombok.Value;

@Value
public class ActionResult {
    ImmediateActions immediateActions;
    boolean canUndo;

    static ActionResult undoAllowed(ImmediateActions immediateActions) {
        return new ActionResult(immediateActions, true);
    }

    static ActionResult undoNotAllowed(ImmediateActions immediateActions) {
        return new ActionResult(immediateActions, false);
    }

    public boolean canUndo() {
        return canUndo;
    }
}
