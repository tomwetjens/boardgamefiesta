package com.wetjens.gwt;

public abstract class NeutralBuilding extends Building {

    protected NeutralBuilding() {
        super(Fee.NONE);
    }

    public static final class A extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.DiscardOneGuernsey.class, Action.HireWorker.class, Action.HireSecondWorker.class);
        }

    }

    public static final class B extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.DiscardOneDutchBelt.class, Action.PlaceBuilding.class);
        }

    }

    public static final class C extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(C.GainCertificate.class, Action.GainObjectiveCard.class),
                    Action.MoveEngineForward.class);
        }

        public static class GainCertificate extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().gainCertificates(1);
                return ImmediateActions.none();
            }
        }
    }

    public static final class D extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithIndians.class, Action.Pay2DollarsToMoveEngine2SpacesForward.class),
                    Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class E extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Discard1BlackAngusToGain2Dollars.class, Action.BuyCattle.class);
        }

        public static final class Discard1BlackAngusToGain2Dollars extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().discardCattleCards(CattleType.BLACK_ANGUS, 1);
                game.currentPlayerState().gainDollars(2);
                return ImmediateActions.none();
            }
        }
    }

    public static final class F extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(DiscardPairToGain4Dollars.class, RemoveHazard.class);
        }

        public static final class DiscardPairToGain4Dollars extends Action {

            private final CattleType type;

            public DiscardPairToGain4Dollars(CattleType type) {
                this.type = type;
            }

            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().discardCattleCards(type, 2);
                game.currentPlayerState().gainDollars(4);
                return ImmediateActions.none();
            }
        }

        public static final class RemoveHazard extends Action {

            private final HazardType type;

            public RemoveHazard(HazardType type) {
                this.type = type;
            }

            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().payDollars(7);
                game.getTrail().removeHazard(type);
                return ImmediateActions.none();
            }
        }
    }

    public static final class G extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.MoveEngineForward.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }
}
