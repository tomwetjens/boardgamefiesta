/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.dominantspecies.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ActionType {

    INITIATIVE(null, 1, 1, null, ActionType::initiative, null),
    ADAPTATION(Action.Adaptation.class, 3, 3, null, null, null),
    REGRESSION(Action.Regression.class, 3, 3, new FreeActionPawn(AnimalType.REPTILES, 2), Action.Regression::activate, null),
    ABUNDANCE(Action.Abundance.class, 2, 2, null, null, null),
    WASTELAND(Action.Wasteland.class, 1, 1, null, null, DominantSpecies::removeAllWastelandElementsFromTundraTiles),
    DEPLETION(Action.Depletion.class, 1, 1, null, null, null),
    GLACIATION(Action.Glaciation.class, 4, 1, null, null, null),
    SPECIATION(Action.Speciation.class, 7, 7, new FreeActionPawn(AnimalType.INSECTS, 6), null, null),
    WANDERLUST(Action.Wanderlust.class, 3, 3, null, null, null),
    MIGRATION(Action.Migration.class, 6, 6, null, null, null),
    COMPETITION(Action.Competition.class, 8, 8, new FreeActionPawn(AnimalType.ARACHNIDS, 0), null, null),
    DOMINATION(Action.Domination.class, 5, 5, null, null, null);

    public static final List<ActionType> EXECUTION_ORDER = Arrays.asList(
            INITIATIVE, ADAPTATION, REGRESSION, ABUNDANCE, WASTELAND, DEPLETION, GLACIATION, SPECIATION,
            WANDERLUST, MIGRATION, COMPETITION, DOMINATION);

    @Getter
    private final Class<? extends Action> action;
    private final int capacity;
    @Getter
    private final int executable;
    @Getter
    private final FreeActionPawn freeActionPawn;
    private final Function<DominantSpecies, FollowUpActions> onActivate;
    private final Consumer<DominantSpecies> onDeactivate;

    public static Optional<ActionType> forAction(Class<? extends Action> action) {
        for (var actionType : values()) {
            if (actionType.getAction().equals(action)) {
                return Optional.of(actionType);
            }
        }
        return Optional.empty();
    }

    void deactivate(DominantSpecies game) {
        if (onDeactivate != null) {
            onDeactivate.accept(game);
        }
    }

    FollowUpActions activate(DominantSpecies game) {
        if (onActivate != null) {
            return onActivate.apply(game);
        }
        return FollowUpActions.none();
    }

    private static FollowUpActions initiative(DominantSpecies game) {
        return game.getActionDisplay().getLeftMostExecutableActionPawn(ActionType.INITIATIVE)
                .map(actionPawn -> {
                    game.getActionDisplay().removeLeftMostActionPawn(ActionType.INITIATIVE);

                    var index = game.moveForwardOnInitiative(actionPawn.getAnimalType());

                    game.fireEvent(Event.Type.INITIATIVE, List.of(index + 1));

                    return FollowUpActions.of(List.of(PossibleAction.mandatory(actionPawn.getAnimalType(), Action.PlaceActionPawn.class)));
                })
                .orElse(FollowUpActions.none());
    }

    boolean isFreeActionPawn(int index) {
        return freeActionPawn != null && freeActionPawn.getIndex() == index;
    }

    int getCapacity(Set<AnimalType> playingAnimals) {
        if (freeActionPawn == null) {
            return capacity;
        }
        return playingAnimals.contains(freeActionPawn.getAnimalType()) ? capacity : capacity - 1;
    }

    public boolean hasActivation() {
        return onActivate != null;
    }

    @Value
    public static class FreeActionPawn {
        AnimalType animalType;
        int index;
    }
}
