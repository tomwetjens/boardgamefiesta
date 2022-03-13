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
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DominantSpeciesTest {

    Player playerA = new Player("Player A", PlayerColor.BLACK, Player.Type.HUMAN);
    Player playerB = new Player("Player B", PlayerColor.RED, Player.Type.HUMAN);
    Player playerC = new Player("Player C", PlayerColor.YELLOW, Player.Type.HUMAN);
    Player playerD = new Player("Player D", PlayerColor.GREEN, Player.Type.HUMAN);
    Player playerE = new Player("Player E", PlayerColor.WHITE, Player.Type.HUMAN);
    Player playerF = new Player("Player F", PlayerColor.BLUE, Player.Type.HUMAN);

    @Nested
    class Start {

        @Test
        void twoPlayers() {
            var ds = DominantSpecies.start(Set.of(playerA, playerB), new Random(0));

            assertThat(ds.getRound()).isEqualTo(1);
            assertThat(ds.getPhase()).isEqualTo(Phase.PLANNING);
            assertThat(ds.getInitiativeTrack()).containsExactly(AnimalType.ARACHNIDS, AnimalType.REPTILES);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);

            assertThat(ds.getAnimals()).containsOnlyKeys(AnimalType.ARACHNIDS, AnimalType.REPTILES);
            assertThat(ds.getAnimals().get(AnimalType.ARACHNIDS).getPlayer()).isSameAs(playerA);
            assertThat(ds.getAnimals().get(AnimalType.REPTILES).getPlayer()).isSameAs(playerB);

            assertThat(ds.getAnimals()).allSatisfy((animalType, animal) -> {
                assertThat(animal.getType()).isEqualTo(animalType);
                assertThat(animal.getGenePool()).isEqualTo(55);
                assertThat(animal.getActionPawns()).isEqualTo(7);
            });

            assertThat(ds.getTiles()).hasSize(7);
            assertThat(ds.getElements()).hasSize(12);

            assertThat(ds.getCurrentPlayers()).containsExactly(playerA);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);
        }
    }

    @Nested
    class Planning {

        @Test
        void placeActionPawnsUntilNobodyHasActionPawnsLeft() {
            var ds = DominantSpecies.start(Set.of(playerA, playerB, playerC), new Random(0));

            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);
            assertThat(ds.getCurrentPlayers()).containsExactly(playerA);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);

            ds.perform(new Action.PlaceActionPawn(ActionType.INITIATIVE, 0), new Random(0));
            assertThat(ds.possibleActions()).isEmpty();
            ds.endTurn(new Random(0));

            assertThat(ds.getActionDisplay().getActionPawns().get(ActionType.INITIATIVE)).containsExactly(AnimalType.ARACHNIDS);
            assertThat(ds.getAnimals().get(AnimalType.ARACHNIDS).getActionPawns()).isEqualTo(5);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.BIRDS);
            assertThat(ds.getCurrentPlayers()).containsExactly(playerC);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);

            ds.perform(new Action.PlaceActionPawn(ActionType.ADAPTATION, 0), new Random(0));
            assertThat(ds.possibleActions()).isEmpty();
            ds.endTurn(new Random(0));

            assertThat(ds.getActionDisplay().getActionPawns().get(ActionType.ADAPTATION)).containsExactly(AnimalType.BIRDS, null, null);
            assertThat(ds.getAnimals().get(AnimalType.BIRDS).getActionPawns()).isEqualTo(5);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);
            assertThat(ds.getCurrentPlayers()).containsExactly(playerB);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);

            ds.perform(new Action.PlaceActionPawn(ActionType.REGRESSION, 0), new Random(0));
            assertThat(ds.possibleActions()).isEmpty();
            ds.endTurn(new Random(0));

            assertThat(ds.getActionDisplay().getActionPawns().get(ActionType.REGRESSION)).containsExactly(AnimalType.REPTILES, null, AnimalType.REPTILES);
            assertThat(ds.getAnimals().get(AnimalType.REPTILES).getActionPawns()).isEqualTo(5);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);
            assertThat(ds.getCurrentPlayers()).containsExactly(playerA);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);

            while (ds.getAnimals().get(ds.getCurrentAnimal()).getActionPawns() > 0
                    && ds.getPhase() == Phase.PLANNING) {
                var placement = ds.getActionDisplay().possiblePlacements()
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("No available action space"));

                ds.perform(new Action.PlaceActionPawn(placement.getActionType(), placement.getIndex()), new Random(0));
                ds.endTurn(new Random(0));
            }

            assertThat(ds.getPhase()).isEqualTo(Phase.EXECUTION);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);
        }

        @Test
        void skipPlayerIfNoActionPawnsLeft() {
            var drawBag = DrawBag.initial();
            var ds = DominantSpecies.builder()
                    .phase(Phase.PLANNING)
                    .round(1)
                    .currentAnimal(AnimalType.ARACHNIDS)
                    .initiativeTrack(List.of(AnimalType.ARACHNIDS, AnimalType.REPTILES, AnimalType.MAMMALS))
                    .animals(Map.of(
                            AnimalType.ARACHNIDS, Animal.builder().type(AnimalType.ARACHNIDS).player(playerA).actionPawns(5).build(),
                            AnimalType.REPTILES, Animal.builder().type(AnimalType.REPTILES).player(playerB).actionPawns(3).build(),
                            AnimalType.MAMMALS, Animal.builder().type(AnimalType.MAMMALS).player(playerC).actionPawns(1).build()
                    ))
                    .tiles(DominantSpecies.createInitialTiles())
                    .elements(DominantSpecies.createInitialElements())
                    .currentAnimal(AnimalType.ARACHNIDS)
                    .drawBag(drawBag)
                    .actionDisplay(ActionDisplay.initial(Set.of(AnimalType.ARACHNIDS, AnimalType.REPTILES, AnimalType.MAMMALS), drawBag, new Random(0)))
                    .actionQueue(ActionQueue.initial(PossibleAction.mandatory(AnimalType.ARACHNIDS, Action.PlaceActionPawn.class)))
                    .build();

            while (ds.getAnimals().get(ds.getCurrentAnimal()).getActionPawns() > 0
                    && ds.getPhase() == Phase.PLANNING) {
                var placement = ds.getActionDisplay().possiblePlacements()
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("No available action space"));

                ds.perform(new Action.PlaceActionPawn(placement.getActionType(), placement.getIndex()), new Random(0));
                assertThat(ds.possibleActions()).isEmpty();
                ds.endTurn(new Random(0));
            }

            assertThat(ds.getPhase()).isEqualTo(Phase.EXECUTION);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void allActionTypes() {
            var drawBag = DrawBag.initial();
            var ds = DominantSpecies.builder()
                    .phase(Phase.PLANNING)
                    .round(1)
                    .currentAnimal(AnimalType.ARACHNIDS)
                    .initiativeTrack(Arrays.asList(AnimalType.ARACHNIDS, AnimalType.REPTILES, AnimalType.MAMMALS))
                    .animals(Map.of(
                            AnimalType.ARACHNIDS, Animal.builder()
                                    .type(AnimalType.ARACHNIDS)
                                    .player(playerA)
                                    .genePool(50)
                                    .elements(new ArrayList<>(AnimalType.ARACHNIDS.getInitialElements()))
                                    .build(),
                            AnimalType.REPTILES, Animal.builder()
                                    .type(AnimalType.REPTILES)
                                    .player(playerB)
                                    .genePool(50)
                                    .elements(new ArrayList<>(AnimalType.REPTILES.getInitialElements()))
                                    .build(),
                            AnimalType.MAMMALS, Animal.builder()
                                    .type(AnimalType.MAMMALS)
                                    .player(playerC)
                                    .genePool(50)
                                    .elements(new ArrayList<>(AnimalType.MAMMALS.getInitialElements()))
                                    .build()
                    ))
                    .tiles(DominantSpecies.createInitialTiles())
                    .elements(DominantSpecies.createInitialElements())
                    .currentAnimal(AnimalType.MAMMALS)
                    .drawBag(drawBag)
                    .wanderlustTiles(WanderlustTiles.initial(new Random(0)))
                    .actionDisplay(ActionDisplay.initial(Set.of(AnimalType.ARACHNIDS, AnimalType.REPTILES, AnimalType.MAMMALS), drawBag, new Random(0))
                            .resetFreeActionPawns(Set.of(AnimalType.ARACHNIDS, AnimalType.REPTILES, AnimalType.MAMMALS))
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.INITIATIVE, 0)
                            .placeActionPawn(AnimalType.ARACHNIDS, ActionType.ADAPTATION, 0)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.ADAPTATION, 1)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.ADAPTATION, 2)
                            .placeActionPawn(AnimalType.ARACHNIDS, ActionType.REGRESSION, 0)
                            .addElement(ActionType.REGRESSION, ElementType.GRASS)
                            .addElement(ActionType.REGRESSION, ElementType.GRUB)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.ABUNDANCE, 0)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.WASTELAND, 0)
                            .addElement(ActionType.WASTELAND, ElementType.SUN)
                            .addElement(ActionType.WASTELAND, ElementType.GRASS)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.DEPLETION, 0)
                            .addElement(ActionType.DEPLETION, ElementType.SEED)
                            .placeActionPawn(AnimalType.ARACHNIDS, ActionType.GLACIATION, 0)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.GLACIATION, 1)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.GLACIATION, 2)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.SPECIATION, 0)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.SPECIATION, 1)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.WANDERLUST, 0)
                            .addElement(ActionType.WANDERLUST, ElementType.GRASS)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.MIGRATION, 0)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.COMPETITION, 1)
                            .placeActionPawn(AnimalType.ARACHNIDS, ActionType.DOMINATION, 0)
                            .placeActionPawn(AnimalType.REPTILES, ActionType.DOMINATION, 1)
                            .placeActionPawn(AnimalType.MAMMALS, ActionType.DOMINATION, 2)
                    )
                    .actionQueue(ActionQueue.create())
                    .availableTundraTiles(11)
                    .build();

            ds.endTurn(new Random(0));

            assertThat(ds.getPhase()).isEqualTo(Phase.EXECUTION);

            // == Initiative

            // Initiative track is updated automatically
            assertThat(ds.getInitiativeTrack()).containsExactly(AnimalType.ARACHNIDS, AnimalType.MAMMALS, AnimalType.REPTILES);

            // Make last AP placement because of Initiative
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);
            assertThat(ds.possibleActions()).containsExactly(Action.PlaceActionPawn.class);
            assertThat(ds.getAnimals().get(AnimalType.MAMMALS).getActionPawns()).isEqualTo(0);

            ds.perform(new Action.PlaceActionPawn(ActionType.ABUNDANCE, 1), new Random(0));
            ds.endTurn(new Random(0));

            assertThat(ds.getAnimals().get(AnimalType.MAMMALS).getActionPawns()).isEqualTo(0);

            // == Adaptation

            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);
            assertThat(ds.possibleActions()).containsExactly(Action.Adaptation.class);

            var adaptationArachnids = ElementType.GRASS;
            ds.perform(new Action.Adaptation(adaptationArachnids), new Random(0));
            ds.endTurn(new Random(0));

            assertThat(ds.getAnimals().get(AnimalType.ARACHNIDS).getElements()).containsExactly(ElementType.GRUB, ElementType.GRUB, adaptationArachnids);

            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);
            assertThat(ds.possibleActions()).containsExactly(Action.Adaptation.class);

            var adaptationReptiles = ds.getActionDisplay().getElements().get(ActionType.ADAPTATION).get(0);
            ds.perform(new Action.Adaptation(adaptationReptiles), new Random(0));
            ds.endTurn(new Random(0));

            assertThat(ds.getAnimals().get(AnimalType.REPTILES).getElements()).containsExactly(ElementType.SUN, ElementType.SUN, adaptationReptiles);

            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);
            assertThat(ds.possibleActions()).containsExactly(Action.Adaptation.class);

            var adaptationMammals = ds.getActionDisplay().getElements().get(ActionType.ADAPTATION).get(0);
            ds.perform(new Action.Adaptation(adaptationMammals), new Random(0));
            ds.endTurn(new Random(0));

            assertThat(ds.getAnimals().get(AnimalType.MAMMALS).getActionPawns()).isEqualTo(1);
            assertThat(ds.getAnimals().get(AnimalType.MAMMALS).getElements()).containsExactly(ElementType.MEAT, ElementType.MEAT, adaptationMammals);

            // == Regression

            assertThat(ds.possibleActions()).containsExactly(Action.Regression.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);

            ds.perform(new Action.Regression(Set.of(adaptationReptiles)), new Random(0));

            assertThat(ds.getAnimals().get(AnimalType.REPTILES).getElements()).containsExactlyInAnyOrder(ElementType.SUN, ElementType.SUN, adaptationReptiles);
            assertThat(ds.getAnimals().get(AnimalType.REPTILES).getActionPawns()).isEqualTo(1); // Not returned the 'free AP'

            ds.endTurn(new Random(0));

            assertThat(ds.possibleActions()).containsExactly(Action.Regression.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);

            ds.perform(new Action.Regression(Set.of(adaptationArachnids)), new Random(0));

            assertThat(ds.getAnimals().get(AnimalType.ARACHNIDS).getActionPawns()).isEqualTo(2);
            assertThat(ds.getAnimals().get(AnimalType.ARACHNIDS).getElements()).containsExactlyInAnyOrder(ElementType.GRUB, ElementType.GRUB, adaptationArachnids);

            ds.endTurn(new Random(0));

            // == Abundance

            assertThat(ds.possibleActions()).containsExactly(Action.Abundance.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);

            var abundanceCorner1 = new Corner(DominantSpecies.INITIAL_FOREST, new Hex(-2, 1), new Hex(-2, 2));
            ds.perform(new Action.Abundance(ElementType.SEED, abundanceCorner1), new Random(0));

            assertThat(ds.getElements()).hasSize(13);
            assertThat(ds.getElements()).containsEntry(abundanceCorner1, ElementType.SEED);
            assertThat(ds.getAnimals().get(AnimalType.MAMMALS).getActionPawns()).isEqualTo(2);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);
            assertThat(ds.possibleActions()).containsExactly(Action.Abundance.class);

            var abundanceCorner2 = new Corner(DominantSpecies.INITIAL_JUNGLE, new Hex(-2, 0), new Hex(-2, 1));
            ds.perform(new Action.Abundance(ElementType.MEAT, abundanceCorner2), new Random(0));
            ds.endTurn(new Random(0));

            assertThat(ds.getElements()).hasSize(14);
            assertThat(ds.getElements()).containsEntry(abundanceCorner2, ElementType.MEAT);
            assertThat(ds.getAnimals().get(AnimalType.MAMMALS).getActionPawns()).isEqualTo(3);

            // == Wasteland

            assertThat(ds.possibleActions()).containsExactly(Action.Wasteland.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);

            ds.perform(new Action.Wasteland(ElementType.GRASS), new Random(0));
            ds.endTurn(new Random(0));

            // Wasteland element(s) should be removed from tundra tiles
            assertThat(ds.getAdjacentElementTypes(DominantSpecies.INITIAL_SEA))
                    .containsExactlyInAnyOrder(ElementType.SEED, ElementType.GRASS, ElementType.MEAT, ElementType.WATER, ElementType.GRUB);

            // == Depletion

            assertThat(ds.possibleActions()).containsExactly(Action.Depletion.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);
            assertThat(ds.getDrawBag().count(ElementType.SEED)).isEqualTo(17);

            var depletionCorner = new Corner(DominantSpecies.INITIAL_JUNGLE, DominantSpecies.INITIAL_FOREST, DominantSpecies.INITIAL_SEA);
            ds.perform(new Action.Depletion(depletionCorner), new Random(0));

            // Element should have been removed from Earth and placed back into the Draw Bag
            assertThat(ds.getElement(depletionCorner)).isEmpty();
            assertThat(ds.getDrawBag().count(ElementType.SEED)).isEqualTo(18);

            ds.endTurn(new Random(0));

            // == Glaciation

            assertThat(ds.possibleActions()).containsExactly(Action.Glaciation.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);

            ds.perform(new Action.Glaciation(DominantSpecies.INITIAL_SAVANNAH), new Random(0));

            assertThat(ds.getTile(DominantSpecies.INITIAL_SAVANNAH).get().isTundra()).isTrue();
            // Other APs are still on Glaciation
            assertThat(ds.getActionDisplay().getActionPawns().get(ActionType.GLACIATION)).containsExactly(null, AnimalType.REPTILES, AnimalType.MAMMALS, null);

            ds.endTurn(new Random(0));

            // == Speciation

            assertThat(ds.possibleActions()).containsExactly(Action.Speciation.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);

            ds.perform(new Action.Speciation(new Corner(DominantSpecies.INITIAL_MOUNTAIN, DominantSpecies.INITIAL_DESERT, new Hex(1, 1)),
                    List.of(DominantSpecies.INITIAL_MOUNTAIN, DominantSpecies.INITIAL_DESERT),
                    List.of(2, 2)), new Random(0));

            assertThat(ds.getTile(DominantSpecies.INITIAL_MOUNTAIN).get().getSpecies(AnimalType.REPTILES)).isEqualTo(2);
            assertThat(ds.getTile(DominantSpecies.INITIAL_DESERT).get().getSpecies(AnimalType.REPTILES)).isEqualTo(2);

            ds.endTurn(new Random(0));

            assertThat(ds.possibleActions()).containsExactly(Action.Speciation.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);

            ds.perform(new Action.Speciation(new Corner(DominantSpecies.INITIAL_DESERT, DominantSpecies.INITIAL_SAVANNAH, new Hex(2, -1)),
                    List.of(DominantSpecies.INITIAL_DESERT),
                    List.of(2)), new Random(0));

            assertThat(ds.getTile(DominantSpecies.INITIAL_DESERT).get().getSpecies(AnimalType.MAMMALS)).isEqualTo(2);

            ds.endTurn(new Random(0));

            // == Wanderlust

            assertThat(ds.possibleActions()).containsExactly(Action.Wanderlust.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);

            var wanderlustTileType = ds.getWanderlustTiles().getStack(0).getFaceUp().get();
            var wanderlustHex = new Hex(1, 1);
            var wanderlustElementType = ds.getActionDisplay().getElements().get(ActionType.WANDERLUST).get(0);
            var wanderlustCorner = new Corner(DominantSpecies.INITIAL_MOUNTAIN, wanderlustHex, new Hex(0, 2));

            ds.perform(new Action.Wanderlust(0, wanderlustHex, wanderlustElementType, wanderlustCorner), new Random(0));

            assertThat(ds.getTile(wanderlustHex).get().getType()).isEqualTo(wanderlustTileType);
            assertThat(ds.getTile(wanderlustHex).get().isTundra()).isFalse();
            assertThat(ds.getTile(wanderlustHex).get().getTotalSpecies()).isEqualTo(0);
            assertThat(ds.getElement(wanderlustCorner).get()).isEqualTo(wanderlustElementType);

            ds.endTurn(new Random(0));

            assertThat(ds.possibleActions()).containsExactly(Action.WanderlustMove.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);

            ds.perform(new Action.WanderlustMove(List.of(
                    new Action.WanderlustMove.Move(DominantSpecies.INITIAL_DESERT, 2)
            )), new Random(0));

            ds.endTurn(new Random(0));

            assertThat(ds.getTile(wanderlustHex).get().getSpecies(AnimalType.MAMMALS)).isEqualTo(2);
            assertThat(ds.getTile(DominantSpecies.INITIAL_DESERT).get().getSpecies(AnimalType.MAMMALS)).isEqualTo(0);

            assertThat(ds.possibleActions()).containsExactly(Action.WanderlustMove.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);

            ds.perform(new Action.WanderlustMove(List.of(
                    new Action.WanderlustMove.Move(DominantSpecies.INITIAL_MOUNTAIN, 2),
                    new Action.WanderlustMove.Move(DominantSpecies.INITIAL_DESERT, 1)
            )), new Random(0));

            ds.endTurn(new Random(0));

            assertThat(ds.getTile(wanderlustHex).get().getSpecies(AnimalType.REPTILES)).isEqualTo(3);
            assertThat(ds.getTile(DominantSpecies.INITIAL_MOUNTAIN).get().getSpecies(AnimalType.REPTILES)).isEqualTo(0);
            assertThat(ds.getTile(DominantSpecies.INITIAL_DESERT).get().getSpecies(AnimalType.REPTILES)).isEqualTo(1);

            // == Migration

            assertThat(ds.possibleActions()).containsExactly(Action.Migration.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);

            ds.perform(new Action.Migration(List.of(
                    new Action.Migration.Move(DominantSpecies.INITIAL_DESERT, DominantSpecies.INITIAL_SAVANNAH, 1)
            )), new Random(0));

            ds.endTurn(new Random(0));

            assertThat(ds.getTile(DominantSpecies.INITIAL_SAVANNAH).get().getSpecies(AnimalType.REPTILES)).isEqualTo(1);
            assertThat(ds.getTile(DominantSpecies.INITIAL_DESERT).get().getSpecies(AnimalType.REPTILES)).isEqualTo(0);

            // == Competition

            assertThat(ds.possibleActions()).containsExactly(Action.Competition.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS); // Free AP

            ds.endTurn(new Random(0)); // Skip

            assertThat(ds.possibleActions()).containsExactly(Action.Competition.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.MAMMALS);

            // TODO Skipped for now, as there are no tiles that qualify
            ds.endTurn(new Random(0));

            // == Domination

            // TODO

            assertThat(ds.possibleActions()).containsExactly(Action.Domination.class);
            assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);
        }

        @Test
        void removeAllWastelandElementsFromTundraEvenIfNoWastelandActionPawnWasPlaced() {
            // TODO
        }

        @Test
        void removeAllWastelandElementsFromTundraEvenIfWastelandActionIsSkipped() {
            // TODO
        }

        @Nested
        class Regression {

            @Test
            void regressionNoElements() {
                // TODO
            }

            @Test
            void regressionArachnidsPlayerNoActionPawns() {
                var ds = DominantSpecies.start(Set.of(playerA, playerB), new Random(0));
                assertThat(ds.getAnimals()).containsKeys(AnimalType.REPTILES, AnimalType.ARACHNIDS);
                assertThat(ds.getAnimal(AnimalType.REPTILES).getPlayer()).isSameAs(playerB);
                assertThat(ds.getAnimal(AnimalType.ARACHNIDS).getPlayer()).isSameAs(playerA);

                ds.getActionDisplay().addElement(ActionType.REGRESSION, ElementType.GRASS);

                // Both animals could lose this element
                ds.getAnimal(AnimalType.REPTILES).addElement(ElementType.GRASS);
                ds.getAnimal(AnimalType.ARACHNIDS).addElement(ElementType.GRASS);

                // Both animals do not place any AP at regression (reptiles get 1 free AP there)
                placeAllRemainingActionPawns(ds, ActionType.INITIATIVE, ActionType.REGRESSION, ActionType.ADAPTATION);

                // Then we expect Regression to have executed automatically
                // Reptiles should have kept their element because of the free AP
                assertThat(ds.getAnimal(AnimalType.REPTILES).getElements()).containsExactly(ElementType.SUN, ElementType.SUN, ElementType.GRASS);
                // Arachnids should have removed their element
                assertThat(ds.getAnimal(AnimalType.ARACHNIDS).getElements()).containsExactly(ElementType.GRUB, ElementType.GRUB);
                // Game should continue with next AP
                assertThat(ds.possibleActions()).containsExactly(Action.Abundance.class);
                assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.ARACHNIDS);
            }

            @Test
            void regressionReptilePlayerNoActionPawns() {
                var ds = DominantSpecies.start(Set.of(playerA, playerB), new Random(0));
                assertThat(ds.getAnimals()).containsKeys(AnimalType.REPTILES, AnimalType.ARACHNIDS);
                assertThat(ds.getAnimal(AnimalType.REPTILES).getPlayer()).isSameAs(playerB);
                assertThat(ds.getAnimal(AnimalType.ARACHNIDS).getPlayer()).isSameAs(playerA);

                // Make sure there is more than 1 element in the Regression box, so the Reptile's free AP doesn't allow them to skip entirely
                ds.getActionDisplay().addElement(ActionType.REGRESSION, ElementType.GRASS);
                ds.getActionDisplay().addElement(ActionType.REGRESSION, ElementType.MEAT);

                // Both animals could lose both these element types
                ds.getAnimal(AnimalType.REPTILES).addElement(ElementType.GRASS);
                ds.getAnimal(AnimalType.REPTILES).addElement(ElementType.MEAT);
                ds.getAnimal(AnimalType.ARACHNIDS).addElement(ElementType.GRASS);
                ds.getAnimal(AnimalType.ARACHNIDS).addElement(ElementType.MEAT);

                // Both animals do not place any(more) APs at Regression (Reptiles get 1 free AP there)
                placeAllRemainingActionPawns(ds, ActionType.INITIATIVE, ActionType.REGRESSION, ActionType.ADAPTATION);
                assertThat(ds.getPhase()).isEqualTo(Phase.EXECUTION);

                // Reptiles can skip 1 element, but should decide which 1 element is removed
                assertThat(ds.possibleActions()).containsExactly(Action.Regression.class);
                assertThat(ds.getCurrentAnimal()).isEqualTo(AnimalType.REPTILES);
                ds.perform(new Action.Regression(Set.of(ElementType.GRASS)), new Random(0));
                ds.endTurn(new Random(0));

                assertThat(ds.getAnimal(AnimalType.REPTILES).getElements()).containsExactly(ElementType.SUN, ElementType.SUN, ElementType.GRASS);
            }

            @Test
            void regressionPlayerDoesntHaveElementOfType() {

            }

            @Test
            void regressionPlayesActionPawnsEqualOrGreaterThanUniqueElements() {
                // TODO
            }

            @Test
            void regressionPlayerLessActionPawnsThanElements() {
                // TODO
            }

            @Test
            void regressionPlayerActionPawnsEqualToElements() {
                // TODO
            }
        }
    }

    void placeAllRemainingActionPawns(DominantSpecies ds, ActionType... exclude) {
        while (ds.getAnimals().get(ds.getCurrentAnimal()).getActionPawns() > 0
                && ds.getPhase() == Phase.PLANNING) {
            var placement = ds.getActionDisplay().possiblePlacements()
                    .filter(p -> !Arrays.asList(exclude).contains(p.getActionType()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No available action space"));

            ds.perform(new Action.PlaceActionPawn(placement.getActionType(), placement.getIndex()), new Random(0));
            ds.endTurn(new Random(0));
        }
    }
}