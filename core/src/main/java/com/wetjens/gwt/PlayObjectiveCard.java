package com.wetjens.gwt;

public class PlayObjectiveCard extends Action {

    private final ObjectiveCard objectiveCard;

    public PlayObjectiveCard(ObjectiveCard objectiveCard) {
        this.objectiveCard = objectiveCard;
    }

    @Override
    public boolean canPlayAnyTime() {
        return true;
    }

    @Override
    public ImmediateActions perform(Game game) {
        return game.currentPlayerState().playObjectiveCard(objectiveCard);
    }
}
