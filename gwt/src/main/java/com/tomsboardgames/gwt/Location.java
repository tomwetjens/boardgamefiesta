package com.tomsboardgames.gwt;

import lombok.Getter;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Location {

    @Getter
    private final String name;

    @NonNull
    private final Set<Location> next;

    Location(String name, Location... next) {
        this.name = name;
        this.next = new LinkedHashSet<>(Arrays.asList(next));
    }

    public Set<Location> getNext() {
        return Collections.unmodifiableSet(next);
    }

    ImmediateActions activate(Game game) {
        return ImmediateActions.none();
    }

    public Hand getHand() {
        return Hand.NONE;
    }

    public abstract boolean isEmpty();

    @Override
    public String toString() {
        return name;
    }

    public boolean isDirect(Location to) {
        return next.stream().anyMatch(between -> between == to || (between.isEmpty() && between.isDirect(to)));
    }

    public Set<Location> reachableLocations(int atLeast, int atMost) {
        if (atMost <= 0) {
            return Collections.emptySet();
        }

        return next.stream()
                .flatMap(step -> {
                    if (!step.isEmpty()) {
                        return Stream.concat(atLeast <= 1 ? Stream.of(step) : Stream.empty(),
                                step.reachableLocations(atLeast - 1, atMost - 1).stream());
                    } else {
                        return step.reachableLocations(atLeast, atMost).stream();
                    }
                })
                .collect(Collectors.toSet());
    }

    public Stream<List<Location>> routes(Location to) {
        if (next.contains(to)) {
            return Stream.of(List.of(to));
        }

        return next.stream().flatMap(location -> location.routes(to)
                .map(route -> Stream.concat(Stream.of(location), route.stream()).collect(Collectors.toList())));
    }

    public static class Start extends Location {

        Start(Location... next) {
            super("START", next);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    @Getter
    public static class BuildingLocation extends Location {

        private final Class<? extends Action> riskAction;
        private final boolean inWoods;

        private Building building;

        BuildingLocation(String name, boolean inWoods, Location... next) {
            super(name, next);

            this.riskAction = null;
            this.inWoods = inWoods;
        }

        BuildingLocation(String name, Class<? extends Action> riskAction, boolean inWoods, Location... next) {
            super(name, next);

            this.riskAction = riskAction;
            this.inWoods = inWoods;
        }

        @Override
        ImmediateActions activate(Game game) {
            if (building != null) {
                if (building instanceof NeutralBuilding || ((PlayerBuilding) building).getPlayer() == game.getCurrentPlayer()) {
                    var buildingAction = building.getPossibleAction(game);

                    if (riskAction != null) {
                        // There is an optional risk action

                        if (buildingAction.canPerform(Action.SingleOrDoubleAuxiliaryAction.class)) {
                            // Leave out the SingleAuxiliaryAction, because it is useless when you can choose SingleOrDoubleAuxiliaryAction
                            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(PossibleAction.any(buildingAction, riskAction))));
                        } else {
                            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(PossibleAction.any(buildingAction, riskAction), Action.SingleAuxiliaryAction.class)));
                        }
                    } else {
                        // No risk action

                        if (buildingAction.canPerform(Action.SingleOrDoubleAuxiliaryAction.class)) {
                            // Leave out the SingleAuxiliaryAction, because it is useless when you can choose SingleOrDoubleAuxiliaryAction
                            return ImmediateActions.of(buildingAction);
                        } else {
                            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(buildingAction, Action.SingleAuxiliaryAction.class)));
                        }
                    }
                } else {
                    // Other player's building, only allowed to use aux action
                    return ImmediateActions.of(PossibleAction.optional(Action.SingleAuxiliaryAction.class));
                }
            }
            return ImmediateActions.none();
        }

        @Override
        public Hand getHand() {
            if (building != null) {
                return building.getHand();
            }
            return Hand.NONE;
        }

        @Override
        public boolean isEmpty() {
            return building == null;
        }

        public Optional<Building> getBuilding() {
            return Optional.ofNullable(building);
        }

        void placeBuilding(Building building) {
            if (this.building != null) {
                if (this.building instanceof NeutralBuilding) {
                    throw new GWTException(GWTError.CANNOT_REPLACE_NEUTRAL_BUILDING);
                }

                if (this.building instanceof PlayerBuilding) {
                    if (!(building instanceof PlayerBuilding)) {
                        throw new GWTException(GWTError.REPLACEMENT_BUILDING_MUST_BE_PLAYER_BUILDING);
                    }

                    PlayerBuilding existing = (PlayerBuilding) this.building;
                    PlayerBuilding replacement = (PlayerBuilding) building;

                    if (replacement.getPlayer() != existing.getPlayer()) {
                        throw new GWTException(GWTError.CANNOT_REPLACE_BUILDING_OF_OTHER_PLAYER);
                    }

                    if (replacement.getCraftsmen() < existing.getCraftsmen()) {
                        throw new GWTException(GWTError.REPLACEMENT_BUILDING_MUST_BE_HIGHER);
                    }
                }
            }
            this.building = building;
        }
    }

    public static final class HazardLocation extends Location {

        @NonNull
        @Getter
        private final HazardType type;

        @Getter
        private final int number;

        private Hazard hazard;

        HazardLocation(@NonNull HazardType type, int number, Location... next) {
            super(type + "-" + number, next);

            this.type = type;
            this.number = number;
        }

        public Optional<Hazard> getHazard() {
            return Optional.ofNullable(hazard);
        }

        @Override
        ImmediateActions activate(Game game) {
            if (hazard != null) {
                return ImmediateActions.of(PossibleAction.optional(Action.SingleAuxiliaryAction.class));
            }
            return ImmediateActions.none();
        }

        @Override
        public Hand getHand() {
            if (hazard != null) {
                return hazard.getHand();
            }
            return Hand.NONE;
        }

        @Override
        public boolean isEmpty() {
            return hazard == null;
        }

        void placeHazard(@NonNull Hazard hazard) {
            if (this.hazard != null) {
                throw new GWTException(GWTError.LOCATION_NOT_EMPTY);
            }

            if (hazard.getType() != this.type) {
                throw new GWTException(GWTError.HAZARD_MUST_BE_OF_TYPE);
            }

            this.hazard = hazard;
        }

        void removeHazard() {
            if (this.hazard == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }
            this.hazard = null;
        }
    }

    public static class KansasCity extends Location {

        KansasCity(Location... next) {
            super("KANSAS_CITY", next);
        }

        @Override
        ImmediateActions activate(Game game) {
            return ImmediateActions.of(PossibleAction.mandatory(Action.ChooseForesights.class));
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public static final class TeepeeLocation extends Location {

        @Getter
        private final int reward;
        private Teepee teepee;

        TeepeeLocation(int reward, Location... next) {
            super("TEEPEE-" + reward, next);
            this.reward = reward;
        }

        @Override
        public ImmediateActions activate(Game game) {
            if (teepee != null) {
                return ImmediateActions.of(PossibleAction.optional(Action.SingleAuxiliaryAction.class));
            }
            return ImmediateActions.none();
        }

        @Override
        public boolean isEmpty() {
            return teepee == null;
        }

        public Optional<Teepee> getTeepee() {
            return Optional.ofNullable(teepee);
        }

        @Override
        public Hand getHand() {
            if (teepee != null) {
                return teepee.getHand();
            }
            return Hand.NONE;
        }

        void placeTeepee(@NonNull Teepee teepee) {
            if (this.teepee != null) {
                throw new GWTException(GWTError.LOCATION_NOT_EMPTY);
            }
            this.teepee = teepee;
        }

        void removeTeepee() {
            if (this.teepee == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }
            this.teepee = null;
        }
    }
}
