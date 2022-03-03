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
                }
                // TODO Add actions of Dominance Cards

                action.ifPresentOrElse(a -> game.perform(player, a, random), () -> game.skip(player, random));
            }
        } while (game.getCurrentPlayers().contains(player));
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
        var opposingSpecies = game.getOpposingSpecies(game.getCurrentAnimal());

        var possibleTileTypes = Action.Competition.getPossibleTileTypes(game);

        var tiles = possibleTileTypes.stream()
                // Don't do every tile type always
                .filter(tileType -> random.nextInt(6) == 0) // TODO Make smarter than just random
                .flatMap(tileType -> {
                    var possibleTiles = Action.Competition.getPossibleTiles(game, opposingSpecies)
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
                    var opposingSpeciesOnTile = opposingSpecies.stream()
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

        var actionPawn = game.getActionDisplay().getLeftMostExecutableActionPawn(ActionType.MIGRATION)
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

            var adjacentTiles = game.getAdjacentHexes(from)
                    .filter(game::hasTile)
                    .collect(Collectors.toList());
            var to = adjacentTiles.get(random.nextInt(adjacentTiles.size()));

            moves.add(new Action.Migration.Move(from, to, species));

            remainingSpeciesToMove -= species;
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
                        )))
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

            var possibleCorners = adjacentHexes.stream()
                    .flatMap(b -> adjacentHexes.stream()
                            .map(c -> new Corner(hex, b, c)))
                    .filter(game::isVacant)
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
        var possibleElementTypes = game.getActionDisplay().getElements(ActionType.ADAPTATION);

        if (possibleElementTypes.isEmpty()) {
            return Optional.empty();
        }

        // TODO Make smarter than just random
        var elementType = possibleElementTypes.get(random.nextInt(possibleElementTypes.size()));

        return Optional.of(new Action.Adaptation(elementType));
    }

    private Optional<Action> speciation(DominantSpecies game, Random random) {
        var actionPawn = game.getActionDisplay().getLeftMostExecutableActionPawn(ActionType.SPECIATION)
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

            var hexes = List.of(element.getA(), element.getB(), element.getC());

            var tiles = hexes.stream()
                    .map(game::getTile)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            var species = tiles.stream()
                    // TODO Just do max for now
                    .map(Action.Speciation::getMaxSpeciation)
                    .collect(Collectors.toList());

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
