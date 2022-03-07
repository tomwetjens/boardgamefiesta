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
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
@Slf4j
public class ActionDisplay {

    @Getter
    private Map<ActionType, AnimalType[]> actionPawns;

    @Getter
    private Map<ActionType, List<ElementType>> elements;

    private ActionType executing;

    static ActionDisplay initial(Set<AnimalType> playingAnimals, DrawBag drawBag, Random random) {
        var actionPawns = Arrays.stream(ActionType.values())
                .collect(Collectors.toMap(Function.identity(), type -> new AnimalType[type.getCapacity(playingAnimals)]));

        var elements = Map.of(
                ActionType.ADAPTATION, new ArrayList<ElementType>(4),
                ActionType.REGRESSION, new ArrayList<ElementType>(4),
                ActionType.ABUNDANCE, new ArrayList<ElementType>(4),
                ActionType.WASTELAND, new ArrayList<ElementType>(4),
                ActionType.DEPLETION, new ArrayList<ElementType>(4),
                ActionType.WANDERLUST, (List<ElementType>) new ArrayList<ElementType>(4)
        );

        var actionDisplay = new ActionDisplay(actionPawns, elements, null);

        actionDisplay.drawElements(drawBag, random);
        actionDisplay.resetFreeActionPawns(playingAnimals);

        return actionDisplay;
    }

    void drawElements(DrawBag drawBag, Random random) {
        elements.get(ActionType.ADAPTATION).addAll(drawBag.draw(4, random));
        elements.get(ActionType.ABUNDANCE).addAll(drawBag.draw(4, random));
        elements.get(ActionType.WANDERLUST).addAll(drawBag.draw(4, random));
    }

    ActionDisplay resetFreeActionPawns(Set<AnimalType> playingAnimals) {
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
        var spaces = actionPawns.get(actionType);
        return IntStream.range(0, Math.min(spaces.length, actionType.getExecutable()))
                .filter(index -> spaces[index] != null)
                .mapToObj(index -> new ActionPawn(spaces[index], actionType, index))
                .findFirst();
    }

    Optional<ActionPawn> getLeftMostExecutableActionPawn() {
        return getLeftMostExecutableActionPawn(executing);
    }

    ActionDisplay placeActionPawn(AnimalType animalType, ActionType actionType, int index) {
        var spaces = actionPawns.get(actionType);

        if (index < 0 || index >= spaces.length) {
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

        for (var i = 0; i < spaces.length; i++) {
            if (spaces[i] != null) {
                spaces[i] = null;
                return !actionType.isFreeActionPawn(i);
            }
        }

        throw new DominantSpeciesException(DominantSpeciesError.ACTION_SPACE_EMPTY);
    }

    boolean removeLeftMostActionPawn() {
        return removeLeftMostActionPawn(executing);
    }

    /**
     * @return number of removed action pawns that were not free action pawns
     */
    int removeActionPawns(ActionType actionType, AnimalType animalType) {
        var spaces = actionPawns.get(actionType);

        var count = 0;

        for (var i = 0; i < spaces.length; i++) {
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
        return IntStream.range(0, pawns.length)
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

        if (getLeftMostExecutableActionPawn(executing).isPresent()) {
            game.fireEvent(Event.Type.EXECUTING, List.of(executing));
        }

        var followUpActions = executing.activate(game);
        while (followUpActions.isEmpty()) {
            followUpActions = nextActionType(game);
        }

        return followUpActions;
    }

    private FollowUpActions nextActionType(DominantSpecies game) {
        log.debug("Moving to next Action after {}", executing);

        var followUpActions = FollowUpActions.none();

        do {
            log.debug("Deactivating Action {}", executing);
            executing.deactivate(game);

            var index = ActionType.EXECUTION_ORDER.indexOf(executing);
            if (index == ActionType.EXECUTION_ORDER.size() - 1) {
                log.debug("End of Actions reached");
                return FollowUpActions.none();
            }

            executing = ActionType.EXECUTION_ORDER.get(index + 1);
            log.debug("Activating Action {}", executing);

            if (executing.hasActivation()) {
                game.fireEvent(Event.Type.EXECUTING, List.of(executing));
            }
            followUpActions = executing.activate(game);

            if (followUpActions.isEmpty()) {
                log.debug("No follow up actions from activation of Action {}, so move to first Action Pawn", executing);
                followUpActions = nextActionPawn(game);

                if (!followUpActions.isEmpty() && !executing.hasActivation()) {
                    game.fireEvent(Event.Type.EXECUTING, List.of(executing));
                } else {
                    log.debug("No Action Pawns on {}, moving to next action type", executing);
                }
            }
        } while (followUpActions.isEmpty());

        return followUpActions;
    }

    Optional<ActionPawn> getNextActionPawn() {
        var actionType = executing;

        Optional<ActionPawn> actionPawn;
        do {
            actionPawn = getLeftMostExecutableActionPawn(actionType);

            if (actionPawn.isEmpty()) {
                var index = ActionType.EXECUTION_ORDER.indexOf(actionType);
                if (index == ActionType.EXECUTION_ORDER.size() - 1) {
                    return Optional.empty();
                }

                actionType = ActionType.EXECUTION_ORDER.get(index + 1);
            }
        } while (actionPawn.isEmpty());

        return actionPawn;
    }

    FollowUpActions nextActionPawn(DominantSpecies game) {
        log.debug("Moving to next Action Pawn of {}", executing);
        return getLeftMostExecutableActionPawn(executing)
                .map(ActionPawn::toFollowUpActions)
                .orElseGet(() -> {
                    log.debug("No more Action Pawns on {}, moving to next Action", executing);
                    return nextActionType(game);
                });
    }

    int getNumberOfActionPawns(ActionType actionType, AnimalType animalType) {
        return (int) Arrays.stream(actionPawns.get(actionType))
                .filter(ap -> ap == animalType)
                .count();
    }

    List<ElementType> getElements(ActionType actionType) {
        return elements.get(actionType);
    }

    void slideGlaciationActionPawnsLeft() {
        var actionPawns = this.actionPawns.get(ActionType.GLACIATION);

        for (var i = 0; i < actionPawns.length - 1; i++) {
            if (actionPawns[i] == null) {
                actionPawns[i] = actionPawns[i + 1];
                actionPawns[i + 1] = null;
            }
        }
    }

    List<ElementType> removeAllElements(ActionType actionType) {
        var removed = List.copyOf(elements.get(actionType));
        elements.get(actionType).clear();
        return removed;
    }

    void slideElementsDown() {
        var wastelandBox = elements.get(ActionType.WASTELAND);
        var depletionBox = elements.get(ActionType.DEPLETION);
        depletionBox.addAll(wastelandBox);
        wastelandBox.clear();

        var abundanceBox = elements.get(ActionType.ABUNDANCE);
        wastelandBox.addAll(abundanceBox);
        abundanceBox.clear();

        var adaptationBox = elements.get(ActionType.ADAPTATION);
        var regressionBox = elements.get(ActionType.REGRESSION);
        regressionBox.addAll(adaptationBox);
        adaptationBox.clear();
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
            return FollowUpActions.of(List.of(PossibleAction.optional(animalType, actionType.getAction())));
        }

        boolean isFree() {
            return actionType.isFreeActionPawn(index);
        }
    }
}
