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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
public class Tile implements Cloneable {

    TileType type;

    boolean tundra;

    Map<AnimalType, Integer> species;
    Map<AnimalType, Integer> hibernating; // species that ignore extinction

    AnimalType dominant;

    static Tile initial(@NonNull TileType type, boolean tundra) {
        return new Tile(type, tundra, new HashMap<>(), new HashMap<>(), null);
    }

    @Override
    public Tile clone() {
        return new Tile(type, tundra, new HashMap<>(species), new HashMap<>(hibernating), dominant);
    }

    boolean hasSpecies(@NonNull AnimalType animalType) {
        return getSpecies(animalType) > 0;
    }

    void removeSpecies(@NonNull AnimalType animalType) {
        removeSpecies(animalType, 1);
    }

    void removeSpecies(@NonNull AnimalType animalType, int species) {
        if (species <= 0) {
            throw new DominantSpeciesException(DominantSpeciesError.SPECIES_MUST_BE_POSITIVE);
        }

        var count = getSpecies(animalType);

        if (count < species) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_ENOUGH_SPECIES_ON_TILE);
        }

        var newCount = count - species;
        this.species.put(animalType, newCount);
        if (this.hibernating != null) {
            this.hibernating.compute(animalType, (k, hib) -> Math.min(newCount, hib != null ? hib : 0));
        }
    }

    void removeAllSpecies(AnimalType animalType) {
        this.species.remove(animalType);
        this.hibernating.remove(animalType);
    }

    void addSpecies(@NonNull AnimalType animalType) {
        addSpecies(animalType, 1);
    }

    void addSpecies(AnimalType animalType, int species) {
        if (species <= 0) {
            throw new DominantSpeciesException(DominantSpeciesError.SPECIES_MUST_BE_POSITIVE);
        }

        this.species.put(animalType, getSpecies(animalType) + species);
    }

    void addHibernatingSpecies(AnimalType animalType, int species) {
        addSpecies(animalType, species);
        this.hibernating.compute(animalType, (k, existing) -> (existing != null ? existing : 0) + species);
    }

    int getTotalSpecies() {
        return species.values().stream().reduce(Integer::sum).orElse(0);
    }

    int getSpecies(@NonNull AnimalType animalType) {
        var count = species.get(animalType);
        return count != null ? count : 0;
    }

    public Optional<AnimalType> getDominant() {
        return Optional.ofNullable(dominant);
    }

    void recalculateDominance(@NonNull Collection<Animal> animals, @NonNull List<ElementType> adjacentElements) {
        var matches = animals.stream()
                .filter(animal -> hasSpecies(animal.getType()))
                .collect(Collectors.toMap(Animal::getType, animal -> animal.matchElements(adjacentElements)));

        var max = matches.values().stream().max(Integer::compare).orElse(0);

        this.dominant = null;

        if (!isEndangered(max)) {
            var maxAnimals = matches.entrySet().stream()
                    .filter(entry -> max.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (maxAnimals.size() == 1) {
                this.dominant = maxAnimals.get(0);
            }
        }
    }

    static boolean isEndangered(int matchingElements) {
        return matchingElements == 0;
    }

    Map<AnimalType, Integer> score() {
        var ranking = species.keySet().stream()
                .filter(this::hasSpecies)
                .sorted(Comparator.<AnimalType>comparingInt(species::get)
                        .thenComparingInt(AnimalType.FOOD_CHAIN_ORDER::indexOf))
                .collect(Collectors.toList());

        return IntStream.range(0, ranking.size())
                .filter(place -> place != 1 || !tundra)
                .boxed()
                .collect(Collectors.toMap(ranking::get, place -> DominantSpecies.tileScore(type, place)));
    }

    void glaciate() {
        if (tundra) {
            throw new DominantSpeciesException(DominantSpeciesError.ALREADY_TUNDRA);
        }
        tundra = true;
    }

    int getEndangeredSpecies(@NonNull Animal animal, @NonNull List<ElementType> adjacentElements) {
        var species = getSpecies(animal.getType());

        if (species > 0) {
            var matching = animal.matchElements(adjacentElements);

            if (isEndangered(matching)) {
                return species;
            }
        }

        return 0;
    }

    public boolean hasOpposingSpecies(AnimalType animalType) {
        return species.entrySet().stream()
                .anyMatch(entry -> entry.getKey() != animalType && entry.getValue() > 0);
    }

    /**
     * @return eliminated species
     */
    int extinction(Animal animal, List<ElementType> adjacentElementTypes, int speciesToSave) {
        var endangered = getEndangeredSpecies(animal, adjacentElementTypes);

        // Ignore species that were placed through the Hibernation action this turn
        var hibernating = this.hibernating != null ? this.hibernating.getOrDefault(animal.getType(), 0) : 0;
        var species = Math.max(0, endangered - hibernating - speciesToSave);

        if (species > 0) {
            removeSpecies(animal.getType(), species);
        }

        stopHibernating(animal);

        return species;
    }

    private void stopHibernating(Animal animal) {
        if (this.hibernating != null) {
            this.hibernating.remove(animal.getType());
        }
    }

}
