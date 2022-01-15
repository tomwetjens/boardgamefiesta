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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class PossibleAction {

    @Getter
    private AnimalType animal;

    private Class<? extends Action> mandatory;
    private Class<? extends Action> optional;
    private List<Class<? extends Action>> choice;

    static PossibleAction mandatory(@NonNull AnimalType animal, @NonNull Class<? extends Action> action) {
        return new PossibleAction(animal, action, null, null);
    }

    static PossibleAction optional(@NonNull AnimalType animal, @NonNull Class<? extends Action> action) {
        return new PossibleAction(animal, null, action, null);
    }

    static PossibleAction choice(@NonNull AnimalType animal, @NonNull Class<? extends Action>... choices) {
        return new PossibleAction(animal, null, null, Arrays.asList(choices));
    }

    boolean canSkip() {
        return optional != null;
    }

    boolean canPerform(Class<? extends Action> action) {
        return action.equals(mandatory)
                || action.equals(optional)
                || (choice != null && choice.contains(action));
    }

    public List<Class<? extends Action>> getActions() {
        if (mandatory != null) return Collections.singletonList(mandatory);
        if (optional != null) return Collections.singletonList(optional);
        return Collections.unmodifiableList(choice);
    }

    boolean canBePerformedBy(AnimalType animalType) {
        return this.animal == animalType;
    }

}
