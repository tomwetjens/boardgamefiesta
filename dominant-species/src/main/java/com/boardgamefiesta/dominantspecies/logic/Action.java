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

            game.fireEvent(Event.Type.PLACE_ACTION_PAWN, List.of(actionType, index + 1));

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
            game.getActionDisplay().removeCurrentActionPawn();
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.getActionDisplay().removeElement(ActionType.ADAPTATION, elementType);
            game.getAnimal(game.getCurrentAnimal()).addElement(elementType);

            game.fireEvent(Event.Type.ADAPTATION, List.of(elementType));

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

            game.fireEvent(Event.Type.REGRESSION, List.of(numberOfActionPawns, elementTypesInRegressionBox.size()));

            var elementTypesToRemove = new ArrayList<>(elementTypesInRegressionBox);
            this.elementTypes.forEach(elementType -> {
                elementTypesToRemove.remove(elementType);
                game.fireEvent(Event.Type.SKIP_REGRESSION_OF_ELEMENT, List.of(elementType));
            });

            var animal = game.getAnimal(game.getCurrentAnimal());
            elementTypesToRemove.forEach(elementType -> {
                if (animal.canRemoveElement(elementType)) {
                    animal.removeElement(elementType);
                    game.fireEvent(Event.Type.REMOVE_ELEMENT_FROM_ANIMAL, List.of(elementType));
                }
            });

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

            game.fireEvent(animal.getType(), Event.Type.REGRESSION, List.of(numberOfActionPawns, elementTypesInRegressionBox.size()));

            if (numberOfElementsToRemove == 0 || !animal.canRemoveOneOfElementTypes(elementTypesInRegressionBox)) {
                // Placed enough APs to skip all removals, regression box is empty, or animal cannot remove any of the element types
                returnActionPawns(game, animal);
                return FollowUpActions.none();
            } else if (numberOfActionPawns == 0) {
                // Not placed any APs therefore must remove one of each type of Element in the Regression Box
                elementTypesInRegressionBox.forEach(elementType -> {
                    if (animal.canRemoveElement(elementType)) {
                        animal.removeElement(elementType);
                        game.fireEvent(animal.getType(), Event.Type.REMOVE_ELEMENT_FROM_ANIMAL, List.of(elementType));
                    }
                });
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
            game.getActionDisplay().removeCurrentActionPawn();
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.getActionDisplay().removeElement(ActionType.ABUNDANCE, elementType);
            game.addElement(corner, elementType);

            game.fireEvent(Event.Type.ABUNDANCE, List.of(elementType, corner));

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
            game.getActionDisplay().removeCurrentActionPawn();
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.getActionDisplay().removeElement(ActionType.WASTELAND, elementType);
            game.getDrawBag().add(elementType);

            game.fireEvent(Event.Type.WASTELAND, List.of(elementType));

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

            game.getActionDisplay().removeCurrentActionPawn();
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            game.fireEvent(Event.Type.DEPLETION, List.of(element, corner));

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

            game.fireEvent(Event.Type.GLACIATION, List.of(this.tile, tile.getType()));

            var elementsToRemove = game.getElements().keySet().stream()
                    .filter(element -> game.getTiles().entrySet().stream()
                            .filter(t -> t.getValue().isTundra())
                            .filter(t -> element.isAdjacent(t.getKey()))
                            .count() == 3)
                    .collect(Collectors.toSet());
            elementsToRemove.forEach(corner -> {
                var elementType = game.removeElement(corner);
                game.fireEvent(Event.Type.REMOVE_ELEMENT, List.of(elementType, corner));
            });

            var animalTypes = new LinkedList<>(tile.getSpecies().keySet());
            for (var animalType : animalTypes) {
                var speciesToRemove = tile.getSpecies(animalType) - 1;
                if (speciesToRemove > 0) {
                    tile.removeSpecies(animalType, speciesToRemove);
                    game.getAnimal(animalType).addSpeciesToGenePool(speciesToRemove);
                    game.fireEvent(Event.Type.REMOVE_SPECIES, List.of(animalType, speciesToRemove, this.tile, tile.getType()));
                }
            }

            var bonusVPs = DominantSpecies.bonusVPs(numberOfAdjacentTundraTiles);

            var animal = game.getAnimal(game.getCurrentAnimal());
            animal.addVPs(bonusVPs);
            game.fireEvent(Event.Type.GAIN_BONUS_VPS, List.of(bonusVPs));

            game.getActionDisplay().removeCurrentActionPawn();
            animal.addActionPawn();

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
            var actionPawn = game.getActionDisplay().getCurrentActionPawn()
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
                var hex = tiles.get(i);

                var tile = game.getTile(hex)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var species = this.species.get(i);

                if (species > getMaxSpeciation(tile)) {
                    throw new DominantSpeciesException(DominantSpeciesError.MAX_SPECIES_EXCEEDED);
                }

                animal.removeSpeciesFromGenePool(species);
                tile.addSpecies(animal.getType(), species);

                game.fireEvent(Event.Type.SPECIATION, List.of(species, hex, tile.getType()));
            }

            if (!game.getActionDisplay().removeCurrentActionPawn().isFree()) {
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

            game.fireEvent(Event.Type.WANDERLUST, List.of(stack + 1, tileType, hex));

            if (elementType != null && corner != null) {
                game.getActionDisplay().removeElement(ActionType.WANDERLUST, elementType);
                game.addElement(corner, elementType);

                game.fireEvent(Event.Type.ADD_ELEMENT, List.of(elementType, corner));
            }

            var animal = game.getAnimal(game.getCurrentAnimal());

            var adjacentTiles = game.getAdjacentTiles(hex).collect(Collectors.toList());
            var numberOfAdjacentTiles = adjacentTiles.size();
            var bonusVPs = DominantSpecies.bonusVPs(numberOfAdjacentTiles);
            animal.addVPs(bonusVPs);
            game.fireEvent(Event.Type.GAIN_BONUS_VPS, List.of(bonusVPs));

            game.getActionDisplay().removeCurrentActionPawn();
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
            if (moves.stream().anyMatch(move -> move.getSpecies() <= 0)) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_MOVE);
            }

            var lastPlacedTile = game.getLastPlacedTile()
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_TILE_SELECTED));

            var tile = game.getTile(lastPlacedTile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            // Combine moves for the same 'from' into a single move
            var normalized = moves.stream()
                    .collect(Collectors.groupingBy(Move::getFrom, Collectors.reducing((a, b) -> new WanderlustMove.Move(a.from, a.species + b.species))))
                    .values().stream()
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            normalized.forEach(move -> {
                var from = game.getTile(move.from)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                if (!move.from.isAdjacent(lastPlacedTile)) {
                    throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TILE);
                }

                from.removeSpecies(game.getCurrentAnimal(), move.species);
                tile.addSpecies(game.getCurrentAnimal(), move.species);

                game.fireEvent(Event.Type.MOVE_SPECIES, List.of(game.getCurrentAnimal(), move.species, move.from, from.getType(), lastPlacedTile, tile.getType()));
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

            Move combine(Move other) {
                if (!from.equals(other.from) || !to.equals(other.to)) {
                    throw new IllegalArgumentException("From and To must be equal when combining moves");
                }
                return new Move(from, to, species + other.species);
            }
        }

        @Value
        private static class FromTo {
            Hex from;
            Hex to;
        }

        List<Move> moves;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var actionPawn = game.getActionDisplay().getCurrentActionPawn()
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));

            var maxSpecies = getMaxSpecies(actionPawn);
            var maxDistance = game.getCurrentAnimal() == AnimalType.BIRDS ? 2 : 1;

            if (moves.stream().anyMatch(move -> move.getSpecies() <= 0)) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_MOVE);
            }

            if (moves.stream().mapToInt(Move::getSpecies).sum() > maxSpecies) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_SPECIES_EXCEEDED);
            }

            if (moves.stream().anyMatch(move -> move.from.distance(move.to) > maxDistance)) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_DISTANCE_EXCEEDED);
            }

            if (moves.stream().anyMatch(move -> !game.canMoveThroughAdjacentTiles(move.from, move.to))) {
                throw new DominantSpeciesException(DominantSpeciesError.CANNOT_MOVE_THROUGH_BLANK_HEX);
            }

            // Must check before moving any species in the loop below,
            // because it's only allowed to move species pre-existing on a tile
            if (moves.stream().anyMatch(move -> game.getTile(move.from).get().getSpecies(game.getCurrentAnimal()) < move.getSpecies())) {
                throw new DominantSpeciesException(DominantSpeciesError.NOT_ENOUGH_SPECIES_ON_TILE);
            }

            // Combine moves for the same from/to pair into a single move
            var normalized = moves.stream()
                    .collect(Collectors.groupingBy(move -> new FromTo(move.from, move.to),
                            Collectors.reducing(Move::combine)))
                    .values().stream()
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            normalized.forEach(move -> {
                var from = game.getTile(move.from)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var to = game.getTile(move.to)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                from.removeSpecies(game.getCurrentAnimal(), move.species);
                to.addSpecies(game.getCurrentAnimal(), move.species);

                game.fireEvent(Event.Type.MIGRATION, List.of(move.species, move.from, from.getType(), move.to, to.getType()));
            });

            game.getActionDisplay().removeCurrentActionPawn();
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

            if (animals.contains(game.getCurrentAnimal())) {
                throw new DominantSpeciesException(DominantSpeciesError.NOT_OPPOSING_SPECIES);
            }

            var possibleTiles = getPossibleTiles(game).collect(Collectors.toSet());

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
                var hex = this.tiles.get(i);
                var tile = tiles.get(i);
                var animal = game.getAnimal(animals.get(i));

                tile.removeSpecies(animal.getType());
                animal.addEliminatedSpecies();

                game.fireEvent(Event.Type.COMPETITION, List.of(animal.getType(), hex, tile.getType()));
            }

            if (!game.getActionDisplay().removeCurrentActionPawn().isFree()) {
                game.getAnimal(game.getCurrentAnimal()).addActionPawn();
            }

            return ActionResult.undoAllowed();
        }

        static Stream<Hex> getPossibleTiles(DominantSpecies game) {
            var possibleTileTypes = getPossibleTileTypes(game);

            return game.getTiles().entrySet().stream()
                    .filter(entry -> entry.getValue().isTundra() || possibleTileTypes.contains(entry.getValue().getType()))
                    .filter(entry -> entry.getValue().hasSpecies(game.getCurrentAnimal())
                            && game.getAnimals().keySet().stream()
                            .filter(animalType -> animalType != game.getCurrentAnimal())
                            .anyMatch(entry.getValue()::hasSpecies))
                    .map(Map.Entry::getKey);
        }

        public static Set<TileType> getPossibleTileTypes(DominantSpecies game) {
            var actionPawn = game.getActionDisplay().getCurrentActionPawn()
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
            game.getActionDisplay().removeCurrentActionPawn();
            game.getAnimal(game.getCurrentAnimal()).addActionPawn();

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            if (game.getScoredTiles().contains(this.tile)) {
                throw new DominantSpeciesException(DominantSpeciesError.TILE_ALREADY_SCORED);
            }

            game.fireEvent(Event.Type.DOMINATION, List.of(this.tile, tile.getType(), tile.getDominant().map(AnimalType::name).orElse("")));

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
            game.fireEvent(Event.Type.REMOVE_ELEMENT_FROM_ANIMAL, List.of(element));
            return ActionResult.undoAllowed();
        }
    }

    public static final class RemoveActionPawn extends Action {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            game.getAnimal(game.getCurrentAnimal()).removeActionPawn();
            game.fireEvent(Event.Type.REMOVE_ACTION_PAWN);
            return ActionResult.undoAllowed();
        }
    }

    public static final class RemoveAllBut1SpeciesOnEachTile extends Action {
        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            var animalType = game.getCurrentAnimal();
            var animal = game.getAnimal(animalType);

            game.getTiles().entrySet().stream()
                    .filter(tile -> tile.getValue().getSpecies(animalType) > 1)
                    .forEach(tile -> {
                        var species = tile.getValue().getSpecies(animalType) - 1;

                        tile.getValue().removeSpecies(animalType, species);
                        animal.addEliminatedSpecies(species);

                        game.fireEvent(Event.Type.ELIMINATE_SPECIES, List.of(animalType, species, tile.getKey(), tile.getValue().getType()));
                    });

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Aquatic extends Action {

        public static final int MAX_SPECIES = 4;

        ElementType elementType;
        Corner corner;

        Hex tile;
        int species;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (species < 0) {
                throw new DominantSpeciesException(DominantSpeciesError.INVALID_SPECIES);
            }

            if (species > MAX_SPECIES) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_4_SPECIES_ALLOWED);
            }

            if (game.getAdjacentTiles(corner).noneMatch(tile -> tile.getType() == TileType.WETLAND || tile.getType() == TileType.SEA)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_AQUATIC_TILE);
            }

            game.getDrawBag().remove(elementType);
            game.addElement(corner, elementType);

            game.fireEvent(Event.Type.ADD_ELEMENT, List.of(elementType, corner));

            if (this.tile != null) {
                if (!corner.isAdjacent(this.tile)) {
                    throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TILE);
                }

                var tile = game.getTile(this.tile)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                if (tile.getType() != TileType.SEA && tile.getType() != TileType.WETLAND) {
                    throw new DominantSpeciesException(DominantSpeciesError.INVALID_TILE);
                }

                if (species > 0) {
                    game.getAnimal(game.getCurrentAnimal()).removeSpeciesFromGenePool(species);
                    tile.addSpecies(game.getCurrentAnimal(), species);

                    game.fireEvent(Event.Type.ADD_SPECIES, List.of(species, this.tile, tile.getType()));
                }
            }

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
                var hex = tiles.get(i);
                var tile = game.getTile(hex)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var animalType = animals.get(i);

                tile.removeSpecies(animalType);
                game.getAnimal(animalType).addEliminatedSpecies();

                game.fireEvent(Event.Type.ELIMINATE_SPECIES, List.of(animalType, 1, hex, tile.getType()));
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

        Hex tile;
        Set<Corner> elements;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (!elements.stream().allMatch(corner -> corner.isAdjacent(tile))) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_1_TILE);
            }

            var elementsOnTile = game.getAdjacentElementTypes(tile);
            if (elements.size() < elementsOnTile.size() - 1) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ALL_BUT_1_ELEMENT_ON_TILE);
            }

            for (var corner : elements) {
                var elementType = game.removeElement(corner);

                game.fireEvent(Event.Type.REMOVE_ELEMENT, List.of(elementType, corner));
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

            game.fireEvent(Event.Type.CATASTROPHE, List.of(keep, this.tile, tile.getType()));

            if (tile.getTotalSpecies() > 0) {
                if (!tile.hasSpecies(keep)) {
                    throw new DominantSpeciesException(DominantSpeciesError.NOT_ENOUGH_SPECIES_ON_TILE);
                }

                for (var animalType : tile.getSpecies().keySet()) {
                    var speciesOnTile = tile.getSpecies(animalType);

                    if (speciesOnTile > 0) {
                        var speciesToEliminate = animalType != keep ? speciesOnTile : (speciesOnTile > 1 ? speciesOnTile - 1 : 0);

                        if (speciesToEliminate > 0) {

                            tile.removeSpecies(animalType, speciesToEliminate);
                            game.getAnimal(animalType).addEliminatedSpecies(speciesToEliminate);

                            game.fireEvent(Event.Type.ELIMINATE_SPECIES, List.of(animalType, speciesToEliminate, this.tile, tile.getType()));
                        }
                    }
                }
            }

            for (var i = 0; i < adjacentTiles.size(); i++) {
                var adjacentHex = adjacentTiles.get(i);
                var adjacentTile = game.getTile(adjacentHex)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var animalType = animals.get(i);

                if (adjacentTile.getTotalSpecies() > 0) {
                    adjacentTile.removeSpecies(animalType);
                    game.getAnimal(animalType).addEliminatedSpecies();

                    game.fireEvent(Event.Type.ELIMINATE_SPECIES, List.of(animalType, 1, adjacentHex, adjacentTile.getType()));
                }
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
                var hex = tiles.get(i);
                var tile = game.getTile(hex)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

                var animal = game.getAnimal(animals.get(i));

                currentAnimal.removeSpeciesFromGenePool();

                tile.removeSpecies(animal.getType());
                animal.addSpeciesToGenePool(1);
                game.fireEvent(Event.Type.REMOVE_SPECIES, List.of(animal.getType(), 1, hex, tile.getType()));

                tile.addSpecies(game.getCurrentAnimal());
                game.fireEvent(Event.Type.ADD_SPECIES, List.of(1, hex, tile.getType()));
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
                    .forEach(tile -> {
                        game.getAnimal(game.getCurrentAnimal()).removeSpeciesFromGenePool();
                        tile.getValue().addSpecies(game.getCurrentAnimal());
                        game.fireEvent(Event.Type.ADD_SPECIES, List.of(1, tile.getKey(), tile.getValue().getType()));
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

            var vps = tile.getTotalSpecies();
            game.getAnimal(game.getCurrentAnimal()).addVPs(vps);
            game.fireEvent(Event.Type.GAIN_VPS, List.of(vps));

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

            game.fireEvent(Event.Type.ADD_ELEMENT, List.of(elementType, corner));

            return ActionResult.undoAllowed();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
    public static final class Hibernation extends Action {

        public static final int MAX_SPECIES = 5;

        Hex tile;
        int species;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (species > MAX_SPECIES) {
                throw new DominantSpeciesException(DominantSpeciesError.MAX_5_ELIMINATED_SPECIES_ALLOWED);
            }

            var animal = game.getAnimal(game.getCurrentAnimal());

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            animal.removeEliminatedSpecies(species);
            tile.addHibernatingSpecies(animal.getType(), species);

            game.fireEvent(Event.Type.HIBERNATION, List.of(species, this.tile, tile.getType()));

            return ActionResult.undoAllowed();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class MassExodus extends Action {

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static final class Move {
            Hex to;
            AnimalType animal;
            int species;

            private Move combine(Move other) {
                if (other.to != to || other.animal != animal) {
                    throw new IllegalArgumentException("Cannot combine moves for different tiles or animals");
                }
                return new Move(to, animal, species + other.species);
            }
        }

        @Value
        private static class Pair<A, B> {
            A a;
            B b;
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

            var normalized = moves.stream().collect(Collectors.groupingBy(move -> new Pair<>(move.getTo(), move.getAnimal()),
                    Collectors.reducing(Move::combine)))
                    .values().stream()
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            for (var move : normalized) {
                var to = game.getTile(move.to)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                from.removeSpecies(move.animal, move.species);
                to.addSpecies(move.animal, move.species);

                game.fireEvent(Event.Type.MOVE_SPECIES, List.of(move.animal, move.species, this.from, from.getType(), move.to, to.getType()));
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

            game.fireEvent(Event.Type.REMOVE_ELEMENT_FROM_ANIMAL, List.of(from));
            game.fireEvent(Event.Type.ADD_ELEMENT_TO_ANIMAL, List.of(to));

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

            game.fireEvent(Event.Type.CARD, List.of(card));

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
            if (!tiles.containsAll(occupiedTiles)) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_ALL_TILES_OCCUPIED_BY_PLAYER);
            }

            if (animals.contains(game.getCurrentAnimal())) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_SELECT_OPPOSING_SPECIES);
            }

            for (var i = 0; i < tiles.size(); i++) {
                var hex = tiles.get(i);
                var tile = game.getTile(hex)
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));
                var animalType = animals.get(i);

                if (tile.hasOpposingSpecies(game.getCurrentAnimal())) {
                    tile.removeSpecies(animalType);
                    game.getAnimal(animalType).addEliminatedSpecies();

                    game.fireEvent(Event.Type.ELIMINATE_SPECIES, List.of(animalType, 1, hex, tile.getType()));
                }
            }

            return ActionResult.undoAllowed();
        }

        public static Set<Hex> getOccupiedTiles(DominantSpecies game) {
            return game.getTilesWithSpecies(game.getCurrentAnimal());
        }
    }

    @Data
    @AllArgsConstructor
    public static class SaveFromExtinction extends Action {

        Hex tile;

        @Override
        ActionResult perform(DominantSpecies game, Random random) {
            if (game.getCurrentAnimal() != AnimalType.MAMMALS) {
                throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_MAMMALS);
            }

            var tile = game.getTile(this.tile)
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.TILE_NOT_FOUND));

            game.fireEvent(Event.Type.SAVE_FROM_EXTINCTION, List.of(this.tile, tile.getType()));

            game.extinction(this.tile, random);

            return ActionResult.undoAllowed();
        }
    }
}
