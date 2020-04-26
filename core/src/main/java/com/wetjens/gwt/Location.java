package com.wetjens.gwt;

import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public abstract class Location implements Serializable {

    private static final long serialVersionUID = 1L;

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

    ImmediateActions activate(Game game, Player player) {
        return ImmediateActions.none();
    }

    public Hand getHand() {
        return Hand.NONE;
    }

    abstract boolean isEmpty();

    @Override
    public String toString() {
        return name;
    }

    public boolean isDirect(Location to) {
        return next.stream().anyMatch(between -> between == to || (between.isEmpty() && between.isDirect(to)));
    }

    public static final class Start extends Location {

        private static final long serialVersionUID = 1L;

        Start(Location... next) {
            super("START", next);
        }

        @Override
        boolean isEmpty() {
            return false;
        }
    }

    @Getter
    public static final class BuildingLocation extends Location {

        private static final long serialVersionUID = 1L;

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
        ImmediateActions activate(Game game, Player player) {
            if (building != null) {
                if (riskAction != null) {
                    return ImmediateActions.of(PossibleAction.any(PossibleAction.choice(building.getPossibleAction(game), Action.SingleAuxiliaryAction.class), riskAction));
                } else {
                    return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(building.getPossibleAction(game), Action.SingleAuxiliaryAction.class)));
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
        boolean isEmpty() {
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

        private static final long serialVersionUID = 1L;

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
        ImmediateActions activate(Game game, Player player) {
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
        boolean isEmpty() {
            return hazard == null;
        }

        void placeHazard(@NonNull Hazard hazard) {
            if (this.hazard != null) {
                throw new GWTException(GWTError.LOCATION_NOT_EMPTY);
            }

            if (hazard.getType() != this.type) {
                throw new GWTException(GWTError.HAZARD_MUST_BE_OF_TYPE, type);
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

    public static final class KansasCity extends Location {

        private static final long serialVersionUID = 1L;

        KansasCity(Location... next) {
            super("KANSAS_CITY", next);
        }

        @Override
        ImmediateActions activate(Game game, Player player) {
            return ImmediateActions.of(PossibleAction.mandatory(Action.ChooseForesights.class));
        }

        @Override
        boolean isEmpty() {
            return false;
        }
    }

    public static final class TeepeeLocation extends Location {

        private static final long serialVersionUID = 1L;

        @Getter
        private final int reward;
        private Teepee teepee;

        TeepeeLocation(int reward, Location... next) {
            super("TEEPEE-" + reward, next);
            this.reward = reward;
        }

        @Override
        public ImmediateActions activate(Game game, Player player) {
            if (teepee != null) {
                return ImmediateActions.of(PossibleAction.optional(Action.SingleAuxiliaryAction.class));
            }
            return ImmediateActions.none();
        }

        @Override
        boolean isEmpty() {
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
