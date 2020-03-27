package com.wetjens.gwt;

public abstract class Action {

    public abstract ImmediateActions perform(Game game);

    /**
     * Indicates whether this action can be performed at any time during a player's turn.
     *
     * @return
     */
    public boolean isArbitrary() {
        return false;
    }
}
