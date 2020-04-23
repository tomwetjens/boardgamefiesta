package com.wetjens.gwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class PlayerBuilding extends Building {

    private static final long serialVersionUID = 1L;

    private final Player player;
    private final int craftsmen;
    private final int points;
    private final int number;
    private final BuildingSet.Side side;

    private PlayerBuilding(int number, BuildingSet.Side side, Player player, Hand hand, int craftsmen, int points) {
        super(number + side.toString().toLowerCase(), hand);

        this.number = number;
        this.side = side;
        this.player = player;
        this.craftsmen = craftsmen;
        this.points = points;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class BuildingSet {

        @NonNull List<Side> sides;

        public static BuildingSet beginner() {
            return new BuildingSet(Stream.generate(() -> Side.A)
                    .limit(10)
                    .collect(Collectors.toList()));
        }

        public static BuildingSet random(@NonNull Random random) {
            return new BuildingSet(Stream.generate(() -> random.nextBoolean() ? Side.A : Side.B)
                    .limit(10)
                    .collect(Collectors.toList()));
        }

        public Set<PlayerBuilding> createPlayerBuildings(@NonNull Player player) {
            return new HashSet<>(Arrays.asList(
                    sides.get(0) == Side.A ? new Building1A(player) : new Building1B(player),
                    sides.get(1) == Side.A ? new Building2A(player) : new Building2B(player),
                    sides.get(2) == Side.A ? new Building3A(player) : new Building3B(player),
                    sides.get(3) == Side.A ? new Building4A(player) : new Building4B(player),
                    sides.get(4) == Side.A ? new Building5A(player) : new Building5B(player),
                    sides.get(5) == Side.A ? new Building6A(player) : new Building6B(player),
                    sides.get(6) == Side.A ? new Building7A(player) : new Building7B(player),
                    sides.get(7) == Side.A ? new Building8A(player) : new Building8B(player),
                    sides.get(8) == Side.A ? new Building9A(player) : new Building9B(player),
                    sides.get(9) == Side.A ? new Building10A(player) : new Building10B(player)));
        }

        private enum Side {
            A, B
        }
    }

    public static final class Building1A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building1A(Player player) {
            super(1, BuildingSet.Side.A, player, Hand.GREEN, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Gain2DollarsPerBuildingInWoods.class);
        }
    }

    public static final class Building2A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building2A(Player player) {
            super(2, BuildingSet.Side.A, player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard2GuernseyToGain4Dollars.class, Action.BuyCattle.class);
        }
    }

    public static final class Building3A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building3A(Player player) {
            super(3, BuildingSet.Side.A, player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.DiscardPairToGain3Dollars.class, Action.Move1Forward.class);
        }
    }

    public static final class Building4A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building4A(Player player) {
            super(4, BuildingSet.Side.A, player, Hand.BLACK, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.RemoveHazardFor5Dollars.class, Action.Move2Forward.class);
        }
    }

    public static final class Building5A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building5A(Player player) {
            super(5, BuildingSet.Side.A, player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.HireCheapWorker.class, Action.MoveEngineForward.class);
        }
    }

    public static final class Building6A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building6A(Player player) {
            super(6, BuildingSet.Side.A, player, Hand.NONE, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1HolsteinToGain10Dollars.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class Building7A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building7A(Player player) {
            super(7, BuildingSet.Side.A, player, Hand.BOTH, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class);
        }
    }

    public static final class Building8A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building8A(Player player) {
            super(8, BuildingSet.Side.A, player, Hand.GREEN, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithIndians.class, Action.SingleOrDoubleAuxiliaryAction.class),
                    Action.MoveEngineAtMost2Forward.class);
        }
    }

    public static final class Building9A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building9A(Player player) {
            super(9, BuildingSet.Side.A, player, Hand.NONE, 7, 9);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.MoveEngineAtMost3Forward.class, Action.ExtraordinaryDelivery.class);
        }
    }

    public static final class Building10A extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building10A(Player player) {
            super(10, BuildingSet.Side.A, player, Hand.BLACK, 9, 13);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.MaxCertificates.class, Action.MoveEngineAtMost5Forward.class);
        }
    }

    private static class Building1B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building1B(Player player) {
            super(1, BuildingSet.Side.B, player, Hand.GREEN, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1ObjectiveCardToGain2Certificates.class, Action.MoveEngine1BackwardsToGain3Dollars.class);
        }
    }

    private static class Building2B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building2B(Player player) {
            super(2, BuildingSet.Side.B, player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1JerseyToMoveEngine1Forward.class, Action.Discard1DutchBeltToGain3Dollars.class);
        }
    }

    private static class Building3B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building3B(Player player) {
            super(3, BuildingSet.Side.B, player, Hand.NONE, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.SingleOrDoubleAuxiliaryAction.class, Action.Move1Forward.class);
        }
    }

    private static class Building4B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building4B(Player player) {
            super(4, BuildingSet.Side.B, player, Hand.BLACK, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(PossibleAction.whenThen(0, game.currentPlayerState().getNumberOfCowboys(), Action.DrawCard.class, Action.DiscardCard.class), Action.Move3Forward.class);
        }
    }

    private static class Building5B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building5B(Player player) {
            super(5, BuildingSet.Side.B, player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1BlackAngusToGain2Certificates.class, Action.Gain1DollarPerEngineer.class);
        }
    }

    private static class Building6B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building6B(Player player) {
            super(6, BuildingSet.Side.B, player, Hand.NONE, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class);
        }
    }

    private static class Building7B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building7B(Player player) {
            super(7, BuildingSet.Side.B, player, Hand.BOTH, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class);
        }
    }

    private static class Building8B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building8B(Player player) {
            super(8, BuildingSet.Side.B, player, Hand.NONE, 6, 8);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.UseAdjacentBuilding.class);
        }
    }

    private static class Building9B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building9B(Player player) {
            super(9, BuildingSet.Side.B, player, Hand.NONE, 6, 8);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.UpgradeAnyStationBehindEngine.class);
        }
    }

    private static class Building10B extends PlayerBuilding {

        private static final long serialVersionUID = 1L;

        Building10B(Player player) {
            super(10, BuildingSet.Side.B, player, Hand.BLACK, 8, 11);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Gain4Dollars.class, Action.MoveEngineAtMost4Forward.class, Action.Move4Forward.class);
        }
    }
}
