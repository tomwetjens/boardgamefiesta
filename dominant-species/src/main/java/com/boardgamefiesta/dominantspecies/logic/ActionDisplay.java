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

import lombok.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class ActionDisplay {

    @Getter
    private Map<ActionType, AnimalType[]> actionPawns;

    @Getter
    private Map<ActionType, List<ElementType>> elements;

    private ActionType executing;

    static ActionDisplay initial(Set<AnimalType> playingAnimals, DrawBag drawBag, Random random) {
        var actionPawns = Arrays.stream(ActionType.values())
                .collect(Collectors.toMap(Function.identity(), type -> new AnimalType[type.getCapacity()]));

        var elements = Map.of(
                ActionType.ADAPTATION, drawBag.draw(4, random),
                ActionType.REGRESSION, new ArrayList<ElementType>(4),
                ActionType.ABUNDANCE, drawBag.draw(4, random),
                ActionType.WASTELAND, new ArrayList<ElementType>(4),
                ActionType.DEPLETION, new ArrayList<ElementType>(4),
                ActionType.WANDERLUST, drawBag.draw(4, random)
        );

        var actionDisplay = new ActionDisplay(actionPawns, elements, null);

        actionDisplay.reset(playingAnimals);

        return actionDisplay;
    }

    ActionDisplay reset(Set<AnimalType> playingAnimals) {
        for (var actionType : ActionType.values()) {
            var pawns = actionPawns.get(actionType);

            Arrays.fill(pawns, null);

            if (actionType.getFreeActionPawn() != null) {
                var freeAction = actionType.getFreeActionPawn();
                if (playingAnimals.contains(freeAction.getAnimalType())) {
                    pawns[freeAction.getIndex()] = freeAction.getAnimalType();
                }
            }
        }
        return this;
    }

    Optional<ActionPawn> getLeftMostExecutableActionPawn(ActionType actionType) {
        var pawns = actionPawns.get(actionType);
        return IntStream.range(0, actionType.getExecutable())
                .filter(index -> pawns[index] != null)
                .mapToObj(index -> new ActionPawn(pawns[index], actionType, index))
                .findFirst();
    }

    ActionDisplay placeActionPawn(AnimalType animalType, ActionType actionType, int index) {
        var spaces = actionPawns.get(actionType);

        if (index < 0 || index >= actionType.getCapacity()) {
            throw new DominantSpeciesException(DominantSpeciesError.INVALID_ACTION_SPACE);
        }

        if (spaces[index] != null) {
            throw new DominantSpeciesException(DominantSpeciesError.ACTION_SPACE_NOT_EMPTY);
        }

        spaces[index] = animalType;

        return this;
    }

    /**
     * @return <code>true</code> if the AP was not a free action and must be added back to the animal
     */
    boolean removeLeftMostActionPawn(ActionType actionType) {
        var spaces = actionPawns.get(actionType);

        for (var i = 0; i < actionType.getCapacity(); i++) {
            if (spaces[i] != null) {
                spaces[i] = null;
                return !actionType.isFreeActionPawn(i);
            }
        }

        throw new DominantSpeciesException(DominantSpeciesError.ACTION_SPACE_EMPTY);
    }

    /**
     * @return number of removed action pawns that were not free action pawns
     */
    int removeActionPawns(ActionType actionType, AnimalType animalType) {
        var spaces = actionPawns.get(actionType);

        var count = 0;

        for (var i = 0; i < actionType.getCapacity(); i++) {
            if (spaces[i] == animalType) {
                spaces[i] = null;
                if (actionType.getFreeActionPawn() == null
                        || actionType.getFreeActionPawn().getAnimalType() != animalType
                        || actionType.getFreeActionPawn().getIndex() != i) {
                    count++;
                }
            }
        }

        return count;
    }

    public Stream<PossibleSpace> possiblePlacements() {
        return ActionType.EXECUTION_ORDER.stream()
                .flatMap(this::possiblePlacements);
    }

    private Stream<PossibleSpace> possiblePlacements(ActionType actionType) {
        var pawns = actionPawns.get(actionType);
        return IntStream.range(0, actionType.getCapacity())
                .filter(index -> pawns[index] == null)
                .mapToObj(index -> new PossibleSpace(actionType, index));
    }

    void removeElement(ActionType actionType, ElementType elementType) {
        if (!elements.get(actionType).remove(elementType)) {
            throw new DominantSpeciesException(DominantSpeciesError.ELEMENT_NOT_AVAILABLE);
        }
    }

    ActionDisplay addElement(ActionType actionType, ElementType elementType) {
        elements.get(actionType).add(elementType);
        return this;
    }

    FollowUpActions startAtInitiative(DominantSpecies game) {
        executing = ActionType.INITIATIVE;

        var followUpActions = executing.activate(game);
        while (followUpActions.isEmpty()) {
            followUpActions = nextActionType(game);
        }

        return followUpActions;
    }

    private FollowUpActions nextActionType(DominantSpecies game) {
        var followUpActions = FollowUpActions.none();

        do {
            executing.deactivate(game);

            var index = ActionType.EXECUTION_ORDER.indexOf(executing);
            if (index == ActionType.EXECUTION_ORDER.size() - 1) {
                return followUpActions;
            }

            executing = ActionType.EXECUTION_ORDER.get(index + 1);

            followUpActions = executing.activate(game);

            if (followUpActions.isEmpty()) {
                followUpActions = nextActionPawn(game);
            }
        } while (followUpActions.isEmpty());

        return followUpActions;
    }

    FollowUpActions nextActionPawn(DominantSpecies game) {
        return getLeftMostExecutableActionPawn(executing)
                .map(ActionPawn::toFollowUpActions)
                .orElseGet(() -> nextActionType(game));
    }

    int getNumberOfActionPawns(ActionType actionType, AnimalType animalType) {
        return (int) Arrays.stream(actionPawns.get(actionType))
                .filter(ap -> ap == animalType)
                .count();
    }

    @Value
    public static class PossibleSpace {
        ActionType actionType;
        int index;
    }

    @Value
    public static class ActionPawn {
        AnimalType animalType;
        ActionType actionType;
        int index;

        FollowUpActions toFollowUpActions() {
            return FollowUpActions.of(List.of(PossibleAction.mandatory(animalType, actionType.getAction())));
        }

        boolean isFree() {
            return actionType.isFreeActionPawn(index);
        }
    }
}
