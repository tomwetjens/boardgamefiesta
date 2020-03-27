package com.wetjens.gwt;

public class MoveEngineForward extends Action {

    private final RailroadTrack.Space to;

    public MoveEngineForward(RailroadTrack.Space to) {
        this.to = to;
    }

    @Override
    public ImmediateActions perform(Game game) {
        return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, game.currentPlayerState().getNumberOfEngineers());
    }
}
