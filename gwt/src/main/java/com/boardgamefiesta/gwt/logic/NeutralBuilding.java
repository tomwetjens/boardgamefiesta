package com.boardgamefiesta.gwt.logic;

import java.util.stream.Stream;

public abstract class NeutralBuilding extends Building {

    NeutralBuilding(String name) {
        super(name, Hand.NONE);
    }

    public static NeutralBuilding forName(String name) {
        switch (name) {
            case "A":
                return new NeutralBuilding.A();
            case "B":
                return new NeutralBuilding.B();
            case "C":
                return new NeutralBuilding.C();
            case "D":
                return new NeutralBuilding.D();
            case "E":
                return new NeutralBuilding.E();
            case "F":
                return new NeutralBuilding.F();
            case "G":
                return new NeutralBuilding.G();
            default:
                throw new IllegalArgumentException("Unsupported neutral building: " + name);
        }
    }

    public static final class A extends NeutralBuilding {

        A() {
            super("A");
        }

        @Override
        public PossibleAction activate(Game game) {
            return PossibleAction.any(Action.Discard1Guernsey.class, Action.HireWorker.class, Action.HireWorkerPlus2.class);
        }
    }

    public static final class B extends NeutralBuilding {

        B() {
            super("B");
        }

        @Override
        public PossibleAction activate(Game game) {
            return PossibleAction.any(Action.Discard1DutchBeltToGain2Dollars.class, Action.PlaceBuilding.class);
        }
    }

    public static final class C extends NeutralBuilding {

        C() {
            super("C");
        }

        @Override
        public PossibleAction activate(Game game) {
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
        public PossibleAction activate(Game game) {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithTribes.class, Action.Pay2DollarsToMoveEngine2Forward.class),
                    Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class E extends NeutralBuilding {

        E() {
            super("E");
        }

        @Override
        public PossibleAction activate(Game game) {
            game.currentPlayerState().resetUsedCowboys();
            return PossibleAction.any(Stream.of(
                    PossibleAction.optional(Action.Discard1BlackAngusToGain2Dollars.class),
                    PossibleAction.repeat(0, game.currentPlayerState().getNumberOfCowboys(), Action.BuyCattle.class),
                    PossibleAction.repeat(0, game.currentPlayerState().getNumberOfCowboys(), Action.Draw2CattleCards.class)));
        }

    }

    public static final class F extends NeutralBuilding {

        F() {
            super("F");
        }

        @Override
        public PossibleAction activate(Game game) {
            return PossibleAction.any(Action.DiscardPairToGain4Dollars.class, Action.RemoveHazard.class);
        }
    }

    public static final class G extends NeutralBuilding {

        G() {
            super("G");
        }

        @Override
        public PossibleAction activate(Game game) {
            return PossibleAction.any(Action.MoveEngineForward.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }
}
