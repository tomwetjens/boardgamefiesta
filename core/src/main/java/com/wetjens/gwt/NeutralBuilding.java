package com.wetjens.gwt;

import java.util.Optional;

public abstract class NeutralBuilding extends Building {

    protected NeutralBuilding() {
        super(Fee.NONE);
    }

    public static final class A extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(A.DiscardOneGuernsey.class, A.HireWorker.class, A.HireSecondWorker.class);
        }

        public static final class DiscardOneGuernsey extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                PlayerState playerState = game.currentPlayerState();
                playerState.discardCattleCards(CattleType.GUERNSEY, 1);
                playerState.gainDollars(2);
                return ImmediateActions.none();
            }
        }

        public static class HireWorker extends Action {

            private final Worker worker;
            private final int modifier;

            public HireWorker(Worker worker) {
                this(worker, 0);
            }

            protected HireWorker(Worker worker, int modifier) {
                this.worker = worker;
                this.modifier = modifier;
            }

            @Override
            public ImmediateActions perform(Game game) {
                int cost = game.getJobMarket().cost(worker) + modifier;
                game.currentPlayerState().payDollars(cost);
                game.getJobMarket().takeWorker(worker);
                return game.currentPlayerState().gainWorker(worker);
            }
        }

        public static final class HireSecondWorker extends A.HireWorker {
            public HireSecondWorker(Worker worker) {
                super(worker, 2);
            }
        }
    }

    public static final class B extends NeutralBuilding {

        @Override
        public PossibleAction getPossibleAction() {
            return PossibleAction.any(B.DiscardOneDutchBelt.class, B.PlaceBuilding.class);
        }

        public static final class DiscardOneDutchBelt extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                PlayerState playerState = game.currentPlayerState();
                playerState.discardCattleCards(CattleType.DUTCH_BELT, 1);
                playerState.gainDollars(2);
                return ImmediateActions.none();
            }
        }

        public static final class PlaceBuilding extends Action {
            private final int costPerCraftsman;
            private final Location.BuildingLocation location;
            private final PlayerBuilding building;

            public PlaceBuilding(Location.BuildingLocation location, PlayerBuilding building) {
                this.location = location;
                this.building = building;
                this.costPerCraftsman = 2;
            }

            @Override
            public ImmediateActions perform(Game game) {
                if (!game.currentPlayerState().hasAvailable(building)) {
                    throw new IllegalStateException("Building not available for player");
                }

                int craftsmenNeeded = craftsmenNeeded();

                if (craftsmenNeeded > game.currentPlayerState().getNumberOfCraftsmen()) {
                    throw new IllegalStateException("Not enough craftsmen");
                }

                int cost = craftsmenNeeded * costPerCraftsman;
                game.currentPlayerState().payDollars(cost);
                game.currentPlayerState().removeBuilding(building);

                location.placeBuilding(building);

                return ImmediateActions.none();
            }

            private int craftsmenNeeded() {
                return existingBuildingToReplace()
                        // If replacing an existing building, only the difference is needed
                        .filter(existingBuilding -> {
                            if (existingBuilding.getCraftsmen() > building.getCraftsmen()) {
                                throw new IllegalStateException("Replacement building must be higher valued that existing building");
                            }
                            return true;
                        })
                        .map(existingBuilding -> building.getCraftsmen() - existingBuilding.getCraftsmen())
                        .orElse(building.getCraftsmen());
            }

            private Optional<PlayerBuilding> existingBuildingToReplace() {
                return location.getBuilding()
                        .filter(existingBuilding -> {
                            if (!(existingBuilding instanceof PlayerBuilding)) {
                                throw new IllegalStateException("Can only replace a player building");
                            }
                            return true;
                        })
                        .map(existingBuilding -> (PlayerBuilding) existingBuilding)
                        .filter(existingBuilding -> {
                            if (existingBuilding.getPlayer() != building.getPlayer()) {
                                throw new IllegalStateException("Can only replace building of same player");
                            }
                            return true;
                        });
            }
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
