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

public abstract class PlayerBuilding extends Building {

    @Getter
    private final Player player;
    private int craftsmen;

    protected PlayerBuilding(Player player, Fee fee) {
        super(fee);

        this.player = player;
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

        public Set<com.wetjens.gwt.PlayerBuilding> createPlayerBuildings(@NonNull Player player) {
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

    public static final class Building1A extends com.wetjens.gwt.PlayerBuilding {

        public Building1A(Player player) {
            super(player, Fee.GREEN);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building2A extends com.wetjens.gwt.PlayerBuilding {

        public Building2A(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building3A extends com.wetjens.gwt.PlayerBuilding {

        public Building3A(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building4A extends com.wetjens.gwt.PlayerBuilding {

        public Building4A(Player player) {
            super(player, Fee.BLACK);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building5A extends com.wetjens.gwt.PlayerBuilding {

        public Building5A(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building6A extends com.wetjens.gwt.PlayerBuilding {

        public Building6A(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building7A extends com.wetjens.gwt.PlayerBuilding {

        public Building7A(Player player) {
            super(player, Fee.BOTH);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building8A extends com.wetjens.gwt.PlayerBuilding {

        public Building8A(Player player) {
            super(player, Fee.GREEN);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building9A extends com.wetjens.gwt.PlayerBuilding {

        public Building9A(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    public static final class Building10A extends com.wetjens.gwt.PlayerBuilding {

        public Building10A(Player player) {
            super(player, Fee.BLACK);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building1B extends com.wetjens.gwt.PlayerBuilding {

        public Building1B(Player player) {
            super(player, Fee.GREEN);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building2B extends com.wetjens.gwt.PlayerBuilding {

        public Building2B(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building3B extends com.wetjens.gwt.PlayerBuilding {

        public Building3B(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building4B extends com.wetjens.gwt.PlayerBuilding {

        public Building4B(Player player) {
            super(player, Fee.BLACK);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building5B extends com.wetjens.gwt.PlayerBuilding {

        public Building5B(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building6B extends com.wetjens.gwt.PlayerBuilding {

        public Building6B(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building7B extends com.wetjens.gwt.PlayerBuilding {

        public Building7B(Player player) {
            super(player, Fee.BOTH);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building8B extends com.wetjens.gwt.PlayerBuilding {

        public Building8B(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building9B extends com.wetjens.gwt.PlayerBuilding {

        public Building9B(Player player) {
            super(player, Fee.NONE);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }

    private static class Building10B extends com.wetjens.gwt.PlayerBuilding {

        public Building10B(Player player) {
            super(player, Fee.BLACK);
        }

        @Override
        PossibleAction getPossibleAction() {
            return null;
        }
    }
}
