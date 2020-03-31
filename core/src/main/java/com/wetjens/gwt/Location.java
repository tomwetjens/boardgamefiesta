package com.wetjens.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

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

    Optional<PossibleAction> getPossibleAction() {
        return Optional.empty();
    }

    public Fee getFee() {
        return Fee.NONE;
    }

    abstract boolean isEmpty();

    @Override
    public String toString() {
        return name;
    }

    public boolean isDirect(Location to) {
        return next.contains(to) || (isEmpty() && next.stream().anyMatch(between -> between.isDirect(to)));
    }

    public static final class Start extends Location {

        public Start(Location... next) {
            super("START", next);
        }

        @Override
        boolean isEmpty() {
            return false;
        }
    }

    public static final class BuildingLocation extends Location {

        private final Class<? extends Action> riskAction;

        private Building building;

        BuildingLocation(String name, Location... next) {
            super(name, next);

            this.riskAction = null;
        }

        BuildingLocation(String name, Class<? extends Action> riskAction, Location... next) {
            super(name, next);

            this.riskAction = riskAction;
        }

        @Override
        Optional<PossibleAction> getPossibleAction() {
            if (building != null) {
                if (riskAction != null) {
                    return Optional.of(PossibleAction.optional(PossibleAction.choice(building.getPossibleAction(), riskAction, Action.SingleAuxiliaryAction.class)));
                } else {
                    return Optional.of(PossibleAction.optional(PossibleAction.choice(building.getPossibleAction(), Action.SingleAuxiliaryAction.class)));
                }
            }
            return Optional.empty();
        }

        @Override
        public Fee getFee() {
            if (building != null) {
                return building.getFee();
            }
            return Fee.NONE;
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
    }

    public static final class HazardLocation extends Location {

        @NonNull
        @Getter
        private final HazardType type;

        private Hazard hazard;

        HazardLocation(@NonNull HazardType type, int number, Location... next) {
            super(type + "-" + number, next);
            this.type = type;
        }

        Optional<Hazard> getHazard() {
            return Optional.ofNullable(hazard);
        }

        @Override
        Optional<PossibleAction> getPossibleAction() {
            if (hazard != null) {
                return Optional.of(PossibleAction.optional(Action.SingleAuxiliaryAction.class));
            }
            return Optional.empty();
        }

        @Override
        public Fee getFee() {
            if (hazard != null) {
                return hazard.getFee();
            }
            return Fee.NONE;
        }

        @Override
        boolean isEmpty() {
            return hazard == null;
        }

        void placeHazard(@NonNull Hazard hazard) {
            if (this.hazard != null) {
                throw new IllegalStateException("Location already has a hazard");
            }

            if (hazard.getType() != this.type) {
                throw new IllegalArgumentException("Hazard must be of type " + this.type);
            }

            this.hazard = hazard;
        }

        void removeHazard() {
            if (this.hazard == null) {
                throw new IllegalStateException("No hazard at location");
            }
            this.hazard = null;
        }
    }

    public static final class KansasCity extends Location {

        KansasCity(Location... next) {
            super("KANSAS_CITY", next);
        }

        @Override
        Optional<PossibleAction> getPossibleAction() {
            return Optional.of(PossibleAction.mandatory(Action.ChooseForesights.class));
        }

        @Override
        boolean isEmpty() {
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
        public Optional<PossibleAction> getPossibleAction() {
            if (teepee != null) {
                return Optional.of(PossibleAction.any(Action.SingleAuxiliaryAction.class));
            }
            return Optional.empty();
        }

        @Override
        boolean isEmpty() {
            return teepee == null;
        }

        public Optional<Teepee> getTeepee() {
            return Optional.ofNullable(teepee);
        }

        public void placeTeepee(@NonNull Teepee teepee) {
            if (this.teepee != null) {
                throw new IllegalStateException("Location already has a teepee");
            }
            this.teepee = teepee;
        }

        public void removeTeepee() {
            if (this.teepee == null) {
                throw new IllegalStateException("No teepee at location");
            }
            this.teepee = null;
        }
    }
}
