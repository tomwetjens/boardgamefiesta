package com.wetjens.gwt;

import java.util.HashSet;
import java.util.Set;

public final class SingleOrDoubleAuxiliaryAction extends Location.BuildingLocation.SingleAuxiliaryAction {

    @Override
    protected Set<Class<? extends Action>> unlockedActions(PlayerState playerState) {
        Set<Class<? extends Action>> actions = new HashSet<>(super.unlockedActions(playerState));

        if (playerState.hasAllUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(Gain2Dollars.class);
        }
        if (playerState.hasAllUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(DrawCardThenDiscardCard.Draw2CardsThenDiscard2Cards.class);
        }
        if (playerState.hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates.class);
        }
        if (playerState.hasAllUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(Pay2DollarsToMoveEngine2SpacesForward.class);
        }
        if (playerState.hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            actions.add(MoveEngine2SpacesBackwardsToRemove2Cards.class);
        }

        return actions;
    }

    public static final class Gain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static final class Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates extends Action {

        private final RailroadTrack.Space to;

        public Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(2);
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2);
            // TODO Check if gaining certificates AFTER possible immediate actions from railroad track is OK
            game.currentPlayerState().gainCertificates(2);
            return immediateActions;
        }
    }

    public static final class Pay2DollarsToMoveEngine2SpacesForward extends Action {

        private final RailroadTrack.Space to;

        public Pay2DollarsToMoveEngine2SpacesForward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(2);
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 2, 2);
        }
    }

    public static final class MoveEngine2SpacesBackwardsToRemove2Cards extends Action {

        private final RailroadTrack.Space to;

        public MoveEngine2SpacesBackwardsToRemove2Cards(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2)
                    .andThen(PossibleAction.of(Remove2Cards.class));
        }
    }
}
