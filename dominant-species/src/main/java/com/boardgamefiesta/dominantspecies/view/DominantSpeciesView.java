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

package com.boardgamefiesta.dominantspecies.view;

import com.boardgamefiesta.dominantspecies.logic.*;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class DominantSpeciesView {

    int round;
    Phase phase;
    Map<AnimalType, Animal> animals;
    List<AnimalType> initiativeTrack;
    Map<Hex, Tile> tiles;
    Map<Corner, ElementType> elements;
    ActionDisplay actionDisplay;
    DrawBag drawBag;
    AnimalType currentAnimal;
    ActionQueue actionQueue;
    int deckSize;
    Set<Card> availableCards;
    int availableTundraTiles;
    List<StackView> wanderlustTiles;

    boolean canUndo;
    List<String> actions;

    public DominantSpeciesView(DominantSpecies state) {
        this.round = state.getRound();
        this.phase = state.getPhase();
        this.animals = state.getAnimals();
        this.initiativeTrack = state.getInitiativeTrack();
        this.tiles = state.getTiles();
        this.elements = state.getElements();
        this.actionDisplay = state.getActionDisplay();
        this.drawBag = state.getDrawBag();
        this.currentAnimal = state.getCurrentAnimal();
        this.deckSize = state.getDeckSize();
        this.availableCards = state.getAvailableCards();
        this.availableTundraTiles = state.getAvailableTundraTiles();
        this.wanderlustTiles = IntStream.range(0, 3)
                .mapToObj(i -> new StackView(state.getWanderlustTiles().getStack(i)))
                .collect(Collectors.toList());

        this.canUndo = state.canUndo();
        this.actions = state.possibleActions().stream()
                .map(Action::getName)
                .collect(Collectors.toList());
    }

    @Data
    public static class StackView {

        TileType faceUp;
        int size;

        public StackView(WanderlustTiles.Stack stack) {
            this.faceUp = stack.getFaceUp().orElse(null);
            this.size = stack.size();
        }
    }
}
