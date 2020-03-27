package com.wetjens.gwt;

public final class GainObjectiveCard extends Action {

    @Override
    public ImmediateActions perform(Game game) {
        ObjectiveCard objectiveCard = game.takeObjectiveCard();

        game.currentPlayerState().gainCard(objectiveCard);

        return ImmediateActions.none();
    }

}
