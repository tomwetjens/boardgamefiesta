package com.wetjens.gwt;

public abstract class NeutralBuilding extends Building {

    protected NeutralBuilding() {
        super(Hand.NONE);
    }

    public static final class A extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1Guernsey.class, Action.HireWorker.class, Action.HireSecondWorker.class);
        }
    }

    public static final class B extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1DutchBeltToGain2Dollars.class, Action.PlaceBuilding.class);
        }
    }

    public static final class C extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(Action.GainCertificate.class, Action.GainObjectiveCard.class),
                    Action.MoveEngineForward.class);
        }
    }

    public static final class D extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithIndians.class, Action.Pay2DollarsToMoveEngine2Forward.class),
                    Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class E extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1BlackAngusToGain2Dollars.class, Action.BuyCattle.class);
        }
    }

    public static final class F extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.DiscardPairToGain4Dollars.class, Action.RemoveHazard.class);
        }
    }

    public static final class G extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.MoveEngineForward.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }
}
