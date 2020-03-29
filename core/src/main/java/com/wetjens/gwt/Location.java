package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

public abstract class Location {

    @NonNull
    private final Set<Location> next;

    Location(Location... next) {
        this.next = new HashSet<>(Arrays.asList(next));
    }

    Set<Location> getNext() {
        return Collections.unmodifiableSet(next);
    }

    Optional<PossibleAction> getPossibleAction() {
        return Optional.empty();
    }

    Fee getFee() {
        return Fee.NONE;
    }

    abstract boolean isEmpty();

    public boolean isDirect(Location to) {
        return next.contains(to) || (isEmpty() && next.stream().anyMatch(between -> between.isDirect(to)));
    }

    static final class Start extends Location {

        public Start(Location... next) {
            super(next);
        }

        @Override
        boolean isEmpty() {
            return false;
        }
    }

    static final class BuildingLocation extends Location {

        private final Class<? extends Action> riskAction;

        private Building building;

        BuildingLocation(Location... next) {
            super(next);

            this.riskAction = null;
        }

        BuildingLocation(Class<? extends Action> riskAction, Location... next) {
            super(next);

            this.riskAction = riskAction;
        }

        @Override
        Optional<PossibleAction> getPossibleAction() {
            if (building != null) {
                if (riskAction != null) {
                    return Optional.of(PossibleAction.choice(building.getPossibleAction(), riskAction, SingleAuxiliaryAction.class));
                } else {
                    return Optional.of(PossibleAction.choice(building.getPossibleAction(), SingleAuxiliaryAction.class));
                }
            }
            return Optional.empty();
        }

        @Override
        Fee getFee() {
            if (building != null) {
                return building.getFee();
            }
            return Fee.NONE;
        }

        @Override
        boolean isEmpty() {
            return building != null;
        }

        Optional<Building> getBuilding() {
            return Optional.ofNullable(building);
        }

        void placeBuilding(Building building) {
            if (this.building != null) {
                if (this.building instanceof NeutralBuilding) {
                    throw new IllegalStateException("Cannot replace a neutral building");
                }
                if (this.building instanceof PlayerBuilding) {
                    if (!(building instanceof PlayerBuilding)) {
                        throw new IllegalStateException("Replacement must be a player building");
                    }

                    PlayerBuilding existing = (PlayerBuilding) this.building;
                    PlayerBuilding replacement = (PlayerBuilding) building;

                    if (replacement.getPlayer() != existing.getPlayer()) {
                        throw new IllegalStateException("Can only replace building of same player");
                    }

                    if (replacement.getCraftsmen() < existing.getCraftsmen()) {
                        throw new IllegalStateException("Replacement building must be higher valued that existing building");
                    }
                }
            }
            this.building = building;
        }

        public static class SingleAuxiliaryAction extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                return ImmediateActions.of(PossibleAction.choice(unlockedActions(game.currentPlayerState())));
            }

            protected Set<Class<? extends Action>> unlockedActions(PlayerState playerState) {
                Set<Class<? extends Action>> actions = new HashSet<>();
                if (playerState.hasUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
                    actions.add(Gain1Dollars.class);
                }
                if (playerState.hasUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
                    actions.add(DrawCardThenDiscardCard.Draw1CardThenDiscard1Card.class);
                }
                if (playerState.hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
                    actions.add(Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate.class);
                }
                if (playerState.hasUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
                    actions.add(Pay1DollarToMoveEngine1SpaceForward.class);
                }
                if (playerState.hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
                    actions.add(MoveEngine1SpaceBackwardsToRemove1Card.class);
                }
                return actions;
            }

            public static final class Gain1Dollars extends Action {
                @Override
                public ImmediateActions perform(Game game) {
                    game.currentPlayerState().gainDollars(1);
                    return ImmediateActions.none();
                }
            }

            public static final class Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate extends Action {
                private final RailroadTrack.Space to;

                public Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate(RailroadTrack.Space to) {
                    this.to = to;
                }

                @Override
                public ImmediateActions perform(Game game) {
                    game.currentPlayerState().payDollars(1);
                    ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1);
                    // TODO Check if gaining certificate AFTER possible immediate actions from railroad track is OK
                    game.currentPlayerState().gainCertificates(1);
                    return immediateActions;
                }
            }

            public static final class Pay1DollarToMoveEngine1SpaceForward extends Action {
                private final RailroadTrack.Space to;

                public Pay1DollarToMoveEngine1SpaceForward(RailroadTrack.Space to) {
                    this.to = to;
                }

                @Override
                public ImmediateActions perform(Game game) {
                    game.currentPlayerState().payDollars(1);
                    return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1);
                }
            }

            public static final class MoveEngine1SpaceBackwardsToRemove1Card extends Action {
                private final RailroadTrack.Space to;

                public MoveEngine1SpaceBackwardsToRemove1Card(RailroadTrack.Space to) {
                    this.to = to;
                }

                @Override
                public ImmediateActions perform(Game game) {
                    return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1)
                            .andThen(PossibleAction.of(Remove1Card.class));
                }
            }
        }
    }

    static final class HazardLocation extends Location {

        @NonNull
        @Getter
        private final HazardType type;

        private Hazard hazard;

        HazardLocation(@NonNull HazardType type, Location... next) {
            super(next);
            this.type = type;
        }

        Optional<Hazard> getHazard() {
            return Optional.ofNullable(hazard);
        }

        @Override
        Optional<PossibleAction> getPossibleAction() {
            if (hazard != null) {
                return Optional.of(PossibleAction.any(BuildingLocation.SingleAuxiliaryAction.class));
            }
            return Optional.empty();
        }

        @Override
        Fee getFee() {
            if (hazard != null) {
                return hazard.getFee();
            }
            return Fee.NONE;
        }

        @Override
        boolean isEmpty() {
            return hazard != null;
        }

        public void placeHazard(@NonNull Hazard hazard) {
            if (this.hazard != null) {
                throw new IllegalStateException("Location already has a hazard");
            }
            this.hazard = hazard;
        }
    }

    static final class KansasCity extends Location {

        KansasCity(Location... next) {
            super(next);
        }

        @Override
        Optional<PossibleAction> getPossibleAction() {
            return Optional.of(PossibleAction.of(ChooseForesights.class));
        }

        @Override
        boolean isEmpty() {
            return false;
        }

        public static final class ChooseForesights extends Action {

            private final List<Choice> choices;

            public ChooseForesights(@NonNull List<Choice> choices) {
                if (choices.size() != 3) {
                    throw new IllegalArgumentException("Must have 3 choices");
                }
                this.choices = new ArrayList<>(choices);
            }

            @Override
            public ImmediateActions perform(Game game) {
                for (int columnIndex = 0; columnIndex < choices.size(); columnIndex++) {
                    Choice choice = choices.get(columnIndex);

                    KansasCitySupply.Tile tile = game.getForesights().take(columnIndex, choice.getRowIndex());

                    if (tile.getWorker() != null) {
                        boolean fillUpCattleMarket = game.getJobMarket().addWorker(tile.getWorker());
                        if (fillUpCattleMarket) {
                            game.getCattleMarket().fillUp();
                        }
                    } else if (tile.getHazard() != null) {
                        if (!(choice.getLocation() instanceof HazardLocation)) {
                            throw new IllegalArgumentException("Must pick a hazard location");
                        }
                        ((HazardLocation) choice.getLocation()).placeHazard(tile.getHazard());
                    } else {
                        if (!(choice.getLocation() instanceof TeepeeLocation)) {
                            throw new IllegalArgumentException("Must pick a teepee location");
                        }
                        ((TeepeeLocation) choice.getLocation()).placeTeepee(tile.getTeepee());
                    }
                }

                return ImmediateActions.none();
            }

            @Value
            public static final class Choice {
                private final int rowIndex;
                private final Location location;
            }
        }

        public static final class DeliverToCity extends Action {

            private final City city;

            DeliverToCity(City city) {
                this.city = city;
            }

            @Override
            public ImmediateActions perform(Game game) {

                // TODO
                if (city == City.SAN_FRANCISCO) {
                    return ImmediateActions.of(PossibleAction.of(GainObjectiveCard.class));
                }

                return ImmediateActions.none();
            }
        }
    }

    static final class TeepeeLocation extends Location {

        private final int reward;
        private Teepee teepee;

        TeepeeLocation(int reward, Location... next) {
            super(next);
            this.reward = reward;
        }

        @Override
        public Optional<PossibleAction> getPossibleAction() {
            if (teepee != null) {
                return Optional.of(PossibleAction.any(BuildingLocation.SingleAuxiliaryAction.class));
            }
            return Optional.empty();
        }

        @Override
        boolean isEmpty() {
            return teepee != null;
        }

        public void placeTeepee(@NonNull Teepee teepee) {
            if (this.teepee != null) {
                throw new IllegalStateException("Location already has a teepee");
            }
            this.teepee = teepee;
        }
    }
}
