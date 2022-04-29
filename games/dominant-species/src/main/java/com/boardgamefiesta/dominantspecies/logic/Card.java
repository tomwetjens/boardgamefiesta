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
            if (game.hasVacantCorner() && !game.getDrawBag().isEmpty()) {
                return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Aquatic.class));
            } else {
                return ActionResult.undoAllowed();
            }
        }
    },

    /**
     * Gain 1 VP for each tile that you share with 1 or more opposing species.
     */
    BIODIVERSITY() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var vps = (int) game.getTiles().values().stream()
                    .filter(tile -> tile.hasSpecies(game.getCurrentAnimal())
                            && tile.hasOpposingSpecies(game.getCurrentAnimal()))
                    .count();

            game.getAnimal(game.getCurrentAnimal()).addVPs(vps);
            game.fireEvent(Event.Type.GAIN_VPS, List.of(vps));

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Eliminate 1 species on each tile containing more species than element markers.
     */
    BIOMASS() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (!Action.Biomass.getAffectedTiles(game).isEmpty()) {
                return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Biomass.class));
            }
            return ActionResult.undoAllowed();
        }
    },

    /**
     * Remove all but 1 element marker from a tile of your choice.
     */
    BLIGHT() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (!game.getElements().isEmpty()) {
                return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Blight.class));
            }
            return ActionResult.undoAllowed();
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

            game.getTiles().entrySet().stream()
                    .filter(entry -> entry.getValue().isTundra())
                    .forEach(entry -> animals.stream()
                            .filter(animal -> entry.getValue().hasSpecies(animal.getType()))
                            .forEach(animal -> {
                                entry.getValue().removeSpecies(animal.getType());
                                animal.addEliminatedSpecies();
                                game.fireEvent(Event.Type.ELIMINATE_SPECIES, List.of(animal.getType(), 1, entry.getKey(), entry.getValue().getType()));
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
                            .filter(game::isAnimalPlaying)
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
            game.getAnimals().values().forEach(animal -> {
                var count = (int) game.getElements().values().stream()
                        .filter(animal::hasElement)
                        .count();
                animal.addVPs(count);
                game.fireEvent(animal.getType(), Event.Type.GAIN_VPS, List.of(count));
            });
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
            if (game.getAnimal(game.getCurrentAnimal()).getGenePool() > 0
                    && !game.getTilesWithSpecies(game.getCurrentAnimal()).isEmpty()) {
                return ActionResult.undoAllowed(PossibleAction.optional(game.getCurrentAnimal(), Action.Fecundity.class));
            } else {
                return ActionResult.undoAllowed();
            }
        }
    },

    /**
     * Select 1 tile you have at least 1 of your species on and score VPs equal to the total number of species there.
     */
    FERTILE() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (!game.getTilesWithSpecies(game.getCurrentAnimal()).isEmpty()) {
                return ActionResult.undoAllowed(PossibleAction.optional(game.getCurrentAnimal(), Action.Fertile.class));
            } else {
                return ActionResult.undoAllowed();
            }
        }
    },

    /**
     * Pick 1 element to add to the board.
     */
    HABITAT() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (!game.getDrawBag().isEmpty() && game.hasVacantCorner()) {
                return ActionResult.undoAllowed(PossibleAction.mandatory(game.getCurrentAnimal(), Action.Habitat.class));
            } else {
                return ActionResult.undoAllowed();
            }
        }
    },

    /**
     * Place up to 5 of your eliminated species onto any 1 tile. They ignore Extinction this turn.
     */
    HIBERNATION() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (game.getAnimal(game.getCurrentAnimal()).getEliminatedSpecies() > 0) {
                return ActionResult.undoAllowed(PossibleAction.optional(game.getCurrentAnimal(), Action.Hibernation.class));
            } else {
                return ActionResult.undoAllowed();
            }
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

            dominating.forEach((animalType, count) -> {
                var bonusVPs = DominantSpecies.bonusVPs(count.intValue());
                game.getAnimal(animalType).addVPs(bonusVPs);
                game.fireEvent(animalType, Event.Type.GAIN_BONUS_VPS, List.of(bonusVPs));
            });

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
                    .filter(game::isAnimalPlaying)
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

            game.getAnimals().entrySet().stream()
                    .filter(animal -> AnimalType.FOOD_CHAIN_ORDER.indexOf(animal.getKey()) <= index)
                    .forEach(animal -> {
                        animal.getValue().addActionPawn();
                        game.fireEvent(animal.getKey(), Event.Type.GAIN_ACTION_PAWN);
                    });

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

            var swappableElementTypes = animal.getRemovableElementTypes();

            if (game.getDrawBag().isEmpty() || swappableElementTypes.isEmpty()) {
                return ActionResult.undoAllowed();
            }

            return ActionResult.undoAllowed(
                    game.getDrawBag().containsAny(swappableElementTypes)
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
                    int lost = animal.loseVPs(firstPlaceScore);
                    game.fireEvent(animal.getType(), Event.Type.LOSE_VPS, List.of(lost));
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

            game.fireEvent(Event.Type.GAIN_ACTION_PAWN);

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

            game.getAnimals().entrySet().stream()
                    .filter(animal -> AnimalType.FOOD_CHAIN_ORDER.indexOf(animal.getKey()) >= index)
                    .forEach(animal -> {
                        animal.getValue().addActionPawn();
                        game.fireEvent(animal.getKey(), Event.Type.GAIN_ACTION_PAWN);
                    });

            return ActionResult.undoAllowed();
        }
    },

    /**
     * Eliminate 1 opposing species on every tile you occupy.
     */
    PREDATOR() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var currentAnimal = game.getCurrentAnimal();

            if (game.getTilesWithSpecies(currentAnimal)
                    .stream()
                    .map(game::getTile)
                    .map(Optional::get)
                    .anyMatch(tile -> tile.hasOpposingSpecies(currentAnimal))) {
                return ActionResult.undoAllowed(PossibleAction.mandatory(currentAnimal, Action.Predator.class));
            } else {
                return ActionResult.undoAllowed();
            }
        }
    },

    /**
     * You and each player with fewer elements than you gets 1 random element from the bag.
     */
    SYMBIOTIC() {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var currentAnimal = game.getAnimal(game.getCurrentAnimal());
            var numberOfElements = currentAnimal.getNumberOfElements();

            game.getAnimals().values().forEach(animal -> {
                if (animal == currentAnimal || animal.getNumberOfElements() < numberOfElements) {
                    if (animal.canAddElement() && !game.getDrawBag().isEmpty()) {
                        var elementType = game.getDrawBag().draw(random);
                        animal.addElement(elementType);

                        game.fireEvent(animal.getType(), Event.Type.ADD_ELEMENT_TO_ANIMAL, List.of(elementType));
                    }
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
