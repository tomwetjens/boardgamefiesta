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

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class ActionQueue {

    @Singular
    private List<PossibleAction> actions;

    static ActionQueue initial(PossibleAction... possibleAction) {
        return new ActionQueue(new LinkedList<>(Arrays.asList(possibleAction)));
    }

    static ActionQueue create() {
        return new ActionQueue(new LinkedList<>());
    }

    Optional<AnimalType> getNextAnimal() {
        return getNextPossibleAction().map(PossibleAction::getAnimal);
    }

    void perform(AnimalType animalType, Action action) {
        var possibleAction = getNextPossibleAction()
                .filter(pa -> pa.canBePerformedBy(animalType))
                .filter(pa -> pa.canPerform(action.getClass()))
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.CANNOT_PERFORM_ACTION));

        actions.remove(possibleAction);
    }


    public boolean canSkip() {
        return getNextPossibleAction().map(PossibleAction::canSkip).orElse(false);
    }

    PossibleAction skip() {
        var possibleAction = getNextPossibleAction()
                .filter(PossibleAction::canSkip)
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.CANNOT_SKIP_ACTION));

        actions.remove(possibleAction);

        return possibleAction;
    }

    Optional<PossibleAction> getNextPossibleAction() {
        return actions.isEmpty() ? Optional.empty() : Optional.of(actions.get(0));
    }

    boolean canPerform(AnimalType animalType, Class<? extends Action> action) {
        return getNextPossibleAction()
                .filter(possibleAction -> possibleAction.canBePerformedBy(animalType))
                .filter(possibleAction -> possibleAction.canPerform(action))
                .isPresent();
    }

    void add(PossibleAction possibleAction) {
        actions.add(possibleAction);
    }


    void addAll(Collection<PossibleAction> possibleActions) {
        actions.addAll(possibleActions);
    }

    boolean hasActions(AnimalType animalType) {
        return getNextPossibleAction()
                .filter(possibleAction -> possibleAction.canBePerformedBy(animalType))
                .isPresent();
    }

    boolean isEmpty() {
        return actions.isEmpty();
    }

    void removeAll(AnimalType animalType) {
        actions.removeIf(possibleAction -> possibleAction.getAnimal() == animalType);
    }
}