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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PROTECTED)// For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
@Getter
public class Animal {

    Player player;

    AnimalType type;

    int genePool;
    int eliminated;

    int actionPawns;

    int score;

    @Builder.Default
    List<ElementType> elements = new ArrayList<>();

    static Animal initial(Player player, AnimalType type, int playerCount) {
        return Animal.builder()
                .player(player)
                .type(type)
                .genePool(initialGenePool(playerCount))
                .actionPawns(initialActionPawns(playerCount))
                .score(0)
                .elements(new ArrayList<>())
                .build();
    }

    static int initialActionPawns(int playerCount) {
        switch (playerCount) {
            case 2:
                return 7;
            case 3:
                return 6;
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
        if (elements.size() == 6) {
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

    void loseVPs(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be positive: " + amount);
        score = Math.max(0, score - amount);
    }

    List<ElementType> getElements() {
        var elements = new ArrayList<>(type.getInitialElements());
        elements.addAll(this.elements);
        return elements;
    }

    boolean hasElement(ElementType elementType) {
        return type.getInitialElements().contains(elementType) || elements.contains(elementType);
    }

    boolean canRemoveElement() {
        return !elements.isEmpty();
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
        eliminated += species;
    }

    void removeEliminatedSpecies(int species) {
        if (eliminated < species) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_ENOUGH_ELIMINATED_SPECIES);
        }
        eliminated -= species;
    }

    void removeElement(ElementType elementType) {
        if (!canRemoveElement(elementType)) {
            throw new DominantSpeciesException(DominantSpeciesError.CANNOT_REMOVE_ELEMENT);
        }

        elements.remove(elementType);
    }

    boolean canRemoveElement(ElementType elementType) {
        return elements.contains(elementType);
    }

    Set<ElementType> getRemovableElements() {
        return Set.copyOf(elements);
    }

    int getNumberOfElements() {
        return type.getInitialElements().size() + elements.size();
    }

    int matchElements(List<ElementType> adjacentElements) {
        return Stream.concat(type.getInitialElements().stream(), elements.stream())
                .mapToInt(elementOnAnimal -> (int) adjacentElements.stream()
                        .filter(adjacentElement -> adjacentElement == elementOnAnimal)
                        .count())
                .sum();
    }

    void removeOneOfEachElement(List<ElementType> elementTypes) {
        elementTypes.forEach(elementType -> {
            if (canRemoveElement(elementType)) {
                removeElement(elementType);
            }
        });
    }

}

