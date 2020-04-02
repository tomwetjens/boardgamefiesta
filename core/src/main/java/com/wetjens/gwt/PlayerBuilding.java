package com.wetjens.gwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class PlayerBuilding extends Building {

    private final Player player;
    private final int craftsmen;
    private final int points;

    protected PlayerBuilding(Player player, Hand hand, int craftsmen, int points) {
        super(hand);

        this.player = player;
        this.craftsmen = craftsmen;
        this.points = points;
    }

    public int getCraftsmen() {
        return craftsmen;
    }

    public enum Variant {
        A,
        B;
    }

    public static final class VariantSet {

        private final List<Variant> variants;

        private VariantSet(List<Variant> variants) {
            this.variants = variants;
        }

        public static VariantSet firstGame() {
            return new VariantSet(Stream.generate(() -> Variant.A)
                    .limit(10)
                    .collect(Collectors.toList()));
        }

        public static VariantSet random(@NonNull Random random) {
            return new VariantSet(Stream.generate(() -> random.nextBoolean() ? Variant.A : Variant.B)
                    .limit(10)
                    .collect(Collectors.toList()));
        }

        public Set<PlayerBuilding> createPlayerBuildings(@NonNull Player player) {
            return new HashSet<>(Arrays.asList(
                    variants.get(0) == Variant.A ? new Building1A(player) : new Building1B(player),
                    variants.get(1) == Variant.A ? new Building2A(player) : new Building2B(player),
                    variants.get(2) == Variant.A ? new Building3A(player) : new Building3B(player),
                    variants.get(3) == Variant.A ? new Building4A(player) : new Building4B(player),
                    variants.get(4) == Variant.A ? new Building5A(player) : new Building5B(player),
                    variants.get(5) == Variant.A ? new Building6A(player) : new Building6B(player),
                    variants.get(6) == Variant.A ? new Building7A(player) : new Building7B(player),
                    variants.get(7) == Variant.A ? new Building8A(player) : new Building8B(player),
                    variants.get(8) == Variant.A ? new Building9A(player) : new Building9B(player),
                    variants.get(9) == Variant.A ? new Building10A(player) : new Building10B(player)));
        }
    }

    public static final class Building1A extends PlayerBuilding {

        public Building1A(Player player) {
            super(player, Hand.GREEN, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.optional(Action.Gain1DollarPerBuildingInWoods.class);
        }
    }

    public static final class Building2A extends PlayerBuilding {

        public Building2A(Player player) {
            super(player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard2GuernseyToGain4Dollars.class, Action.BuyCattle.class);
        }
    }

    public static final class Building3A extends PlayerBuilding {

        public Building3A(Player player) {
            super(player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.DiscardPairToGain3Dollars.class, Action.Move1Forward.class);
        }
    }

    public static final class Building4A extends PlayerBuilding {

        public Building4A(Player player) {
            super(player, Hand.BLACK, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.RemoveHazardFor5Dollars.class, Action.Move2Forward.class);
        }
    }

    public static final class Building5A extends PlayerBuilding {

        public Building5A(Player player) {
            super(player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.HireCheapWorker.class, Action.MoveEngineForward.class);
        }
    }

    public static final class Building6A extends PlayerBuilding {

        public Building6A(Player player) {
            super(player, Hand.NONE, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1HolsteinToGain10Dollars.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class Building7A extends PlayerBuilding {

        public Building7A(Player player) {
            super(player, Hand.BOTH, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.optional(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class);
        }
    }

    public static final class Building8A extends PlayerBuilding {

        public Building8A(Player player) {
            super(player, Hand.GREEN, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithIndians.class, Action.SingleOrDoubleAuxiliaryAction.class),
                    Action.MoveEngineAtMost2Forward.class);
        }
    }

    public static final class Building9A extends PlayerBuilding {

        public Building9A(Player player) {
            super(player, Hand.NONE, 7, 9);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.MoveEngineAtMost3Forward.class, Action.ExtraordinaryDelivery.class);
        }
    }

    public static final class Building10A extends PlayerBuilding {

        public Building10A(Player player) {
            super(player, Hand.BLACK, 9, 13);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.MaxCertificates.class, Action.MoveEngineAtMost5Forward.class);
        }
    }

    private static class Building1B extends PlayerBuilding {

        public Building1B(Player player) {
            super(player, Hand.GREEN, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1ObjectiveCardToGain2Certificates.class, Action.MoveEngine1BackwardsToGain3Dollars.class);
        }
    }

    private static class Building2B extends PlayerBuilding {

        public Building2B(Player player) {
            super(player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1JerseyToMoveEngine1Forward.class, Action.Discard1DutchBeltToGain3Dollars.class);
        }
    }

    private static class Building3B extends PlayerBuilding {

        public Building3B(Player player) {
            super(player, Hand.NONE, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.SingleOrDoubleAuxiliaryAction.class, Action.Move1Forward.class);
        }
    }

    private static class Building4B extends PlayerBuilding {

        public Building4B(Player player) {
            super(player, Hand.BLACK, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.DrawCardsUpToNumberOfCowboysThenDiscardCards.class, Action.Move3Forward.class);
        }
    }

    private static class Building5B extends PlayerBuilding {

        public Building5B(Player player) {
            super(player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Discard1BlackAngusToGain2Certificates.class, Action.Gain1DollarPerEngineer.class);
        }
    }

    private static class Building6B extends PlayerBuilding {

        public Building6B(Player player) {
            super(player, Hand.NONE, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.optional(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class);
        }
    }

    private static class Building7B extends PlayerBuilding {

        public Building7B(Player player) {
            super(player, Hand.BOTH, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.optional(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class);
        }
    }

    private static class Building8B extends PlayerBuilding {
        public Building8B(Player player) {
            super(player, Hand.NONE, 6, 8);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.optional(Action.UseAdjacentBuilding.class);
        }
    }

    private static class Building9B extends PlayerBuilding {

        public Building9B(Player player) {
            super(player, Hand.NONE, 6, 8);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.optional(Action.UpgradeAnyStationBehindEngine.class);
        }
    }

    private static class Building10B extends PlayerBuilding {

        public Building10B(Player player) {
            super(player, Hand.BLACK, 8, 11);
        }

        @Override
        PossibleAction getPossibleAction() {
            return PossibleAction.any(Action.Gain4Dollars.class, Action.MoveEngineAtMost4Forward.class, Action.Move4Forward.class);
        }
    }
}
