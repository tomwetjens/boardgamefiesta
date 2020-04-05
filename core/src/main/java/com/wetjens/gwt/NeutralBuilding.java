package com.wetjens.gwt;

public abstract class NeutralBuilding extends Building {

    private static final long serialVersionUID = 1L;

    NeutralBuilding(String name) {
        super(name, Hand.NONE);
    }

    public static final class A extends NeutralBuilding {

        private static final long serialVersionUID = 1L;

        A() {
            super("A");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1Guernsey.class, Action.HireWorker.class, Action.HireSecondWorker.class);
        }
    }

    public static final class B extends NeutralBuilding {

        B() {
            super("B");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1DutchBeltToGain2Dollars.class, Action.PlaceBuilding.class);
        }
    }

    public static final class C extends NeutralBuilding {

        C() {
            super("C");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(Action.Gain1Certificate.class, Action.TakeObjectiveCard.class),
                    Action.MoveEngineForward.class);
        }
    }

    public static final class D extends NeutralBuilding {

        D() {
            super("D");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithIndians.class, Action.Pay2DollarsToMoveEngine2Forward.class),
                    Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class E extends NeutralBuilding {

        E() {
            super("E");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1BlackAngusToGain2Dollars.class, Action.BuyCattle.class);
        }
    }

    public static final class F extends NeutralBuilding {

        F() {
            super("F");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.DiscardPairToGain4Dollars.class, Action.RemoveHazard.class);
        }
    }

    public static final class G extends NeutralBuilding {

        G() {
            super("G");
        }

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.MoveEngineForward.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }
}
