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

import com.boardgamefiesta.api.domain.Player;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PROTECTED)// For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
@Getter
public class Animal {

    private static final int MAX_ELEMENTS = 6;

    Player player;

    AnimalType type;

    int genePool;
    int eliminatedSpecies;

    int actionPawns;

    int score;

    @Builder.Default
    List<ElementType> elements = new ArrayList<>();

    static Animal initial(Player player, AnimalType type, int playerCount) {
        return Animal.builder()
                .player(player)
                .type(type)
                .genePool(initialGenePool(playerCount) - 3) // 3 less that are added to tiles initially
                .actionPawns(initialActionPawns(playerCount))
                .score(0)
                .elements(new ArrayList<>(type.getInitialElements()))
                .build();
    }

    static int initialActionPawns(int playerCount) {
        switch (playerCount) {
            case 2:
                return 7;
            case 3:
                return MAX_ELEMENTS;
            case 4:
                return 5;
            case 5:
                return 4;
            default:
                return 3;
        }
    }

    static int initialGenePool(int playerCount) {
        switch (playerCount) {
            case 2:
                return 55;
            case 3:
                return 50;
            case 4:
                return 45;
            case 5:
                return 40;
            default:
                return 35;
        }
    }

    void removeActionPawn() {
        if (actionPawns == 0) {
            throw new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN);
        }
        actionPawns--;
    }

    void addActionPawn() {
        actionPawns++;
    }

    void addActionPawns(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be positive: " + amount);
        actionPawns += amount;
    }

    void addElement(ElementType elementType) {
        if (!canAddElement()) {
            throw new DominantSpeciesException(DominantSpeciesError.MAX_ELEMENTS_REACHED);
        }
        elements.add(elementType);
    }

    boolean hasActionPawn() {
        return actionPawns > 0;
    }

    void addVPs(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be positive: " + amount);
        score += amount;
    }

    int loseVPs(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be positive: " + amount);

        var lost = Math.min(amount, score);
        score -= lost;

        return lost;
    }

    boolean hasElement(ElementType elementType) {
        return elements.contains(elementType);
    }

    boolean canRemoveElement() {
        return elements.size() > type.getInitialElements().size();
    }

    void addSpeciesToGenePool(int species) {
        if (species < 0) throw new IllegalArgumentException("amount must be positive: " + species);
        genePool += species;
    }

    void removeSpeciesFromGenePool() {
        removeSpeciesFromGenePool(1);
    }

    void removeSpeciesFromGenePool(int species) {
        if (genePool < species) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_ENOUGH_SPECIES_IN_GENE_POOL);
        }
        genePool -= species;
    }

    void addEliminatedSpecies() {
        addEliminatedSpecies(1);
    }

    void addEliminatedSpecies(int species) {
        eliminatedSpecies += species;
    }

    void removeEliminatedSpecies(int species) {
        if (eliminatedSpecies < species) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_ENOUGH_ELIMINATED_SPECIES);
        }
        eliminatedSpecies -= species;
    }

    void removeElement(ElementType elementType) {
        if (!canRemoveElement(elementType)) {
            throw new DominantSpeciesException(DominantSpeciesError.CANNOT_REMOVE_ELEMENT);
        }

        elements.remove(elementType);
    }

    boolean canRemoveElement(ElementType elementType) {
        var copy = new ArrayList<>(elements);
        if (!copy.remove(elementType)) {
            return false;
        }
        return copy.size() >= type.getInitialElements().size()
                && copy.subList(0, type.getInitialElements().size()).equals(type.getInitialElements());
    }

    Set<ElementType> getRemovableElementTypes() {
        return Set.copyOf(getRemovableElements());
    }

    List<ElementType> getRemovableElements() {
        return elements.subList(type.getInitialElements().size(), elements.size());
    }

    int getNumberOfElements() {
        return elements.size();
    }

    int matchElements(List<ElementType> adjacentElements) {
        return elements.stream()
                .mapToInt(elementOnAnimal -> (int) adjacentElements.stream()
                        .filter(adjacentElement -> adjacentElement == elementOnAnimal)
                        .count())
                .sum();
    }

    boolean canRemoveOneOfElementTypes(Set<ElementType> elementTypes) {
        return !Collections.disjoint(getRemovableElementTypes(), elementTypes);
    }

    boolean canAddElement() {
        return elements.size() < MAX_ELEMENTS;
    }

    void leave() {
        score = 0;
    }
}

