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

    ActionDisplay actionDisplay;
    List<String> actions;
    Map<AnimalType, Animal> animals;
    Set<Card> availableCards;
    int availableTundraTiles;
    boolean canUndo;
    AnimalType currentAnimal;
    List<ElementView> elements;
    int deckSize;
    DrawBag drawBag;
    List<AnimalType> initiativeTrack;
    Hex lastPlacedTile;
    List<Hex> scoredTiles;
    Phase phase;
    Map<String, List<AnimalType>> players;
    int round;
    List<TileView> tiles;
    List<StackView> wanderlustTiles;

    public DominantSpeciesView(DominantSpecies state) {
        this.actionDisplay = state.getActionDisplay();
        this.actions = state.possibleActions().stream()
                .map(Action::getName)
                .collect(Collectors.toList());
        this.animals = state.getAnimals();
        this.availableCards = state.getAvailableCards();
        this.availableTundraTiles = state.getAvailableTundraTiles();
        this.canUndo = state.canUndo();
        this.currentAnimal = state.getCurrentAnimal();
        this.deckSize = state.getDeckSize();
        this.drawBag = state.getDrawBag();
        this.elements = state.getElements().entrySet().stream().map(entry -> new ElementView(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        this.initiativeTrack = state.getInitiativeTrack();
        this.lastPlacedTile = state.getLastPlacedTile().orElse(null);
        this.scoredTiles = state.getScoredTiles();
        this.round = state.getRound();
        this.phase = state.getPhase();
        var values = state.getAnimals().values();
        var stream = values
                .stream();
        this.players = stream
                .collect(Collectors.groupingBy(animal -> animal.getPlayer().getName(), Collectors.mapping(Animal::getType, Collectors.toList())));
        this.tiles = state.getTiles().entrySet().stream().map(entry -> new TileView(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        this.wanderlustTiles = IntStream.range(0, 3)
                .mapToObj(i -> new StackView(state.getWanderlustTiles().getStack(i)))
                .collect(Collectors.toList());
    }

}
