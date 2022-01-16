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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Card {

    /**
     * Place 1 element onto a sea or wetland tile. Place up to 4 species from your GP onto that same tile.
     */
    AQUATIC() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Aquatic.class));
        }
    },

    /**
     * Gain 1 VP for each tile that you share with 1 or more opposing species.
     */
    BIODIVERSITY() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var opposingSpecies = game.getOpposingSpecies(game.getCurrentAnimal());

            game.getAnimal(game.getCurrentAnimal())
                    .addVPs((int) game.getTiles().values().stream()
                            .filter(tile -> tile.hasSpecies(game.getCurrentAnimal())
                                    && opposingSpecies.stream().anyMatch(tile::hasSpecies))
                            .count());

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Eliminate 1 species on each tile containing more species than element markers.
     */
    BIOMASS() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Biomass.class));
        }
    },

    /**
     * Remove all but 1 element marker from a tile of your choice.
     */
    BLIGHT() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Blight.class));
        }
    },

    /**
     * Select 1 tile and eliminate all but 1 species. Then eliminate 1 species on each adjacent tile.
     */
    CATASTROPHE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Catastrophe.class));
        }
    },

    /**
     * All other players must eliminate 1 of their own species from every tundra tile.
     */
    COLD_SNAP() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            // TODO Card says 'other players' but what if the other player is controlling multiple animals?
            var animals = game.getAnimals().values().stream()
                    .filter(animal -> animal.getPlayer() != game.getCurrentPlayer())
                    .collect(Collectors.toSet());

            game.getTiles().values().stream()
                    .filter(Tile::isTundra)
                    .forEach(tile -> animals.stream()
                            .filter(animal -> tile.hasSpecies(animal.getType()))
                            .forEach(animal -> {
                                tile.removeSpecies(animal.getType());
                                animal.addEliminatedSpecies();
                            }));

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Each player having more elements than you loses 1 element of their choice.
     */
    DISEASE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var currentAnimal = game.getAnimal(game.getCurrentAnimal());
            return ActionResult.undoAllowed(FollowUpActions.of(
                    AnimalType.FOOD_CHAIN_ORDER.stream() // TODO In which order do animals remove an element?
                            .filter(game::hasAnimal)
                            .map(game::getAnimal)
                            .filter(Animal::canRemoveElement)
                            .filter(animal -> animal.getNumberOfElements() > currentAnimal.getNumberOfElements())
                            .map(animal -> PossibleAction.mandatory(animal.getType(), Action.RemoveElement.class))
                            .collect(Collectors.toList())));
        }
    },

    /**
     * Gain 1 VP for each element token on the board that matches any element type of yours.
     */
    ECODIVERSITY() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getAnimals().values().forEach(animal ->
                    animal.addVPs((int) game.getElements().values().stream()
                            .filter(animal::hasElement)
                            .count()));
            return ActionResult.undoAllowed();
        }
    },

    /**
     * Select up to 2 other players and replace 1 of each of their species anywhere on the board with 1 of yours.
     */
    EVOLUTION() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.optional(game.getCurrentAnimal(), Action.Evolution.class));
        }
    },

    /**
     * Place up to 1 species from your GP onto every tile already containing 1 of your species.
     */
    FECUNDITY() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.optional(game.getCurrentAnimal(), Action.Fecundity.class));
        }
    },

    /**
     * Select 1 tile you have at least 1 of your species on and score VPs equal to the total number of species there.
     */
    FERTILE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.optional(game.getCurrentAnimal(), Action.Fertile.class));
        }
    },

    /**
     * Pick 1 element to add to the board.
     */
    HABITAT() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Habitat.class));
        }
    },

    /**
     * Place up to 5 of your eliminated species onto any 1 tile. They ignore Extinction this turn.
     */
    HIBERNATION() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Hibernation.class));
        }
    },

    /**
     * Each player scores bonus VPs based on the number of domination markers they have on the board.
     */
    ICE_AGE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var dominating = game.getTiles().values().stream()
                    .map(Tile::getDominant)
                    .flatMap(Optional::stream)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            dominating.forEach(((animalType, count) ->
                    game.getAnimal(animalType).addVPs(DominantSpecies.bonusVPs(count.intValue()))));

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Take a Glaciation action.
     */
    ICE_SHEET() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Glaciation.class));
        }
    },

    /**
     * In FC order, all players pick to lose 1 element, 1 AP or all but one of their species on each tile they’re on.
     */
    IMMIGRANTS() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(FollowUpActions.of(AnimalType.FOOD_CHAIN_ORDER.stream()
                    .filter(game::hasAnimal)
                    .map(game::getAnimal)
                    .map(animal -> animal.canRemoveElement()
                            ? PossibleAction.choice(animal.getType(), Action.RemoveElement.class, Action.RemoveActionPawn.class, Action.RemoveAllBut1SpeciesOnEachTile.class)
                            : PossibleAction.choice(animal.getType(), Action.RemoveActionPawn.class, Action.RemoveAllBut1SpeciesOnEachTile.class))
                    .collect(Collectors.toList())));
        }
    },

    /**
     * Place 1 of your APs into any vacant eyeball space.
     */
    INSTINCT() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.PlaceActionPawn.class));
        }
    },

    /**
     * You and each player above you in the FC gets 1 new AP.
     */
    INTELLIGENCE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var index = AnimalType.FOOD_CHAIN_ORDER.indexOf(game.getCurrentAnimal());

            game.getAnimals().values().stream()
                    .filter(animal -> AnimalType.FOOD_CHAIN_ORDER.indexOf(animal.getType()) >= index)
                    .forEach(Animal::addActionPawn);

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Select 1 tile and move all species from there to 1 or more adjacent tiles.
     */
    MASS_EXODUS() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.MassExodus.class));
        }
    },

    /**
     * Swap 1 of your elements with 1 of your choice from the bag.
     */
    METAMORPHOSIS() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var animal = game.getAnimal(game.getCurrentAnimal());
            return ActionResult.undoAllowed(game.getDrawBag().containsAny(animal.getRemovableElements())
                    // If player can swap for the same element, then it's basically optional
                    ? PossibleAction.optional(game.getCurrentAnimal(), Action.Metamorphosis.class)
                    : PossibleAction.mandatory(game.getCurrentAnimal(), Action.Metamorphosis.class));
        }
    },

    /**
     * Each player having more VPs than you loses VPs = to 1st place value of the tile just scored.
     */
    NICHE_BIOMES() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var lastScoredTile = game.getLastScoredTile()
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_TILE_SCORED));

            var firstPlaceScore = DominantSpecies.tileScore(lastScoredTile.getType(), 0);

            var currentAnimal = game.getAnimal(game.getCurrentAnimal());

            game.getAnimals().values().forEach(animal -> {
                if (animal.getScore() > currentAnimal.getScore()) {
                    animal.loseVPs(-firstPlaceScore);
                }
            });

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Move your initiative marker 1 space to the left.
     */
    NOCTURNAL() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.moveForwardOnInitiative(game.getCurrentAnimal());
            return ActionResult.undoAllowed();
        }
    },

    /**
     * Gain 1 new AP.
     */
    OMNIVORE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();
            return ActionResult.undoAllowed();
        }
    },

    /**
     * You and each player below you in the FC gets 1 new AP.
     */
    PARASITISM() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var index = AnimalType.FOOD_CHAIN_ORDER.indexOf(game.getCurrentAnimal());

            game.getAnimals().values().stream()
                    .filter(animal -> AnimalType.FOOD_CHAIN_ORDER.indexOf(animal.getType()) <= index)
                    .forEach(Animal::addActionPawn);

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Eliminate 1 opposing species on every tile you occupy.
     */
    PREDATOR() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Predator.class));
        }
    },

    SYMBIOTIC() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var currentAnimal = game.getAnimal(game.getCurrentAnimal());
            var numberOfElements = currentAnimal.getNumberOfElements();

            game.getAnimals().values().forEach(animal -> {
                if (animal == currentAnimal || animal.getNumberOfElements() < numberOfElements) {
                    animal.addElement(game.getDrawBag().draw(random));
                }
            });

            return ActionResult.undoNotAllowed();
        }
    };

    static Queue<Card> initialDeck(Random random) {
        var deck = new ArrayList<>(Arrays.asList(values()));
        deck.remove(ICE_AGE);
        Collections.shuffle(deck, random);
        deck.add(ICE_AGE);
        return new LinkedList<>(deck);
    }

    static final int INITIAL_DECK_SIZE = 26;

    abstract ActionResult perform(DominantSpecies game, Random random);

}
