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

import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.api.domain.Stats;
import lombok.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class DominantSpecies implements State {

    static final Hex INITIAL_JUNGLE = new Hex(-1, 0);
    static final Hex INITIAL_WETLAND = new Hex(0, -1);
    static final Hex INITIAL_SAVANNAH = new Hex(1, -1);
    static final Hex INITIAL_SEA = new Hex(0, 0);
    static final Hex INITIAL_FOREST = new Hex(-1, 1);
    static final Hex INITIAL_MOUNTAIN = new Hex(0, 1);
    static final Hex INITIAL_DESERT = new Hex(1, 0);

    /**
     * (Empty) hexes that make up the Dominant Species board.
     */
    static final Set<Hex> HEXES = IntStream.range(0, 7)
            .map(index -> index - 3)
            .mapToObj(q -> {
                var m = q == 0 ? 5 : 7 - Math.abs(q);
                return IntStream.range(0, m)
                        .map(index -> q == 0 ? index - 2 : q < 0 ? index - (m - 3) + 1 : index - 3)
                        .mapToObj(r -> new Hex(q, r));
            })
            .flatMap(Function.identity())
            .collect(Collectors.toSet());

    static final Map<TileType, List<Integer>> TILE_SCORING = Map.of(
            TileType.SEA, List.of(9, 5, 3, 2),
            TileType.WETLAND, List.of(8, 4, 2, 1),
            TileType.SAVANNAH, List.of(7, 4, 2),
            TileType.JUNGLE, List.of(6, 3, 2),
            TileType.FOREST, List.of(5, 3, 2),
            TileType.DESERT, List.of(4, 2),
            TileType.MOUNTAIN, List.of(3, 2)
    );

    @Getter
    private int round;

    @Getter
    private Phase phase;

    @Getter
    private Map<AnimalType, Animal> animals;

    @Getter
    private List<AnimalType> initiativeTrack;

    @Getter
    private Map<Hex, Tile> tiles;

    @Getter
    private Map<Corner, ElementType> elements;

    @Getter
    private ActionDisplay actionDisplay;

    @Getter
    private DrawBag drawBag;

    @Getter
    private AnimalType currentAnimal;

    private ActionQueue actionQueue;

    @Getter
    @Builder.Default
    private final List<Hex> scoredTiles = new LinkedList<>();

    private Queue<Card> deck;

    @Getter
    private Set<Card> availableCards;

    @Getter
    private int availableTundraTiles;

    @Getter
    private WanderlustTiles wanderlustTiles;

    private Hex lastPlacedTile;

    private boolean canUndo;

    private final transient List<EventListener> eventListeners = new LinkedList<>();

    public static DominantSpecies start(Set<Player> players, Random random) {
        var animals = initialRandomAnimals(players, random);
        var initiativeTrack = reverseFoodChainOrder(animals.values());
        var drawBag = DrawBag.initial();

        var game = builder()
                .round(1)
                .phase(Phase.PLANNING)
                .animals(animals)
                .initiativeTrack(initiativeTrack)
                .tiles(createInitialTiles())
                .elements(createInitialElements())
                .drawBag(drawBag)
                .actionDisplay(ActionDisplay.initial(animals.keySet(), drawBag, random))
                .currentAnimal(initiativeTrack.get(0))
                .actionQueue(ActionQueue.initial(PossibleAction.mandatory(initiativeTrack.get(0), Action.PlaceActionPawn.class)))
                .deck(Card.initialDeck(random))
                .availableCards(new HashSet<>())
                .availableTundraTiles(11)
                .wanderlustTiles(WanderlustTiles.initial(random))
                .build();

        game.drawCards();

        return game;
    }

    private void drawCards() {
        while (availableCards.size() < 5 && !deck.isEmpty()) {
            availableCards.add(deck.poll());
        }
    }

    static Map<Corner, ElementType> createInitialElements() {
        var elements = new HashMap<Corner, ElementType>();
        elements.put(new Corner(INITIAL_JUNGLE, INITIAL_WETLAND, INITIAL_SEA), ElementType.GRUB);
        elements.put(new Corner(INITIAL_JUNGLE, INITIAL_FOREST, new Hex(-2, 1)), ElementType.GRUB);
        elements.put(new Corner(INITIAL_SAVANNAH, INITIAL_WETLAND, INITIAL_SEA), ElementType.WATER);
        elements.put(new Corner(INITIAL_JUNGLE, INITIAL_WETLAND, new Hex(-1, -1)), ElementType.WATER);
        elements.put(new Corner(INITIAL_SAVANNAH, INITIAL_DESERT, INITIAL_SEA), ElementType.GRASS);
        elements.put(new Corner(INITIAL_SAVANNAH, INITIAL_WETLAND, new Hex(1, -2)), ElementType.GRASS);
        elements.put(new Corner(INITIAL_MOUNTAIN, INITIAL_DESERT, INITIAL_SEA), ElementType.SUN);
        elements.put(new Corner(INITIAL_SAVANNAH, INITIAL_DESERT, new Hex(2, -1)), ElementType.SUN);
        elements.put(new Corner(INITIAL_MOUNTAIN, INITIAL_FOREST, INITIAL_SEA), ElementType.MEAT);
        elements.put(new Corner(INITIAL_MOUNTAIN, INITIAL_DESERT, new Hex(1, 1)), ElementType.MEAT);
        elements.put(new Corner(INITIAL_JUNGLE, INITIAL_FOREST, INITIAL_SEA), ElementType.SEED);
        elements.put(new Corner(INITIAL_FOREST, INITIAL_MOUNTAIN, new Hex(-1, 2)), ElementType.SEED);
        return elements;
    }

    static Map<Hex, Tile> createInitialTiles() {
        var tiles = new HashMap<Hex, Tile>();
        tiles.put(INITIAL_JUNGLE, Tile.initial(TileType.JUNGLE, false));
        tiles.put(INITIAL_WETLAND, Tile.initial(TileType.WETLAND, false));
        tiles.put(INITIAL_SAVANNAH, Tile.initial(TileType.SAVANNAH, false));
        tiles.put(INITIAL_SEA, Tile.initial(TileType.SEA, true));
        tiles.put(INITIAL_FOREST, Tile.initial(TileType.FOREST, false));
        tiles.put(INITIAL_MOUNTAIN, Tile.initial(TileType.MOUNTAIN, false));
        tiles.put(INITIAL_DESERT, Tile.initial(TileType.DESERT, false));
        return tiles;
    }

    private static List<AnimalType> reverseFoodChainOrder(Collection<Animal> animals) {
        return animals.stream()
                .map(Animal::getType)
                .sorted(Comparator.<AnimalType>comparingInt(AnimalType.FOOD_CHAIN_ORDER::indexOf).reversed())
                .collect(Collectors.toList());
    }

    private static Map<AnimalType, Animal> initialRandomAnimals(Set<Player> players, Random random) {
        var types = new LinkedList<>(Arrays.asList(AnimalType.values()));
        Collections.shuffle(types, random);

        return players.stream()
                .sorted(Comparator.comparing(Player::getName)) // Deterministic order for testing
                .map(player -> Animal.initial(player, types.poll(), players.size()))
                .collect(Collectors.toMap(Animal::getType, Function.identity()));
    }

    public static int bonusVPs(int quantity) {
        if (quantity < 0) throw new IllegalStateException("quantity must be positive: " + quantity);
        switch (quantity) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 10;
            case 5:
                return 15;
            case 6:
                return 21;
            case 7:
                return 28;
            case 8:
                return 36;
            default:
                return 45;
        }
    }

    public static int tileScore(TileType tileType, int place) {
        var scores = TILE_SCORING.get(tileType);
        return scores.size() > place ? scores.get(place) : 0;
    }

    public void perform(Action action, Random random) {
        if (!actionQueue.canPerform(currentAnimal, action.getClass())) {
            throw new DominantSpeciesException(DominantSpeciesError.CANNOT_PERFORM_ACTION);
        }

        var result = action.perform(this, random);

        actionQueue.perform(currentAnimal, action);

        result.getFollowUpActions().addTo(actionQueue);
        canUndo = result.canUndo();

        recalculateDominance();
    }

    private void recalculateDominance() {
        tiles.forEach((hex, tile) -> {
            var adjacentElements = getAdjacentElements(hex);
            tile.recalculateDominance(animals.values(), adjacentElements);
        });
    }

    void removeAllWastelandElementsFromTundraTiles() {
        var wasteland = actionDisplay.getElements().get(ActionType.WASTELAND);

        var elementsToRemove = elements.entrySet().stream()
                .filter(element -> wasteland.contains(element.getValue()) && isTundra(element.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        elementsToRemove.forEach(elements::remove);
    }

    private boolean isTundra(Corner corner) {
        return tiles.entrySet().stream()
                .filter(entry -> entry.getValue().isTundra())
                .anyMatch(entry -> corner.isAdjacent(entry.getKey()));
    }

    public void endTurn(Random random) {
        skipAll();

        if (actionQueue.isEmpty()) {
            if (phase == Phase.PLANNING) {
                var originalAnimal = currentAnimal;
                do {
                    currentAnimal = initiativeTrack.get((initiativeTrack.indexOf(currentAnimal) + 1) % initiativeTrack.size());
                } while (!animals.get(currentAnimal).hasActionPawn() && currentAnimal != originalAnimal);

                if (currentAnimal == originalAnimal && animals.values().stream().noneMatch(Animal::hasActionPawn)) {
                    triggerExecutionPhase();
                } else {
                    actionQueue.add(PossibleAction.mandatory(currentAnimal, Action.PlaceActionPawn.class));
                }
            } else if (phase == Phase.EXECUTION) {
                nextActionPawn(random);
            }
        } else {
            currentAnimal = actionQueue.getNextAnimal()
                    .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));
        }
    }

    public void skip() {
        var skippedPossibleAction = actionQueue.skip();
        var skippedAction = skippedPossibleAction.getActions().get(0);

        actionDisplay.getLeftMostExecutableActionPawn()
                .map(ActionDisplay.ActionPawn::getActionType)
                .map(ActionType::getAction)
                .filter(skippedAction::equals)
                .ifPresent(action -> {
                    if (actionDisplay.removeLeftMostActionPawn()) {
                        getAnimal(currentAnimal).addActionPawn();
                    }
                });
    }

    private void skipAll() {
        while (actionQueue.hasActions(currentAnimal)) {
            skip();
        }
    }

    private void triggerPlanningPhase() {
        phase = Phase.PLANNING;
        currentAnimal = initiativeTrack.get(0);
    }

    private void triggerExecutionPhase() {
        phase = Phase.EXECUTION;

        // There will always be at least 1 follow up action (from at least 1 AP)
        actionDisplay.startAtInitiative(this)
                .addTo(actionQueue);
        currentAnimal = actionQueue.getNextAnimal()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));
    }

    private void nextActionPawn(Random random) {
        actionDisplay.nextActionPawn(this)
                .addTo(actionQueue);
        actionQueue.getNextAnimal()
                .ifPresentOrElse(next -> currentAnimal = next, () -> triggerResetPhase(random));
    }

    private void triggerResetPhase(Random random) {
        extinction();
        survival();

        scoredTiles.clear();
        lastPlacedTile = null;

        if (!isEnded()) {
            reseed(random);

            round++;

            triggerPlanningPhase();
        } else {
            finalScoring();
        }
    }

    void scoreTile(Hex hex) {
        var scores = tiles.get(hex).score();

        scores.forEach((animalType, score) -> {
            if (score > 0) {
                animals.get(animalType).addVPs(score);
            }
        });

        scoredTiles.add(hex);
    }

    private void finalScoring() {
        tiles.keySet().forEach(this::scoreTile);
    }

    private void reseed(Random random) {
        drawCards();

        actionDisplay.slideGlaciationActionPawnsLeft();

        drawBag.addAll(actionDisplay.removeAllElements(ActionType.REGRESSION));
        drawBag.addAll(actionDisplay.removeAllElements(ActionType.DEPLETION));
        drawBag.addAll(actionDisplay.removeAllElements(ActionType.WANDERLUST));

        actionDisplay.slideElementsDown();
        actionDisplay.drawElements(drawBag, random);

        actionDisplay.resetFreeActionPawns(animals.keySet());

        wanderlustTiles.flipTopFaceUp();
    }

    private Optional<AnimalType> getSurvivalCard() {
        var speciesOnTundraTiles = tiles.values().stream()
                .filter(Tile::isTundra)
                .map(Tile::getSpecies)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

        var maxSpeciesOnTundraTiles = speciesOnTundraTiles.values().stream().max(Integer::compareTo).orElse(0);

        var animalsWithMostSpeciesOnTundraTiles = speciesOnTundraTiles.entrySet().stream()
                .filter(entry -> entry.getValue().equals(maxSpeciesOnTundraTiles))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return animalsWithMostSpeciesOnTundraTiles.size() == 1
                ? Optional.of(animalsWithMostSpeciesOnTundraTiles.get(0))
                : Optional.empty();
    }

    private void survival() {
        getSurvivalCard().ifPresent(animalWithMostSpeciesOnTundraTiles -> {
            var numberOfTundraTilesOccupied = (int) tiles.values().stream()
                    .filter(Tile::isTundra)
                    .filter(tile -> tile.hasSpecies(animalWithMostSpeciesOnTundraTiles))
                    .count();

            getAnimal(animalWithMostSpeciesOnTundraTiles).addVPs(bonusVPs(numberOfTundraTilesOccupied));
        });
    }

    private void extinction() {
        tiles.forEach((hex, tile) ->
                animals.values().forEach(animal -> {
                    var species = tile.removeEndangeredSpecies(animal, getAdjacentElements(hex));
                    animal.addEliminatedSpecies(species);
                }));
    }

    public List<Class<? extends Action>> possibleActions() {
        return actionQueue.getNextPossibleAction()
                .filter(possibleAction -> possibleAction.canBePerformedBy(currentAnimal))
                .map(PossibleAction::getActions)
                .orElse(Collections.emptyList());
    }

    void moveForwardOnInitiative(AnimalType animalType) {
        var index = initiativeTrack.indexOf(animalType);
        if (index > 0) {
            initiativeTrack.set(index, initiativeTrack.get(index - 1));
            initiativeTrack.set(index - 1, animalType);
        }
    }

    void addElement(Corner corner, ElementType elementType) {
        if (elements.containsKey(corner)) {
            throw new DominantSpeciesException(DominantSpeciesError.ALREADY_ELEMENT_AT_CORNER);
        }

        elements.put(corner, elementType);
    }

    Player getCurrentPlayer() {
        return animals.get(currentAnimal).getPlayer();
    }

    Animal getAnimal(AnimalType animalType) {
        var animal = animals.get(animalType);

        if (animal == null) {
            throw new DominantSpeciesException(DominantSpeciesError.ANIMAL_NOT_FOUND);
        }

        return animal;
    }

    Optional<Tile> getTile(Hex hex) {
        return Optional.ofNullable(tiles.get(hex));
    }

    Optional<ElementType> getElement(Corner corner) {
        return Optional.ofNullable(elements.get(corner));
    }

    void removeElement(Corner corner) {
        if (elements.remove(corner) == null) {
            throw new DominantSpeciesException(DominantSpeciesError.NO_ELEMENT_AT_CORNER);
        }
    }

    List<ElementType> getAdjacentElements(Hex tile) {
        return elements.entrySet().stream()
                .filter(element -> element.getKey().isAdjacent(tile))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    Stream<Tile> getAdjacentTiles(Hex hex) {
        return tiles.entrySet().stream().filter(entry -> entry.getKey().isAdjacent(hex)).map(Map.Entry::getValue);
    }

    Stream<Hex> getAdjacentHexes(Hex hex) {
        return HEXES.stream().filter(hex::isAdjacent);
    }

    boolean hasTile(Hex hex) {
        return tiles.containsKey(hex);
    }

    Set<Hex> getTilesWithSpecies(AnimalType animalType) {
        return tiles.entrySet().stream()
                .filter(tile -> tile.getValue().hasSpecies(animalType))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    Optional<Tile> getLastScoredTile() {
        return scoredTiles.isEmpty() ? Optional.empty()
                : getTile(scoredTiles.get(scoredTiles.size() - 1));
    }

    Set<AnimalType> getOpposingSpecies(AnimalType animalType) {
        var player = animals.get(animalType).getPlayer();

        return animals.values().stream()
                .filter(animal -> animal.getPlayer() != player)
                .map(Animal::getType)
                .collect(Collectors.toSet());
    }

    void removeAvailableCard(Card card) {
        if (!availableCards.remove(card)) {
            throw new DominantSpeciesException(DominantSpeciesError.CARD_NOT_AVAILABLE);
        }
    }


    void removeAvailableTundraTile() {
        if (availableTundraTiles == 0) {
            throw new DominantSpeciesException(DominantSpeciesError.NO_TUNDRA_TILE_AVAILABLE);
        }
        availableTundraTiles--;
    }

    void addTile(Hex hex, Tile tile) {
        if (tiles.containsKey(hex)) {
            throw new DominantSpeciesException(DominantSpeciesError.ALREADY_TILE_AT_HEX);
        }

        if (getAdjacentTiles(hex).count() == 0) {
            throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TILE);
        }

        tiles.put(hex, tile);

        lastPlacedTile = hex;
    }

    Optional<Hex> getLastPlacedTile() {
        return Optional.ofNullable(lastPlacedTile);
    }

    boolean hasAnimal(AnimalType animalType) {
        return animals.containsKey(animalType);
    }

    public int getDeckSize() {
        return deck.size();
    }

    // State

    @Override
    public boolean isEnded() {
        return actionQueue.isEmpty() && !deck.contains(Card.ICE_AGE) && !availableCards.contains(Card.ICE_AGE);
    }

    @Override
    public void perform(Player player, com.boardgamefiesta.api.domain.Action action, Random random) {
        if (!getCurrentPlayers().contains(player)) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_CURRENT_PLAYER);
        }

        perform((Action) action, random);
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    @Override
    public void skip(Player player, Random random) {
        skip();
    }

    @Override
    public void endTurn(Player player, Random random) {
        if (getAnimal(currentAnimal).getPlayer() != player) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_CURRENT_PLAYER);
        }
        endTurn(random);
    }

    @Override
    public void forceEndTurn(Player player, @NonNull Random random) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Player> getPlayerOrder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Player> getPlayers() {
        return animals.values().stream().map(Animal::getPlayer).collect(Collectors.toList());
    }

    @Override
    public int score(Player player) {
        return animals.values().stream()
                .filter(animal -> animal.getPlayer().equals(player))
                .mapToInt(Animal::getScore)
                .min()
                .orElseThrow();
    }

    @Override
    public List<Player> ranking() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUndo() {
        return canUndo;
    }

    @Override
    public Set<Player> getCurrentPlayers() {
        return Collections.singleton(animals.get(currentAnimal).getPlayer());
    }

    @Override
    public void leave(Player player, Random random) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Stats stats(Player player) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Integer> getTurn(Player player) {
        // TODO
        return Optional.empty();
    }

    @Override
    public int getProgress() {
        return Math.round((float) (deck.size() + availableCards.size()) / (float) Card.INITIAL_DECK_SIZE);
    }

}
