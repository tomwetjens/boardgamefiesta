package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public abstract class PlayerBuilding extends Building {

    private final Player player;
    private final int craftsmen;
    private final int points;
    private final int number;

    private PlayerBuilding(Name name, Player player, Hand hand, int craftsmen, int points) {
        super(name.toString(), hand);

        this.number = name.getNumber();
        this.player = player;
        this.craftsmen = craftsmen;
        this.points = points;
    }

    static PlayerBuilding forName(String name, Player player) {
        switch (name) {
            case "1a":
                return new PlayerBuilding.Building1A(player);
            case "2a":
                return new PlayerBuilding.Building2A(player);
            case "3a":
                return new PlayerBuilding.Building3A(player);
            case "4a":
                return new PlayerBuilding.Building4A(player);
            case "5a":
                return new PlayerBuilding.Building5A(player);
            case "6a":
                return new PlayerBuilding.Building6A(player);
            case "7a":
                return new PlayerBuilding.Building7A(player);
            case "8a":
                return new PlayerBuilding.Building8A(player);
            case "9a":
                return new PlayerBuilding.Building9A(player);
            case "10a":
                return new PlayerBuilding.Building10A(player);
            case "1b":
                return new PlayerBuilding.Building1B(player);
            case "2b":
                return new PlayerBuilding.Building2B(player);
            case "3b":
                return new PlayerBuilding.Building3B(player);
            case "4b":
                return new PlayerBuilding.Building4B(player);
            case "5b":
                return new PlayerBuilding.Building5B(player);
            case "6b":
                return new PlayerBuilding.Building6B(player);
            case "7b":
                return new PlayerBuilding.Building7B(player);
            case "8b":
                return new PlayerBuilding.Building8B(player);
            case "9b":
                return new PlayerBuilding.Building9B(player);
            case "10b":
                return new PlayerBuilding.Building10B(player);
            case "11a":
                return new PlayerBuilding.Building11A(player);
            case "11b":
                return new PlayerBuilding.Building11B(player);
            case "12a":
                return new PlayerBuilding.Building12A(player);
            case "12b":
                return new PlayerBuilding.Building12B(player);
            case "13a":
                return new PlayerBuilding.Building13A(player);
            case "13b":
                return new PlayerBuilding.Building13B(player);
            default:
                throw new IllegalArgumentException("Unknown player building: " + name);
        }
    }

    public enum Side {
        A, B
    }

    @Value(staticConstructor = "of")
    public static class Name {
        int number;
        Side side;

        @Override
        public String toString() {
            return number + side.name().toLowerCase();
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class BuildingSet {

        public static final Set<Integer> ALL = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
        public static final Set<Integer> ORIGINAL = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        public static final BuildingSet BEGINNER = new BuildingSet(ORIGINAL.stream()
                .map(number -> number + "a")
                .collect(Collectors.toSet()));

        @NonNull Set<String> names;

        public static BuildingSet random(@NonNull Game.Options options, @NonNull Random random) {
            var numbers = new HashSet<>(ORIGINAL);

            if (options.isRailsToTheNorth()) {
                numbers.add(11);
                numbers.add(12);
            } else if (options.isBuilding11()) {
                numbers.add(11);
            }

            if (options.isBuilding13()) {
                numbers.add(13);
            }

            return new BuildingSet(numbers.stream()
                    .map(number -> number + (random.nextBoolean() ? "a" : "b"))
                    .collect(Collectors.toSet()));
        }

        public static BuildingSet from(Game.Options options, @NonNull Random random) {
            return options.getBuildings() == Game.Options.Buildings.BEGINNER
                    ? BEGINNER
                    : random(options, random);
        }

        public Set<PlayerBuilding> createPlayerBuildings(@NonNull Player player) {
            return names.stream()
                    .map(name -> PlayerBuilding.forName(name, player))
                    .collect(Collectors.toSet());
        }

    }

    public static final class Building1A extends PlayerBuilding {

        Building1A(Player player) {
            super(Name.of(1, Side.A), player, Hand.GREEN, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Gain2DollarsPerBuildingInWoods.class);
        }
    }

    public static final class Building2A extends PlayerBuilding {

        Building2A(Player player) {
            super(Name.of(2, Side.A), player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Stream.of(
                    PossibleAction.optional(Action.Discard1GuernseyToGain4Dollars.class),
                    PossibleAction.repeat(0, game.currentPlayerState().getNumberOfCowboys(), Action.BuyCattle.class),
                    PossibleAction.repeat(0, game.currentPlayerState().getNumberOfCowboys(), Action.Draw2CattleCards.class)));
        }
    }

    public static final class Building3A extends PlayerBuilding {

        Building3A(Player player) {
            super(Name.of(3, Side.A), player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.DiscardPairToGain3Dollars.class, Action.Move1Forward.class);
        }
    }

    public static final class Building4A extends PlayerBuilding {

        Building4A(Player player) {
            super(Name.of(4, Side.A), player, Hand.BLACK, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.RemoveHazardFor5Dollars.class, Action.Move2Forward.class);
        }
    }

    public static final class Building5A extends PlayerBuilding {

        Building5A(Player player) {
            super(Name.of(5, Side.A), player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.HireWorkerMinus1.class, Action.MoveEngineForward.class);
        }
    }

    public static final class Building6A extends PlayerBuilding {

        Building6A(Player player) {
            super(Name.of(6, Side.A), player, Hand.NONE, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1HolsteinToGain10Dollars.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class Building7A extends PlayerBuilding {

        Building7A(Player player) {
            super(Name.of(7, Side.A), player, Hand.BOTH, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class);
        }
    }

    public static final class Building8A extends PlayerBuilding {

        Building8A(Player player) {
            super(Name.of(8, Side.A), player, Hand.GREEN, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithTribes.class, Action.SingleOrDoubleAuxiliaryAction.class),
                    Action.MoveEngineAtMost2Forward.class);
        }
    }

    public static final class Building9A extends PlayerBuilding {

        Building9A(Player player) {
            super(Name.of(9, Side.A), player, Hand.NONE, 7, 9);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.MoveEngineAtMost3Forward.class, Action.ExtraordinaryDelivery.class);
        }
    }

    public static final class Building10A extends PlayerBuilding {

        Building10A(Player player) {
            super(Name.of(10, Side.A), player, Hand.BLACK, 9, 13);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.MaxCertificates.class, Action.Move5Forward.class);
        }
    }

    private static class Building1B extends PlayerBuilding {

        Building1B(Player player) {
            super(Name.of(1, Side.B), player, Hand.GREEN, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1ObjectiveCardToGain2Certificates.class, Action.MoveEngine1BackwardsToGain3Dollars.class);
        }
    }

    static class Building2B extends PlayerBuilding {

        Building2B(Player player) {
            super(Name.of(2, Side.B), player, Hand.NONE, 1, 1);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1JerseyToMoveEngine1Forward.class, Action.Discard1DutchBeltToGain3Dollars.class);
        }
    }

    private static class Building3B extends PlayerBuilding {

        Building3B(Player player) {
            super(Name.of(3, Side.B), player, Hand.NONE, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.SingleOrDoubleAuxiliaryAction.class, Action.Move1Forward.class);
        }
    }

    static class Building4B extends PlayerBuilding {

        Building4B(Player player) {
            super(Name.of(4, Side.B), player, Hand.BLACK, 2, 3);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            var numberOfCowboys = game.currentPlayerState().getNumberOfCowboys();
            return PossibleAction.any(drawCardsThenDiscardCardsUpToNumberOfCowboys(numberOfCowboys), Action.Move3Forward.class);
        }

        private static PossibleAction drawCardsThenDiscardCardsUpToNumberOfCowboys(int numberOfCowboys) {
            var choices = new HashSet<PossibleAction>();

            choices.add(PossibleAction.optional(Action.DrawCard.class));

            if (numberOfCowboys > 1) {
                choices.add(PossibleAction.optional(Action.Draw2Cards.class));
            }

            if (numberOfCowboys > 2) {
                choices.add(PossibleAction.optional(Action.Draw3Cards.class));
            }

            if (numberOfCowboys > 3) {
                choices.add(PossibleAction.optional(Action.Draw4Cards.class));
            }

            if (numberOfCowboys > 4) {
                choices.add(PossibleAction.optional(Action.Draw5Cards.class));
            }

            if (numberOfCowboys > 5) {
                choices.add(PossibleAction.optional(Action.Draw6Cards.class));
            }

            return PossibleAction.choice(choices);
        }
    }

    static class Building5B extends PlayerBuilding {

        Building5B(Player player) {
            super(Name.of(5, Side.B), player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Discard1BlackAngusToGain2Certificates.class, Action.Gain1DollarPerEngineer.class);
        }
    }

    private static class Building6B extends PlayerBuilding {

        Building6B(Player player) {
            super(Name.of(6, Side.B), player, Hand.NONE, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class);
        }
    }

    static class Building7B extends PlayerBuilding {

        Building7B(Player player) {
            super(Name.of(7, Side.B), player, Hand.BOTH, 5, 6);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class);
        }
    }

    static class Building8B extends PlayerBuilding {

        Building8B(Player player) {
            super(Name.of(8, Side.B), player, Hand.NONE, 6, 8);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.UseAdjacentBuilding.class);
        }
    }

    static class Building9B extends PlayerBuilding {

        Building9B(Player player) {
            super(Name.of(9, Side.B), player, Hand.NONE, 6, 8);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.UpgradeAnyStationBehindEngine.class);
        }
    }

    static class Building10B extends PlayerBuilding {

        Building10B(Player player) {
            super(Name.of(10, Side.B), player, Hand.BLACK, 8, 11);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(Action.Gain4Dollars.class, Action.MoveEngineAtMost4Forward.class, Action.Move4Forward.class);
        }
    }

    static class Building11A extends PlayerBuilding {

        Building11A(Player player) {
            super(Name.of(11, Side.A), player, Hand.NONE, 12, 25);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.RemoveHazardFor2Dollars.class);
        }
    }

    static class Building11B extends PlayerBuilding {

        Building11B(Player player) {
            super(Name.of(11, Side.B), player, Hand.NONE, 5, 10);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.MoveEngineForwardUpToNumberOfHazards.class);
        }
    }

    static class Building12A extends PlayerBuilding {

        Building12A(Player player) {
            super(Name.of(12, Side.A), player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.optional(Action.Gain1CertificateAnd1DollarPerBell.class);
        }
    }

    static class Building12B extends PlayerBuilding {

        Building12B(Player player) {
            super(Name.of(12, Side.B), player, Hand.NONE, 3, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(
                    Action.Gain1DollarPerCraftsman.class,
                    Action.PlaceBranchlet.class);
        }
    }

    static class Building13A extends PlayerBuilding {

        Building13A(Player player) {
            super(Name.of(13, Side.A), player, Hand.GREEN, 4, 5);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(
                    Action.Discard1JerseyForSingleAuxiliaryAction.class,
                    Action.PlaceCheapBuilding.class);
        }
    }

    static class Building13B extends PlayerBuilding {

        Building13B(Player player) {
            super(Name.of(13, Side.B), player, Hand.NONE, 2, 4);
        }

        @Override
        PossibleAction getPossibleAction(Game game) {
            return PossibleAction.any(
                    Action.Gain2DollarsPerStation.class,
                    Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }
}
