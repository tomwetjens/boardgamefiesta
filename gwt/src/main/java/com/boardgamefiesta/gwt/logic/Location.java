package com.boardgamefiesta.gwt.logic;

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

    PossibleAction activate(GWT game) {
        throw new GWTException(GWTError.LOCATION_EMPTY);
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
        PossibleAction activate(GWT game) {
            return activate(game, false);
        }

        /**
         * @param adjacent Normally using another player's building is not allowed, but when performing
         *                 the "Use Adjacent Location" tile action, it is allowed to use another player's building.
         */
        PossibleAction activate(GWT game, boolean adjacent) {
            if (building == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }

            game.currentPlayerState().activate(this);

            if (canUseBuilding(game, adjacent)) {
                var buildingAction = building.getPossibleAction(game);

                if (riskAction != null) {
                    // There is an optional risk action

                    if (buildingAction.canPerform(Action.SingleOrDoubleAuxiliaryAction.class)) {
                        // Leave out the SingleAuxiliaryAction, because it is useless when you can choose SingleOrDoubleAuxiliaryAction
                        return PossibleAction.optional(PossibleAction.choice(PossibleAction.any(buildingAction, riskAction)));
                    } else {
                        return PossibleAction.optional(PossibleAction.choice(PossibleAction.any(buildingAction, riskAction), Action.SingleAuxiliaryAction.class));
                    }
                } else {
                    // No risk action

                    if (buildingAction.canPerform(Action.SingleOrDoubleAuxiliaryAction.class)) {
                        // Leave out the SingleAuxiliaryAction, because it is useless when you can choose SingleOrDoubleAuxiliaryAction
                        return buildingAction;
                    } else {
                        return PossibleAction.optional(PossibleAction.choice(buildingAction, Action.SingleAuxiliaryAction.class));
                    }
                }
            } else {
                // Other player's building, only allowed to use aux action
                return PossibleAction.optional(Action.SingleAuxiliaryAction.class);
            }
        }

        private boolean canUseBuilding(GWT game, boolean adjacent) {
            return adjacent || building instanceof NeutralBuilding
                    || ((PlayerBuilding) building).getPlayer() == game.getCurrentPlayer();
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
        PossibleAction activate(GWT game) {
            if (hazard == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }
            return PossibleAction.optional(Action.SingleAuxiliaryAction.class);
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

        Hazard removeHazard() {
            if (this.hazard == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }

            var hazard = this.hazard;

            this.hazard = null;

            return hazard;
        }
    }

    public static class KansasCity extends Location {

        KansasCity(Location... next) {
            super("KANSAS_CITY", next);
        }

        @Override
        PossibleAction activate(GWT game) {
            if (game.getForesights().isEmpty(0)) {
                if (game.getForesights().isEmpty(1)) {
                    if (game.getForesights().isEmpty(2)) {
                        // No foresights left, go straight to delivery
                        return PossibleAction.mandatory(Action.DeliverToCity.class);
                    }
                    return PossibleAction.mandatory(Action.ChooseForesight3.class);
                }
                return PossibleAction.mandatory(Action.ChooseForesight2.class);
            }
            return PossibleAction.mandatory(Action.ChooseForesight1.class);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public static class TeepeeLocation extends Location {

        @Getter
        private final int reward; // 0 == exchange token
        private Teepee teepee;

        TeepeeLocation(String name, int reward, Location... next) {
            super(name, next);
            this.reward = reward;
        }

        @Override
        public PossibleAction activate(GWT game) {
            if (teepee == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }
            return PossibleAction.optional(Action.SingleAuxiliaryAction.class);
        }

        @Override
        public boolean isEmpty() {
            return teepee == null;
        }

        public boolean isExchangeToken() {
            return reward == 0;
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

        Teepee removeTeepee() {
            if (this.teepee == null) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }

            var teepee = this.teepee;

            this.teepee = null;

            return teepee;
        }
    }
}
