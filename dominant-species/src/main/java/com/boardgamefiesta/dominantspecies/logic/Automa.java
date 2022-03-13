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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Automa {

    public void perform(DominantSpecies game, Player player, Random random) {
        do {
            var possibleActions = game.possibleActions();
            if (possibleActions.isEmpty()) {
                game.endTurn(player, random);
            } else {
                var action = Optional.<Action>empty();

                if (possibleActions.contains(Action.PlaceActionPawn.class)) {
                    action = Optional.of(placeActionPawn(game, random));
                } else if (possibleActions.contains(Action.Adaptation.class)) {
                    action = adaptation(game, random);
                } else if (possibleActions.contains(Action.Regression.class)) {
                    action = regression(game, random);
                } else if (possibleActions.contains(Action.Abundance.class)) {
                    action = abundance(game, random);
                } else if (possibleActions.contains(Action.Wasteland.class)) {
                    action = wasteland(game, random);
                } else if (possibleActions.contains(Action.Depletion.class)) {
                    action = depletion(game, random);
                } else if (possibleActions.contains(Action.Glaciation.class)) {
                    action = glaciation(game, random);
                } else if (possibleActions.contains(Action.Speciation.class)) {
                    action = speciation(game, random);
                } else if (possibleActions.contains(Action.Wanderlust.class)) {
                    action = wanderlust(game, random);
                } else if (possibleActions.contains(Action.WanderlustMove.class)) {
                    action = wanderlustMove(game, random);
                } else if (possibleActions.contains(Action.Migration.class)) {
                    action = migration(game, random);
                } else if (possibleActions.contains(Action.Competition.class)) {
                    action = competition(game, random);
                } else if (possibleActions.contains(Action.Domination.class)) {
                    action = domination(game, random);
                } else if (possibleActions.contains(Action.DominanceCard.class)) {
                    action = dominanceCard(game, random);
                } else if (possibleActions.contains(Action.Aquatic.class)) {
                    action = aquatic(game, random);
                } else if (possibleActions.contains(Action.Biomass.class)) {
                    action = biomass(game, random);
                } else if (possibleActions.contains(Action.Blight.class)) {
                    action = blight(game, random);
                } else if (possibleActions.contains(Action.Catastrophe.class)) {
                    action = catastrophe(game, random);
                } else if (possibleActions.contains(Action.RemoveElement.class)) {
                    if (possibleActions.size() == 1) {
                        action = removeElement(game, random);
                    } else {
                        action = immigrants(game, random);
                    }
                } else if (possibleActions.contains(Action.RemoveActionPawn.class)) {
                    if (possibleActions.contains(Action.RemoveAllBut1SpeciesOnEachTile.class)) {
                        action = immigrants(game, random);
                    } else {
                        action = removeActionPawn();
                    }
                } else if (possibleActions.contains(Action.MassExodus.class)) {
                    action = massExodus(game, random);
                } else if (possibleActions.contains(Action.Metamorphosis.class)) {
                    action = metamorphosis(game, random);
                } else if (possibleActions.contains(Action.Predator.class)) {
                    action = predator(game, random);
                } else if (possibleActions.contains(Action.Evolution.class)) {
                    action = evolution(game, random);
                } else if (possibleActions.contains(Action.Fecundity.class)) {
                    action = fecundity(game, random);
                } else if (possibleActions.contains(Action.Fertile.class)) {
                    action = fertile(game, random);
                } else if (possibleActions.contains(Action.Habitat.class)) {
                    action = habitat(game, random);
                } else if (possibleActions.contains(Action.Hibernation.class)) {
                    action = hibernation(game, random);
                }
                // TODO Add actions of Dominance Cards

                action.ifPresentOrElse(a -> game.perform(player, a, random), () -> game.skip(player, random));
            }
        } while (game.getCurrentPlayers().contains(player));
    }

    private Optional<Action> hibernation(DominantSpecies game, Random random) {
        var animal = game.getAnimal(game.getCurrentAnimal());

        if (animal.getEliminatedSpecies() == 0) {
            return Optional.empty();
        }

        var species = random.nextInt(Math.min(animal.getEliminatedSpecies(), Action.Hibernation.MAX_SPECIES));

        if (species == 0) {
            return Optional.empty();
        }

        var possibleTiles = new ArrayList<>(game.getTiles().keySet());

        if (possibleTiles.isEmpty()) {
            return Optional.empty();
        }

        var tile = possibleTiles.get(random.nextInt(possibleTiles.size()));

        return Optional.of(new Action.Hibernation(tile, species));
    }

    private Optional<Action> habitat(DominantSpecies game, Random random) {
        if (game.getDrawBag().isEmpty()) {
            return Optional.empty();
        }

        var possibleElementTypes = Arrays.stream(ElementType.values())
                .filter(elementType -> game.getDrawBag().contains(elementType))
                .collect(Collectors.toList());

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        var elementType = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        var possibleCorners = game.getVacantCorners()
                .filter(corner -> game.getAdjacentTiles(corner)
                        .anyMatch(tile -> tile.getType() == TileType.SEA || tile.getType() == TileType.WETLAND))
                .collect(Collectors.toList());

        if (possibleCorners.isEmpty()) {
            return Optional.empty();
        }

        var corner = possibleCorners.get(random.nextInt(possibleCorners.size()));

        return Optional.of(new Action.Habitat(elementType, corner));
    }

    private Optional<Action> fertile(DominantSpecies game, Random random) {
        var tiles = new ArrayList<Hex>(game.getTilesWithSpecies(game.getCurrentAnimal()));
        var tile = tiles.get(random.nextInt(tiles.size()));
        return Optional.of(new Action.Fertile(tile));
    }

    private Optional<Action> fecundity(DominantSpecies game, Random random) {
        var genePool = game.getAnimal(game.getCurrentAnimal()).getGenePool();

        if (genePool == 0) {
            return Optional.empty();
        }

        var tiles = game.getTilesWithSpecies(game.getCurrentAnimal()).stream()
                .filter(hex -> random.nextInt(2) == 1)
                .limit(genePool)
                .collect(Collectors.toSet());

        if (tiles.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Action.Fecundity(tiles));
    }

    private Optional<Action> evolution(DominantSpecies game, Random random) {
        var possibleAnimals = game.getAnimals().keySet().stream()
                .filter(animalType -> animalType != game.getCurrentAnimal())
                .filter(animalType -> !game.getTilesWithSpecies(animalType).isEmpty())
                .collect(Collectors.toList());
        if (possibleAnimals.size() > 1) {
            Collections.shuffle(possibleAnimals);
        }

        var numberOfAnimals = random.nextInt(possibleAnimals.size());
        if (numberOfAnimals == 0) {
            return Optional.empty();
        }

        var animals = possibleAnimals.subList(0, numberOfAnimals);

        var tiles = animals.stream()
                .map(animalType -> {
                    var possibleTiles = new ArrayList<>(game.getTilesWithSpecies(animalType));
                    return possibleTiles.get(random.nextInt(possibleTiles.size()));
                })
                .collect(Collectors.toList());

        return Optional.of(new Action.Evolution(tiles, animals));
    }

    private Optional<Action> predator(DominantSpecies game, Random random) {
        var tiles = new ArrayList<>(game.getTilesWithSpecies(game.getCurrentAnimal()));

        var animals = tiles.stream()
                .map(game::getTile)
                .map(Optional::get)
                .map(tile -> {
                    var opposingSpecies = tile.getSpecies().entrySet().stream()
                            .filter(entry -> entry.getKey() != game.getCurrentAnimal() && entry.getValue() > 0)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    return !opposingSpecies.isEmpty() ? opposingSpecies.get(random.nextInt(opposingSpecies.size())) : null;
                })
                .collect(Collectors.toList());

        return Optional.of(new Action.Predator(tiles, animals));
    }

    private Optional<Action> metamorphosis(DominantSpecies game, Random random) {
        var animal = game.getAnimal(game.getCurrentAnimal());

        var swappableElements = animal.getRemovableElements();

        if (swappableElements.isEmpty()) {
            return Optional.empty();
        }

        var from = swappableElements.get(random.nextInt(swappableElements.size()));

        var possibleElementTypes = Arrays.stream(ElementType.values())
                .filter(elementType -> game.getDrawBag().contains(elementType))
                .collect(Collectors.toList());

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        var to = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        return Optional.of(new Action.Metamorphosis(from, to));
    }

    private Optional<Action> massExodus(DominantSpecies game, Random random) {
        var possibleTiles = new ArrayList<>(game.getTiles().keySet());
        var from = possibleTiles.get(random.nextInt(possibleTiles.size()));
        var tile = game.getTile(from).get();

        var adjacentTiles = game.getAdjacentHexes(from)
                .filter(game::hasTile)
                .collect(Collectors.toList());
        Collections.shuffle(adjacentTiles);

        var moves = new ArrayList<Action.MassExodus.Move>();

        for (var animalType : tile.getSpecies().keySet()) {
            var remaining = tile.getSpecies(animalType);

            while (remaining > 0) {
                var species = random.nextInt(remaining - 1) + 1;

                var to = adjacentTiles.get(random.nextInt(adjacentTiles.size()));

                moves.add(new Action.MassExodus.Move(to, animalType, species));

                remaining -= species;
            }
        }

        return Optional.of(new Action.MassExodus(from, moves));
    }

    private Optional<Action> removeAllBut1SpeciesOnEachTile() {
        return Optional.of(new Action.RemoveAllBut1SpeciesOnEachTile());
    }

    private Optional<Action> removeActionPawn() {
        return Optional.of(new Action.RemoveActionPawn());
    }

    private Optional<Action> immigrants(DominantSpecies game, Random random) {
        var possibleActions = game.possibleActions();

        var possibleAction = possibleActions.get(random.nextInt(possibleActions.size()));

        if (possibleAction == Action.RemoveElement.class) {
            return removeElement(game, random);
        } else if (possibleAction == Action.RemoveActionPawn.class) {
            return removeActionPawn();
        } else if (possibleAction == Action.RemoveAllBut1SpeciesOnEachTile.class) {
            return removeAllBut1SpeciesOnEachTile();
        }
        return Optional.empty();
    }

    private Optional<Action> removeElement(DominantSpecies game, Random random) {
        var animal = game.getAnimal(game.getCurrentAnimal());

        var removableElements = animal.getRemovableElements();

        if (removableElements.isEmpty()) {
            return Optional.empty();
        }

        var element = removableElements.get(random.nextInt(removableElements.size()));

        return Optional.of(new Action.RemoveElement(element));
    }

    private Optional<Action> catastrophe(DominantSpecies game, Random random) {
        var possibleTiles = new ArrayList<>(game.getTiles().keySet());
        var hex = possibleTiles.get(random.nextInt(possibleTiles.size()));
        var tile = game.getTile(hex).get();

        var possibleSaves = game.getAnimals().keySet().stream()
                .filter(tile::hasSpecies)
                .collect(Collectors.toList());

        var save = !possibleSaves.isEmpty()
                ? possibleSaves.get(random.nextInt(possibleSaves.size())) : null;

        var adjacentTiles = game.getAdjacentHexes(hex)
                .filter(game::hasTile)
                .collect(Collectors.toList());

        var eliminateOnAdjacentTiles = adjacentTiles.stream()
                .map(adjacentTile -> {
                    var possibleEliminations = game.getAnimals().keySet().stream()
                            .filter(tile::hasSpecies)
                            .collect(Collectors.toList());

                    return !possibleEliminations.isEmpty()
                            ? possibleEliminations.get(random.nextInt(possibleEliminations.size())) : null;
                })
                .collect(Collectors.toList());

        return Optional.of(new Action.Catastrophe(hex, save, adjacentTiles, eliminateOnAdjacentTiles));
    }

    private Optional<Action> blight(DominantSpecies game, Random random) {
        var possibleTiles = new ArrayList<>(game.getTiles().keySet());

        var tile = possibleTiles.get(random.nextInt(possibleTiles.size()));

        var elements = game.getAdjacentElements(tile);
        if (elements.size() > 1) {
            elements.remove(random.nextInt(elements.size()));
        } else {
            elements.clear();
        }

        return Optional.of(new Action.Blight(tile, new HashSet<>(elements)));
    }

    private Optional<Action> biomass(DominantSpecies game, Random random) {
        var tiles = new ArrayList<>(Action.Biomass.getAffectedTiles(game));

        var animalTypes = tiles.stream()
                .map(game::getTile)
                .map(Optional::get)
                .map(tile -> {
                    var possibleAnimalTypes = tile.getSpecies().entrySet().stream()
                            .filter(entry -> entry.getValue() > 0)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    return possibleAnimalTypes.get(random.nextInt(possibleAnimalTypes.size()));
                })
                .collect(Collectors.toList());

        return Optional.of(new Action.Biomass(tiles, animalTypes));
    }

    private Optional<Action> aquatic(DominantSpecies game, Random random) {
        var possibleElementTypes = Arrays.stream(ElementType.values())
                .filter(elementType -> game.getDrawBag().contains(elementType))
                .collect(Collectors.toList());

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        var elementType = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        var possibleCorners = game.getVacantCorners()
                .filter(corner -> game.getAdjacentTiles(corner)
                        .anyMatch(tile -> tile.getType() == TileType.SEA || tile.getType() == TileType.WETLAND))
                .collect(Collectors.toList());

        if (possibleCorners.isEmpty()) {
            return Optional.empty();
        }

        var corner = possibleCorners.get(random.nextInt(possibleCorners.size()));

        var species = Math.min(game.getAnimal(game.getCurrentAnimal()).getGenePool(), Action.Aquatic.MAX_SPECIES);

        if (species > 0) {
            var possibleTiles = game.getTiles().entrySet().stream()
                    .filter(entry -> entry.getValue().getType() == TileType.SEA || entry.getValue().getType() == TileType.WETLAND)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            var tile = !possibleTiles.isEmpty() ? possibleTiles.get(random.nextInt(possibleTiles.size())) : null;

            return Optional.of(new Action.Aquatic(elementType, corner, tile, species));
        } else {
            return Optional.of(new Action.Aquatic(elementType, corner, null, 0));
        }
    }

    private Optional<Action> dominanceCard(DominantSpecies game, Random random) {
        var possibleCards = new ArrayList<Card>(game.getAvailableCards());

        // TODO Make smarter than just random
        var card = possibleCards.get(random.nextInt(possibleCards.size()));

        return Optional.of(new Action.DominanceCard(card));
    }

    private Optional<Action> domination(DominantSpecies game, Random random) {
        var possibleTiles = game.getTiles().keySet().stream()
                .filter(tile -> !game.getScoredTiles().contains(tile))
                .collect(Collectors.toList());

        if (possibleTiles.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var tile = possibleTiles.get(random.nextInt(possibleTiles.size()));

        return Optional.of(new Action.Domination(tile));
    }

    private Optional<Action> competition(DominantSpecies game, Random random) {
        var possibleTileTypes = Action.Competition.getPossibleTileTypes(game);

        var tiles = possibleTileTypes.stream()
                // Don't do every tile type always
                .filter(tileType -> random.nextInt(6) == 0) // TODO Make smarter than just random
                .flatMap(tileType -> {
                    var possibleTiles = Action.Competition.getPossibleTiles(game)
                            .collect(Collectors.toList());

                    if (possibleTiles.isEmpty()) {
                        return Stream.empty();
                    }

                    // TODO Make smarter than just random
                    return Stream.of(possibleTiles.get(random.nextInt(possibleTiles.size())));
                })
                .collect(Collectors.toList());

        var animals = tiles.stream()
                .map(game::getTile)
                .flatMap(Optional::stream)
                .map(tile -> {
                    var opposingSpeciesOnTile = game.getAnimals().keySet().stream()
                            .filter(animalType -> animalType != game.getCurrentAnimal())
                            .filter(tile::hasSpecies)
                            .collect(Collectors.toList());

                    // TODO Make smarter than just random
                    return opposingSpeciesOnTile.get(random.nextInt(opposingSpeciesOnTile.size()));
                })
                .collect(Collectors.toList());

        return Optional.of(new Action.Competition(tiles, animals));
    }

    private Optional<Action> migration(DominantSpecies game, Random random) {
        // TODO Make smarter than just random

        var actionPawn = game.getActionDisplay().getCurrentActionPawn()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));

        var maxSpecies = Action.Migration.getMaxSpecies(actionPawn);

        var tilesWithSpecies = new ArrayList<>(game.getTilesWithSpecies(game.getCurrentAnimal()));
        if (tilesWithSpecies.isEmpty()) {
            return Optional.empty();
        }

        Collections.shuffle(tilesWithSpecies, random);

        var moves = new ArrayList<Action.Migration.Move>();
        var remainingSpeciesToMove = random.nextInt(maxSpecies);

        for (var from : tilesWithSpecies) {
            if (remainingSpeciesToMove == 0) {
                break;
            }

            var speciesOnTile = game.getTile(from).orElseThrow().getSpecies(game.getCurrentAnimal());
            var species = random.nextInt(Math.min(remainingSpeciesToMove, speciesOnTile));

            if (species > 0) {
                var adjacentTiles = game.getAdjacentHexes(from)
                        .filter(game::hasTile)
                        .collect(Collectors.toList());
                var to = adjacentTiles.get(random.nextInt(adjacentTiles.size()));

                moves.add(new Action.Migration.Move(from, to, species));

                remainingSpeciesToMove -= species;
            }
        }

        return Optional.of(new Action.Migration(moves));
    }

    private Optional<Action> wanderlustMove(DominantSpecies game, Random random) {
        var lastPlacedTile = game.getLastPlacedTile()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_TILE_SELECTED));

        var moves = game.getAdjacentHexes(lastPlacedTile)
                .filter(game::hasTile)
                .flatMap(from -> game.getTile(from)
                        .stream()
                        .filter(tile -> tile.hasSpecies(game.getCurrentAnimal()))
                        .map(tile -> new Action.WanderlustMove.Move(from,
                                random.nextInt(tile.getSpecies(game.getCurrentAnimal())) // TODO Make smarter than just random
                        ))
                        .filter(move -> move.species > 0))
                .collect(Collectors.toList());

        return Optional.of(new Action.WanderlustMove(moves));
    }

    private Optional<Action> wanderlust(DominantSpecies game, Random random) {
        var possibleStacks = IntStream.range(0, 3)
                .filter(index -> game.getWanderlustTiles().getStack(index).getFaceUp().isPresent())
                .toArray();

        if (possibleStacks.length == 0) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var stack = possibleStacks[random.nextInt(possibleStacks.length)];

        var possibleHexes = game.getVacantHexes()
                .collect(Collectors.toList());

        if (possibleHexes.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var hex = possibleHexes.get(random.nextInt(possibleHexes.size()));

        var possibleElementTypes = game.getActionDisplay().getElements(ActionType.WANDERLUST);
        var elementType = !possibleElementTypes.isEmpty()
                ? possibleElementTypes.get(random.nextInt(possibleElementTypes.size())) // TODO Make smarter than just random
                : null;

        Corner corner = null;
        if (elementType != null) {
            var adjacentHexes = game.getAdjacentHexes(hex).collect(Collectors.toList());

            var possibleCorners = game.getVacantCorners()
                    .filter(c -> c.isAdjacent(hex))
                    .collect(Collectors.toList());

            corner = !possibleCorners.isEmpty()
                    ? possibleCorners.get(random.nextInt(possibleCorners.size())) // TODO Make smarter than just random
                    : null;
        }

        return Optional.of(new Action.Wanderlust(stack, hex, elementType, corner));
    }

    private Optional<Action> glaciation(DominantSpecies game, Random random) {
        var possibleHexes = game.getTiles().entrySet().stream()
                .filter(entry -> !entry.getValue().isTundra())
                .filter(entry -> game.getAdjacentTiles(entry.getKey()).anyMatch(Tile::isTundra))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (possibleHexes.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var hex = possibleHexes.get(random.nextInt(possibleHexes.size()));

        return Optional.of(new Action.Glaciation(hex));
    }

    private Optional<Action> depletion(DominantSpecies game, Random random) {
        var possibleElementTypes = game.getActionDisplay().getElements(ActionType.DEPLETION);

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        var possibleCorners = game.getElements().entrySet().stream()
                .filter(entry -> possibleElementTypes.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (possibleCorners.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var corner = possibleCorners.get(random.nextInt(possibleCorners.size()));

        return Optional.of(new Action.Depletion(corner));
    }

    private Optional<Action> wasteland(DominantSpecies game, Random random) {
        var possibleElementTypes = game.getActionDisplay().getElements(ActionType.WASTELAND);

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var elementType = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        return Optional.of(new Action.Wasteland(elementType));
    }

    private Optional<Action> abundance(DominantSpecies game, Random random) {
        var possibleElementTypes = game.getActionDisplay().getElements(ActionType.ABUNDANCE);

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        var possibleCorners = game.getVacantCorners().collect(Collectors.toList());

        if (possibleCorners.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var elementType = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        // TODO Make smarter than just random
        var corner = possibleCorners.get(random.nextInt(possibleCorners.size()));

        return Optional.of(new Action.Abundance(elementType, corner));
    }

    private Optional<Action> regression(DominantSpecies game, Random random) {
        var possibleElementTypes = new ArrayList<>(new HashSet<>(game.getActionDisplay().getElements(ActionType.REGRESSION)));

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        Collections.shuffle(possibleElementTypes, random);

        var numberOfActionPawns = game.getActionDisplay().getNumberOfActionPawns(ActionType.REGRESSION, game.getCurrentAnimal());
        var elementTypes = possibleElementTypes.subList(0, Math.min(possibleElementTypes.size(), numberOfActionPawns));

        return Optional.of(new Action.Regression(Set.copyOf(elementTypes)));
    }

    private Optional<Action> adaptation(DominantSpecies game, Random random) {
        if (!game.getAnimal(game.getCurrentAnimal()).canAddElement()) {
            return Optional.empty();
        }

        var possibleElementTypes = game.getActionDisplay().getElements(ActionType.ADAPTATION);

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var elementType = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        return Optional.of(new Action.Adaptation(elementType));
    }

    private Optional<Action> speciation(DominantSpecies game, Random random) {
        var actionPawn = game.getActionDisplay().getCurrentActionPawn()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));

        if (actionPawn.isFree() && game.getCurrentAnimal() == AnimalType.INSECTS) {
            var possibleHexes = new ArrayList<>(game.getTiles().keySet());
            // TODO Make smarter than just random
            var hex = possibleHexes.get(random.nextInt(possibleHexes.size()));

            return Optional.of(new Action.Speciation(null, List.of(hex), List.of(1)));
        } else {
            var elementType = Action.Speciation.getElementType(actionPawn.getIndex());

            var possibleElements = game.getElements().entrySet().stream()
                    .filter(entry -> entry.getValue() == elementType)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (possibleElements.isEmpty()) {
                return Optional.empty();
            }

            // TODO Make smarter than just random
            var element = possibleElements.get(random.nextInt(possibleElements.size()));

            var hexes = Stream.of(element.getA(), element.getB(), element.getC())
                    .filter(game::hasTile)
                    .collect(Collectors.toList());

            var tiles = hexes.stream()
                    .map(game::getTile)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            var remainingSpecies = game.getAnimal(game.getCurrentAnimal()).getGenePool();
            var species = new ArrayList<Integer>();
            for (var tile : tiles) {
                // TODO Just do max for now
                var amount = Math.min(remainingSpecies, Action.Speciation.getMaxSpeciation(tile));
                if (amount > 0) {
                    species.add(amount);
                    remainingSpecies -= amount;
                }
            }

            // Truncate lists if not enough species in gene pool to place on all tiles
            while (tiles.size() > species.size()) {
                tiles.remove(tiles.size() - 1);
                hexes.remove(hexes.size() - 1);
            }

            return Optional.of(new Action.Speciation(element, hexes, species));
        }
    }

    private Action placeActionPawn(DominantSpecies game, Random random) {
        // TODO Just pick a random action space for now
        var placements = game.getActionDisplay().possiblePlacements().collect(Collectors.toList());
        var placement = placements.get(random.nextInt(placements.size()));
        return new Action.PlaceActionPawn(placement.getActionType(), placement.getIndex());
    }

}
