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
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // make effectively final
public abstract class Action implements com.boardgamefiesta.api.domain.Action {

    abstract ActionResult perform(DominantSpecies game, Random random);

    public static Class<? extends Action> forName(String actionName) throws ClassNotFoundException {
        // TODO Make case insensitive
        return Class.forName(Action.class.getName() + "$" + actionName).asSubclass(Action.class);
    }

    public static String getName(Class<? extends Action> clazz) {
        // TODO Make more "class agnostic"
        return clazz.getSimpleName();
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class PlaceActionPawn extends Action {
        @NonNull
        ActionType actionType;

        int index;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var animal = game.getAnimal(game.getCurrentAnimal());

            if (game.getPhase() == Phase.EXECUTION) {
                if (actionType == ActionType.INITIATIVE) {
                    throw new DominantSpeciesException(DominantSpeciesError.ACTION_SPACE_NOT_ALLOWED);
                }
            } else {
                animal.removeActionPawn();
            }

            game.getActionDisplay().placeActionPawn(animal.getType(), actionType, index);

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Adaptation extends Action {
        @NonNull
        ElementType elementType;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getActionDisplay().removeLeftMostActionPawn(ActionType.ADAPTATION);
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.getActionDisplay().removeElement(ActionType.ADAPTATION, elementType);
            game.getAnimal(game.getCurrentAnimal()).addElement(elementType);

            return ActionResult.undoAllowed();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Regression extends Action {
        /**
         * Element types that should be skipped.
         */
        @NonNull
        Set<ElementType> elementTypes;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var numberOfActionPawns = game.getActionDisplay().getNumberOfActionPawns(ActionType.REGRESSION, game.getCurrentAnimal());

            if (elementTypes.size() > numberOfActionPawns) {
                throw new DominantSpeciesException(DominantSpeciesError.CANNOT_SKIP_MORE_ELEMENT_TYPES_THAN_ACTION_PAWNS_PLACED);
            } // Less is allowed

            var regressionBox = game.getActionDisplay().getElements().get(ActionType.REGRESSION);
            var elementTypesInRegressionBox = new HashSet<>(regressionBox);

            var elementTypesToRemove = new ArrayList<>(elementTypesInRegressionBox);
            this.elementTypes.forEach(elementTypesToRemove::remove);

            var animal = game.getAnimal(game.getCurrentAnimal());
            animal.removeOneOfEachElementType(elementTypesToRemove);

            returnActionPawns(game, animal);

            return ActionResult.undoAllowed();
        }

        static FollowUpActions activate(DominantSpecies game) {
            return AnimalType.FOOD_CHAIN_ORDER.stream() // TODO In which order do the animals perform Regression?
                    .filter(game::hasAnimal)
                    .map(game::getAnimal)
                    .map(animal -> autoRemoveOrFollowUpAction(animal, game))
                    .reduce(FollowUpActions.none(), FollowUpActions::concat);
        }

        static FollowUpActions autoRemoveOrFollowUpAction(Animal animal, DominantSpecies game) {
            var regressionBox = game.getActionDisplay().getElements(ActionType.REGRESSION);
            var elementTypesInRegressionBox = new HashSet<>(regressionBox);

            var numberOfActionPawns = game.getActionDisplay().getNumberOfActionPawns(ActionType.REGRESSION, animal.getType());
            var numberOfElementsToRemove = Math.max(0, elementTypesInRegressionBox.size() - numberOfActionPawns);

            if (numberOfElementsToRemove == 0 || !animal.canRemoveOneOfElementTypes(elementTypesInRegressionBox)) {
                // Placed enough APs to skip all removals, regression box is empty, or animal cannot remove any of the element types
                returnActionPawns(game, animal);
                return FollowUpActions.none();
            } else if (numberOfActionPawns == 0) {
                // Not placed any APs therefore must remove one of each type of Element in the Regression Box
                animal.removeOneOfEachElementType(elementTypesInRegressionBox);
                returnActionPawns(game, animal);
                return FollowUpActions.none();
            } else {
                // May skip one or more element types
                return FollowUpActions.of(List.of(PossibleAction.mandatory(animal.getType(), Action.Regression.class)));
            }
        }

        private static void returnActionPawns(DominantSpecies game, Animal animal) {
            var removedActionPawns = game.getActionDisplay().removeActionPawns(ActionType.REGRESSION, animal.getType());
            animal.addActionPawns(removedActionPawns);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Abundance extends Action {
        @NonNull
        ElementType elementType;

        @NonNull
        Corner corner;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getActionDisplay().removeLeftMostActionPawn(ActionType.ABUNDANCE);
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.getActionDisplay().removeElement(ActionType.ABUNDANCE, elementType);
            game.addElement(corner, elementType);

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Wasteland extends Action {
        @NonNull
        ElementType elementType;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getActionDisplay().removeLeftMostActionPawn(ActionType.WASTELAND);
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.getActionDisplay().removeElement(ActionType.WASTELAND, elementType);
            game.getDrawBag().add(elementType);

            return ActionResult.undoAllowed();
        }

    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Depletion extends Action {
        @NonNull
        Corner corner;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var element = game.getElement(corner)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ELEMENT_AT_CORNER));

            var depletionBox = game.getActionDisplay().getElements().get(ActionType.DEPLETION);

            if (!depletionBox.contains(element)) {
                throw new DominantSpeciesException(DominantSpeciesError.ELEMENT_NOT_IN_DEPLETION_BOX);
            }

            game.removeElement(corner);
            game.getDrawBag().add(element);

            game.getActionDisplay().removeLeftMostActionPawn(ActionType.DEPLETION);
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Glaciation extends Action {
        @NonNull
        Hex tile;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            var numberOfAdjacentTundraTiles = (int) game.getAdjacentTiles(this.tile).filter(Tile::isTundra).count();
            if (numberOfAdjacentTundraTiles == 0) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TUNDRA_TILE);
            }

            game.removeAvailableTundraTile();

            tile.glaciate();

            var elementsToRemove = game.getElements().keySet().stream()
                    .filter(element -> game.getTiles().entrySet().stream()
                            .filter(t -> t.getValue().isTundra())
                            .filter(t -> element.isAdjacent(t.getKey()))
                            .count() == 3)
                    .collect(Collectors.toSet());
            elementsToRemove.forEach(game::removeElement);

            var animalTypes = new LinkedList<>(tile.getSpecies().keySet());
            for (var animalType : animalTypes) {
                var speciesToRemove = tile.getSpecies(animalType) - 1;
                if (speciesToRemove > 0) {
                    tile.removeSpecies(animalType, speciesToRemove);
                    game.getAnimal(animalType).addSpeciesToGenePool(speciesToRemove);
                }
            }

            game.getAnimal(game.getCurrentAnimal()).addVPs(
                    DominantSpecies.bonusVPs(numberOfAdjacentTundraTiles));

            game.getActionDisplay().removeLeftMostActionPawn(ActionType.GLACIATION);

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Speciation extends Action {

        Corner element;
        @NonNull
        List<Hex> tiles;
        @NonNull
        List<Integer> species;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var actionPawn = game.getActionDisplay().getLeftMostExecutableActionPawn(ActionType.SPECIATION)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));

            if (tiles.isEmpty()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_AT_LEAST_1_TILE);
            }
            if (species.size() != tiles.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SPECIFY_SPECIES_FOR_EACH_TILE);
            }

            if (actionPawn.isFree() && game.getCurrentAnimal() == AnimalType.INSECTS) {
                if (tiles.size() != 1) {
                    throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_1_TILE);
                }
                if (species.get(0) > 1) {
                    throw new DominantSpeciesException(DominantSpeciesError.MAX_SPECIES_EXCEEDED);
                }
            } else {
                if (tiles.size() > 3) {
                    throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_UP_TO_3_TILES);
                }

                var element = game.getElement(this.element)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.ELEMENT_NOT_FOUND));

                if (element != getElementType(actionPawn.getIndex())) {
                    throw new DominantSpeciesException(DominantSpeciesError.INVALID_ELEMENT_TYPE);
                }

                if (!tiles.stream().allMatch(this.element::isAdjacent)) {
                    throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_ELEMENT);
                }
            }

            var animal = game.getAnimal(game.getCurrentAnimal());

            for (var i = 0; i < tiles.size(); i++) {
                var tile = game.getTile(tiles.get(i))
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var species = this.species.get(i);

                if (species > getMaxSpeciation(tile)) {
                    throw new DominantSpeciesException(DominantSpeciesError.MAX_SPECIES_EXCEEDED);
                }

                animal.removeSpeciesFromGenePool(species);
                tile.addSpecies(animal.getType(), species);
            }

            if (game.getActionDisplay().removeLeftMostActionPawn(ActionType.SPECIATION)) {
                game.getAnimal(game.getCurrentAnimal()).addActionPawn();
            }

            return ActionResult.undoAllowed();
        }

        static int getMaxSpeciation(Tile tile) {
            if (tile.isTundra()) {
                return 1;
            }
            return tile.getType().getMaxSpeciation();
        }

        static ElementType getElementType(int index) {
            switch (index) {
                case 0:
                    return ElementType.MEAT;
                case 1:
                    return ElementType.SUN;
                case 2:
                    return ElementType.SEED;
                case 3:
                    return ElementType.WATER;
                case 4:
                    return ElementType.GRUB;
                case 5:
                    return ElementType.GRASS;
                default:
                    throw new IllegalArgumentException("Invalid AP index:" + index);
            }
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    @Getter
    @ToString
    public static final class Wanderlust extends Action {
        int stack;

        @NonNull
        Hex hex;

        ElementType elementType;
        Corner corner;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var tileType = game.getWanderlustTiles().getStack(stack).removeFaceUp();

            var tile = Tile.initial(tileType, false);
            game.addTile(hex, tile);

            if (elementType != null && corner != null) {
                game.getActionDisplay().removeElement(ActionType.WANDERLUST, elementType);
                game.addElement(corner, elementType);
            }

            var animal = game.getAnimal(game.getCurrentAnimal());

            var adjacentTiles = game.getAdjacentTiles(hex).collect(Collectors.toList());
            var numberOfAdjacentTiles = adjacentTiles.size();
            animal.addVPs(DominantSpecies.bonusVPs(numberOfAdjacentTiles));

            game.getActionDisplay().removeLeftMostActionPawn(ActionType.WANDERLUST);
            animal.addActionPawn();

            return ActionResult.undoAllowed(FollowUpActions.of(
                    AnimalType.FOOD_CHAIN_ORDER.stream()
                            .filter(animalType -> adjacentTiles.stream().anyMatch(adjacentTile -> adjacentTile.hasSpecies(animalType)))
                            .map(animalType -> PossibleAction.optional(animalType, Action.WanderlustMove.class))
                            .collect(Collectors.toList())));
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class WanderlustMove extends Action {

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static final class Move {
            Hex from;
            int species;
        }

        List<Move> moves;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var lastPlacedTile = game.getLastPlacedTile()
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_TILE_SELECTED));

            var tile = game.getTile(lastPlacedTile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            moves.forEach(move -> {
                var from = game.getTile(move.from)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                if (!move.from.isAdjacent(lastPlacedTile)) {
                    throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TILE);
                }

                from.removeSpecies(game.getCurrentAnimal(), move.species);
                tile.addSpecies(game.getCurrentAnimal(), move.species);
            });

            return ActionResult.undoAllowed();
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Migration extends Action {

        private static final int[] MAX_SPECIES_TO_MOVE = new int[]{7, 6, 5, 4, 3, 2};

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static final class Move {
            Hex from;
            Hex to;
            int species;
        }

        List<Move> moves;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var actionPawn = game.getActionDisplay().getLeftMostExecutableActionPawn(ActionType.MIGRATION)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));

            var maxSpecies = getMaxSpecies(actionPawn);
            var maxDistance = game.getCurrentAnimal() == AnimalType.BIRDS ? 2 : 1;

            if (moves.stream().mapToInt(Move::getSpecies).sum() > maxSpecies) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_SPECIES_EXCEEDED);
            }

            if (moves.stream().anyMatch(move -> move.from.distance(move.to) > maxDistance)) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_DISTANCE_EXCEEDED);
            }

            if (moves.stream().anyMatch(move -> !game.canMoveThroughAdjacentTiles(move.from, move.to))) {
                throw new DominantSpeciesException(DominantSpeciesError.CANNOT_MOVE_THROUGH_BLANK_HEX);
            }

            moves.forEach(move -> {
                var from = game.getTile(move.from)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var to = game.getTile(move.to)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                from.removeSpecies(game.getCurrentAnimal(), move.species);
                to.addSpecies(game.getCurrentAnimal(), move.species);
            });

            game.getActionDisplay().removeLeftMostActionPawn(ActionType.MIGRATION);
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            return ActionResult.undoAllowed();
        }

        static int getMaxSpecies(ActionDisplay.ActionPawn actionPawn) {
            return MAX_SPECIES_TO_MOVE[actionPawn.getIndex()];
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Competition extends Action {

        @NonNull
        List<Hex> tiles;

        @NonNull
        List<AnimalType> animals;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (tiles.size() != animals.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ANIMAL_FOR_EACH_TILE);
            }

            var opposingSpecies = getOpposingSpecies(game);

            if (!opposingSpecies.containsAll(animals)) {
                throw new DominantSpeciesException(DominantSpeciesError.NOT_OPPOSING_SPECIES);
            }

            var possibleTiles = getPossibleTiles(game, opposingSpecies).collect(Collectors.toSet());

            if (!possibleTiles.containsAll(tiles)) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_TILE);
            }

            var tiles = this.tiles.stream()
                    .map(hex -> game.getTile(hex)
                            .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND)))
                    .collect(Collectors.toList());

            if (tiles.stream().map(Tile::getType).distinct().count() != this.tiles.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.DUPLICATE_TILE_TYPES);
            }

            for (var i = 0; i < tiles.size(); i++) {
                var tile = tiles.get(i);
                var animal = game.getAnimal(animals.get(i));

                tile.removeSpecies(animal.getType());
                animal.addEliminatedSpecies();
            }

            if (game.getActionDisplay().removeLeftMostActionPawn(ActionType.COMPETITION)) {
                game.getAnimal(game.getCurrentAnimal()).addActionPawn();
            }

            return ActionResult.undoAllowed();
        }

        private static Set<AnimalType> getOpposingSpecies(DominantSpecies game) {
            return game.getOpposingSpecies(game.getCurrentAnimal());
        }

        static Stream<Hex> getPossibleTiles(DominantSpecies game, Set<AnimalType> opposingSpecies) {
            var possibleTileTypes = getPossibleTileTypes(game);

            return game.getTiles().entrySet().stream()
                    .filter(entry -> entry.getValue().isTundra() || possibleTileTypes.contains(entry.getValue().getType()))
                    .filter(entry -> entry.getValue().hasSpecies(game.getCurrentAnimal())
                            && opposingSpecies.stream().anyMatch(entry.getValue()::hasSpecies))
                    .map(Map.Entry::getKey);
        }

        public static Set<TileType> getPossibleTileTypes(DominantSpecies game) {
            var actionPawn = game.getActionDisplay().getLeftMostExecutableActionPawn(ActionType.COMPETITION)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));

            switch (actionPawn.getIndex()) {
                case 0:
                    return Set.of(TileType.values());
                case 1:
                    return Set.of(TileType.JUNGLE, TileType.WETLAND);
                case 2:
                    return Set.of(TileType.WETLAND, TileType.DESERT);
                case 3:
                    return Set.of(TileType.DESERT, TileType.FOREST);
                case 4:
                    return Set.of(TileType.FOREST, TileType.SAVANNAH);
                case 5:
                    return Set.of(TileType.SAVANNAH, TileType.MOUNTAIN);
                case 6:
                    return Set.of(TileType.MOUNTAIN, TileType.SEA);
                case 7:
                    return Set.of(TileType.SEA, TileType.JUNGLE);
                default:
                    return Collections.emptySet();
            }
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Domination extends Action {

        @NonNull
        Hex tile;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getActionDisplay().removeLeftMostActionPawn(ActionType.DOMINATION);
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            if (game.getScoredTiles().contains(this.tile)) {
                throw new DominantSpeciesException(DominantSpeciesError.TILE_ALREADY_SCORED);
            }

            game.scoreTile(this.tile);

            return tile.getDominant()
                    .map(dominant -> ActionResult.undoAllowed(PossibleAction.mandatory(dominant, Action.DominanceCard.class)))
                    .orElse(ActionResult.undoAllowed());
        }

    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class RemoveElement extends Action {

        ElementType element;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getAnimal(game.getCurrentAnimal()).removeElement(element);
            return ActionResult.undoAllowed();
        }
    }

    public static final class RemoveActionPawn extends Action {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getAnimal(game.getCurrentAnimal()).removeActionPawn();
            return ActionResult.undoAllowed();
        }
    }

    public static final class RemoveAllBut1SpeciesOnEachTile extends Action {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var animalType = game.getCurrentAnimal();

            game.getTiles().values().stream()
                    .filter(tile -> tile.getSpecies(animalType) > 1)
                    .forEach(tile -> tile.removeSpecies(animalType, tile.getSpecies(animalType) - 1));

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Aquatic extends Action {

        Hex tile;
        int species;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (species > 4) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_4_SPECIES_ALLOWED);
            }

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            if (tile.getType() != TileType.SEA && tile.getType() != TileType.WETLAND) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_TILE);
            }

            game.getAnimal(game.getCurrentAnimal()).removeSpeciesFromGenePool(species);
            tile.addSpecies(game.getCurrentAnimal(), species);

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Biomass extends Action {

        List<Hex> tiles;
        List<AnimalType> animals;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (tiles.size() != animals.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ANIMAL_FOR_EACH_TILE);
            }

            if (tiles.stream().distinct().count() != tiles.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.DUPLICATE_TILES);
            }

            var affectedTiles = getAffectedTiles(game);

            if (!tiles.containsAll(affectedTiles)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ALL_AFFECTED_TILES);
            }

            for (var i = 0; i < tiles.size(); i++) {
                var tile = game.getTile(tiles.get(i))
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var animalType = animals.get(i);

                tile.removeSpecies(animalType);
                game.getAnimal(animalType).addEliminatedSpecies();
            }

            return ActionResult.undoAllowed();
        }

        public static Set<Hex> getAffectedTiles(DominantSpecies game) {
            return game.getTiles().entrySet().stream()
                    .filter(tile -> tile.getValue().getTotalSpecies() > game.getElements().keySet().stream()
                            .filter(element -> element.isAdjacent(tile.getKey()))
                            .count())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Blight extends Action {

        Set<Corner> elements;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var tile = Corner.commonHex(elements)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.MUST_HAVE_1_COMMON_HEX));

            var elementsOnTile = game.getAdjacentElements(tile);
            if (elements.size() != elementsOnTile.size() - 1) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ALL_BUT_1_ELEMENT_ON_TILE);
            }

            for (var corner : elements) {
                game.removeElement(corner);
            }

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Catastrophe extends Action {

        Hex tile;
        AnimalType keep;

        List<Hex> adjacentTiles;
        List<AnimalType> animals;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            var adjacentHexes = game.getAdjacentHexes(this.tile)
                    .filter(game::hasTile)
                    .collect(Collectors.toSet());

            if (this.adjacentTiles.size() != adjacentHexes.size() ||
                    !this.adjacentTiles.containsAll(adjacentHexes)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ALL_ADJACENT_TILES);
            }

            tile.removeAllSpecies();
            tile.addSpecies(keep, 1);

            for (var i = 0; i < adjacentTiles.size(); i++) {
                var adjacentTile = game.getTile(adjacentTiles.get(i))
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var animalType = animals.get(i);
                adjacentTile.removeSpecies(animalType);
                game.getAnimal(animalType).addEliminatedSpecies();
            }

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Evolution extends Action {

        List<Hex> tiles;
        List<AnimalType> animals;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (tiles.size() > 2) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_UP_TO_2_TILES);
            }

            if (animals.size() != tiles.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ANIMAL_FOR_EACH_TILE);
            }

            var players = animals.stream()
                    .map(game::getAnimal)
                    .collect(Collectors.toSet());

            if (players.size() != tiles.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_DIFFERENT_PLAYERS);
            }

            var currentAnimal = game.getAnimal(game.getCurrentAnimal());

            for (var i = 0; i < tiles.size(); i++) {
                var tile = game.getTile(tiles.get(i))
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                var animal = game.getAnimal(animals.get(i));

                currentAnimal.removeSpeciesFromGenePool();

                tile.removeSpecies(animal.getType());
                animal.addEliminatedSpecies();

                tile.addSpecies(game.getCurrentAnimal());
            }

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Fecundity extends Action {

        Set<Hex> tiles;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var possibleTiles = getPossibleTiles(game);

            if (!possibleTiles.containsAll(tiles)) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_TILE);
            }

            game.getTiles().entrySet().stream()
                    .filter(tile -> this.tiles.contains(tile.getKey()))
                    .map(Map.Entry::getValue)
                    .forEach(tile -> {
                        game.getAnimal(game.getCurrentAnimal()).removeSpeciesFromGenePool();
                        tile.addSpecies(game.getCurrentAnimal());
                    });

            return ActionResult.undoAllowed();
        }

        public static Set<Hex> getPossibleTiles(DominantSpecies game) {
            return game.getTilesWithSpecies(game.getCurrentAnimal());
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Fertile extends Action {

        Hex tile;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var possibleTiles = getPossibleTiles(game);

            if (!possibleTiles.contains(tile)) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_TILE);
            }

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            game.getAnimal(game.getCurrentAnimal())
                    .addVPs(tile.getTotalSpecies());

            return ActionResult.undoAllowed();
        }

        public static Set<Hex> getPossibleTiles(DominantSpecies game) {
            return game.getTilesWithSpecies(game.getCurrentAnimal());
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Habitat extends Action {

        @NonNull
        ElementType elementType;

        @NonNull
        Corner corner;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getDrawBag().remove(elementType);
            game.addElement(corner, elementType);
            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Hibernation extends Action {

        Hex tile;
        int species; // TODO 'These species ignore Extinction this turn' ??

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (species > 5) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_5_ELIMINATED_SPECIES_ALLOWED);
            }

            var animal = game.getAnimal(game.getCurrentAnimal());

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            animal.removeEliminatedSpecies(species);
            tile.addSpecies(animal.getType(), species);

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class MassExodus extends Action {

        @AllArgsConstructor
        @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
        @Getter
        public static final class Move {
            Hex to;
            AnimalType animal;
            int species;
        }

        Hex from;
        List<Move> moves;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var from = game.getTile(this.from)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            var sum = moves.stream().mapToInt(Move::getSpecies).sum();
            if (sum != from.getTotalSpecies()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_MOVE_ALL_SPECIES_ON_TILE);
            }

            if (!moves.stream().map(Move::getTo).allMatch(this.from::isAdjacent)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ADJACENT_TILES);
            }

            for (var move : moves) {
                var to = game.getTile(move.to)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                from.removeSpecies(move.animal, move.species);
                to.addSpecies(move.animal, move.species);
            }

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Metamorphosis extends Action {

        ElementType from;
        ElementType to;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var animal = game.getAnimal(game.getCurrentAnimal());

            game.getDrawBag().remove(to);

            animal.removeElement(from);
            animal.addElement(to);

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class DominanceCard extends Action {

        Card card;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.removeAvailableCard(card);

            return card.perform(game, random);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Predator extends Action {

        List<Hex> tiles;
        List<AnimalType> animals;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (tiles.size() != animals.size()) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ANIMAL_FOR_EACH_TILE);
            }

            var occupiedTiles = getOccupiedTiles(game);

            if (!occupiedTiles.containsAll(tiles)) {
                throw new DominantSpeciesException(DominantSpeciesError.TILE_NOT_OCCUPIED_BY_PLAYER);
            }
            if (tiles.containsAll(occupiedTiles)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ALL_TILES_OCCUPIED_BY_PLAYER);
            }

            var opposingSpecies = game.getOpposingSpecies(game.getCurrentAnimal());

            if (!opposingSpecies.containsAll(animals)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_OPPOSING_SPECIES);
            }

            for (var i = 0; i < tiles.size(); i++) {
                var tile = game.getTile(tiles.get(i))
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var animalType = animals.get(i);

                tile.removeSpecies(animalType);
                game.getAnimal(animalType).addEliminatedSpecies();
            }

            return ActionResult.undoAllowed();
        }

        public static Set<Hex> getOccupiedTiles(DominantSpecies game) {
            return game.getTilesWithSpecies(game.getCurrentAnimal());
        }
    }
}
