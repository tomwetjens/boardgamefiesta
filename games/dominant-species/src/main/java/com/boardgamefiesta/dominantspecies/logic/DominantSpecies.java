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

import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.api.domain.Stats;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For deserialization
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
@Slf4j
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

    @Getter(AccessLevel.PACKAGE)
    private ActionQueue actionQueue;

    @Getter
    private List<Hex> scoredTiles;

    private Queue<Card> deck;

    @Getter
    private Set<Card> availableCards;

    @Getter
    private int availableTundraTiles;

    @Getter
    private WanderlustTiles wanderlustTiles;

    private Hex lastPlacedTile;

    private boolean canUndo;

    private final transient List<InGameEventListener> eventListeners = new LinkedList<>();

    public static DominantSpecies start(@NonNull Set<Player> players, @NonNull Random random) {
        return start(randomAnimalPerPlayer(players, random), random);
    }

    public static DominantSpecies start(@NonNull Map<AnimalType, Player> animalTypes, @NonNull Random random) {
        if (animalTypes.isEmpty()) {
            throw new DominantSpeciesException(DominantSpeciesError.MIN_1_PLAYER);
        }

        if (animalTypes.values().stream().distinct().count() > 6) {
            throw new DominantSpeciesException(DominantSpeciesError.MAX_6_PLAYERS);
        }

        var animals = initialAnimals(animalTypes);
        var initiativeTrack = reverseFoodChainOrder(animals.values());
        var drawBag = DrawBag.initial();

        var game = builder()
                .round(1)
                .phase(Phase.PLANNING)
                .animals(animals)
                .initiativeTrack(initiativeTrack)
                .tiles(createInitialTiles(animals.keySet()))
                .elements(createInitialElements())
                .drawBag(drawBag)
                .actionDisplay(ActionDisplay.initial(animals.keySet(), drawBag, random))
                .currentAnimal(initiativeTrack.get(0))
                .actionQueue(ActionQueue.initial(PossibleAction.mandatory(initiativeTrack.get(0), Action.PlaceActionPawn.class)))
                .deck(Card.initialDeck(random))
                .availableCards(new HashSet<>())
                .availableTundraTiles(11)
                .wanderlustTiles(WanderlustTiles.initial(random))
                .scoredTiles(new ArrayList<>())
                .build();

        game.drawCards();

        game.recalculateDominance();

        return game;
    }

    private static Map<AnimalType, Animal> initialAnimals(Map<AnimalType, Player> animalTypes) {
        return animalTypes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Animal.initial(entry.getValue(), entry.getKey(), animalTypes.size())));
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

    static Map<Hex, Tile> createInitialTiles(Set<AnimalType> playingAnimals) {
        var tiles = new HashMap<Hex, Tile>();

        var jungle = Tile.initial(TileType.JUNGLE, false);
        var wetland = Tile.initial(TileType.WETLAND, false);
        var savannah = Tile.initial(TileType.SAVANNAH, false);
        var sea = Tile.initial(TileType.SEA, true);
        var forest = Tile.initial(TileType.FOREST, false);
        var mountain = Tile.initial(TileType.MOUNTAIN, false);
        var desert = Tile.initial(TileType.DESERT, false);

        if (playingAnimals.contains(AnimalType.INSECTS)) {
            savannah.addSpecies(AnimalType.INSECTS, 2);
            wetland.addSpecies(AnimalType.INSECTS, 1);
            desert.addSpecies(AnimalType.INSECTS, 1);
        }
        if (playingAnimals.contains(AnimalType.ARACHNIDS)) {
            jungle.addSpecies(AnimalType.ARACHNIDS, 2);
            forest.addSpecies(AnimalType.ARACHNIDS, 1);
            wetland.addSpecies(AnimalType.ARACHNIDS, 1);
        }
        if (playingAnimals.contains(AnimalType.AMPHIBIANS)) {
            wetland.addSpecies(AnimalType.AMPHIBIANS, 2);
            jungle.addSpecies(AnimalType.AMPHIBIANS, 1);
            savannah.addSpecies(AnimalType.AMPHIBIANS, 1);
        }
        if (playingAnimals.contains(AnimalType.BIRDS)) {
            forest.addSpecies(AnimalType.BIRDS, 2);
            mountain.addSpecies(AnimalType.BIRDS, 1);
            jungle.addSpecies(AnimalType.BIRDS, 1);
        }
        if (playingAnimals.contains(AnimalType.REPTILES)) {
            desert.addSpecies(AnimalType.REPTILES, 2);
            savannah.addSpecies(AnimalType.REPTILES, 1);
            mountain.addSpecies(AnimalType.REPTILES, 1);
        }
        if (playingAnimals.contains(AnimalType.MAMMALS)) {
            mountain.addSpecies(AnimalType.MAMMALS, 2);
            desert.addSpecies(AnimalType.MAMMALS, 1);
            forest.addSpecies(AnimalType.MAMMALS, 1);
        }

        tiles.put(INITIAL_JUNGLE, jungle);
        tiles.put(INITIAL_WETLAND, wetland);
        tiles.put(INITIAL_SAVANNAH, savannah);
        tiles.put(INITIAL_SEA, sea);
        tiles.put(INITIAL_FOREST, forest);
        tiles.put(INITIAL_MOUNTAIN, mountain);
        tiles.put(INITIAL_DESERT, desert);

        return tiles;
    }

    private static List<AnimalType> reverseFoodChainOrder(Collection<Animal> animals) {
        return animals.stream()
                .map(Animal::getType)
                .sorted(Comparator.<AnimalType>comparingInt(AnimalType.FOOD_CHAIN_ORDER::indexOf).reversed())
                .collect(Collectors.toList());
    }

    private static Map<AnimalType, Player> randomAnimalPerPlayer(Set<Player> players, Random random) {
        var types = new LinkedList<>(Arrays.asList(AnimalType.values()));
        Collections.shuffle(types, random);

        return players.stream()
                .sorted(Comparator.comparing(Player::getName)) // Deterministic order for testing
                .collect(Collectors.toMap(p -> types.poll(), Function.identity()));
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

    public void perform(@NonNull Action action, @NonNull Random random) {
        if (!actionQueue.canPerform(currentAnimal, action.getClass())) {
            throw new DominantSpeciesException(DominantSpeciesError.CANNOT_PERFORM_ACTION);
        }

        log.debug("{} perform action: {}", currentAnimal, action);

        var result = action.perform(this, random);

        actionQueue.perform(currentAnimal, action);

        result.getFollowUpActions().addTo(actionQueue);
        canUndo = result.canUndo();

        recalculateDominance();

        if (actionQueue.isEmpty()) {
            if (phase == Phase.PLANNING) {
                getNextAnimalToPlaceActionPawn()
                        // If next animal is also the current animal, then just move on
                        .filter(nextAnimal -> nextAnimal == currentAnimal)
                        .ifPresent(animal -> nextAnimalToPlaceActionPawn());
            } else if (phase == Phase.EXECUTION) {
                actionDisplay.getNextActionPawn()
                        // If next action pawn is also for the current animal, then just move on
                        .filter(actionPawn -> actionPawn.getAnimalType() == currentAnimal)
                        .ifPresent(actionPawn -> nextActionPawn(random));
            }
        }
    }

    private void recalculateDominance() {
        tiles.forEach((hex, tile) -> {
            var adjacentElements = getAdjacentElementTypes(hex);
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

    private boolean isTundra(@NonNull Corner corner) {
        return tiles.entrySet().stream()
                .filter(entry -> entry.getValue().isTundra())
                .anyMatch(entry -> corner.isAdjacent(entry.getKey()));
    }

    public void endTurn(@NonNull Random random) {
        var animalEndingTurn = currentAnimal;
        var phaseWhenEndingTurn = phase;

        log.debug("{} end turn", currentAnimal);

        do {
            skipAll(random);

            if (actionQueue.isEmpty()) {
                if (phase == Phase.PLANNING) {
                    nextAnimalToPlaceActionPawn();
                } else if (phase == Phase.EXECUTION) {
                    nextActionPawn(random);
                } else if (phase == Phase.RESET) {
                    completeResetPhase(random);
                }
            } else {
                currentAnimal = actionQueue.getNextAnimal()
                        .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));
            }
        } while (currentAnimal == animalEndingTurn && phase == phaseWhenEndingTurn && canSkip());

        canUndo = false;

        if (isEnded()) {
            log.debug("Ended");
        }
    }

    private Optional<AnimalType> getNextAnimalToPlaceActionPawn() {
        var animalType = currentAnimal;
        do {
            animalType = initiativeTrack.get((initiativeTrack.indexOf(animalType) + 1) % initiativeTrack.size());
        } while (!animals.get(animalType).hasActionPawn() && animalType != currentAnimal);

        if (animalType == currentAnimal && animals.values().stream().noneMatch(Animal::hasActionPawn)) {
            return Optional.empty();
        } else {
            return Optional.of(animalType);
        }
    }

    private void nextAnimalToPlaceActionPawn() {
        getNextAnimalToPlaceActionPawn()
                .ifPresentOrElse(nextAnimal -> {
                    currentAnimal = nextAnimal;
                    actionQueue.add(PossibleAction.mandatory(currentAnimal, Action.PlaceActionPawn.class));
                }, this::triggerExecutionPhase);
    }

    public boolean canSkip() {
        return actionQueue.canSkip();
    }

    public void skip(@NonNull Random random) {
        var skippedPossibleAction = actionQueue.skip();
        var skippedAction = skippedPossibleAction.getActions().get(0);

        log.debug("{} skip action: {}", currentAnimal, skippedAction.getSimpleName());

        if (skippedAction.equals(Action.SaveFromExtinction.class)) {
            extinction(null);
        } else {
            actionDisplay.getCurrentActionPawn()
                    .map(ActionDisplay.ActionPawn::getActionType)
                    .map(ActionType::getAction)
                    .filter(skippedAction::equals)
                    .ifPresent(action -> {
                        if (!actionDisplay.removeCurrentActionPawn().isFree()) {
                            getAnimal(currentAnimal).addActionPawn();
                        }
                    });

            if (actionQueue.isEmpty() && phase == Phase.EXECUTION) {
                actionDisplay.getNextActionPawn()
                        .filter(ap -> ap.getAnimalType() == currentAnimal)
                        .ifPresent(ap -> nextActionPawn(random));
            }
        }

        recalculateDominance();
    }

    private void skipAll(Random random) {
        while (actionQueue.hasActions(currentAnimal)) {
            skip(random);
        }
    }

    private void triggerPlanningPhase() {
        fireEvent(Event.Type.PLANNING_PHASE, List.of(round));
        phase = Phase.PLANNING;
        currentAnimal = initiativeTrack.get(0);
        actionQueue.add(PossibleAction.mandatory(currentAnimal, Action.PlaceActionPawn.class));
    }

    private void triggerExecutionPhase() {
        fireEvent(Event.Type.EXECUTION_PHASE, List.of(round));
        phase = Phase.EXECUTION;

        // There will always be at least 1 follow up action (from at least 1 AP)
        actionDisplay.startAtInitiative(this)
                .addTo(actionQueue);
        currentAnimal = actionQueue.getNextAnimal()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.NO_ACTION_PAWN));
    }

    private void nextActionPawn(@NonNull Random random) {
        actionDisplay.nextActionPawnOrActionType(this)
                .addTo(actionQueue);
        actionQueue.getNextAnimal()
                .ifPresentOrElse(next -> currentAnimal = next, () -> triggerResetPhase(random));
    }

    private void triggerResetPhase(@NonNull Random random) {
        fireEvent(Event.Type.RESET_PHASE, List.of(round));

        phase = Phase.RESET;

        triggerExtinction(random);
    }

    private void triggerExtinction(Random random) {
        var speciesThatCanBeSavedFromExtinction = getTilesWithEndangeredSpecies(AnimalType.MAMMALS)
                .limit(2) // We need just 0, 1 or determine that there's more than 1 to offer a choice
                .collect(Collectors.toList());

        if (speciesThatCanBeSavedFromExtinction.size() > 1) {
            // Must offer choice to player
            actionQueue.add(PossibleAction.optional(AnimalType.MAMMALS, Action.SaveFromExtinction.class));
            currentAnimal = AnimalType.MAMMALS;
        } else {
            // Automatically select the one tile (or none)
            var saveMammalSpeciesOn = speciesThatCanBeSavedFromExtinction.stream()
                    .findFirst()
                    .orElse(null);

            extinction(saveMammalSpeciesOn);

            completeResetPhase(random);
        }
    }

    void scoreTile(@NonNull Hex hex) {
        log.debug("Scoring tile {}", hex);

        var tile = tiles.get(hex);
        var scores = tile.score();

        scores.forEach((animalType, score) -> {
            log.debug("{} get {} VPs from tile {}", animalType, score, hex);

            if (score > 0) {
                animals.get(animalType).addVPs(score);

                fireEvent(animalType, Event.Type.GAIN_VPS_FROM_TILE, List.of(score, hex, tile.getType()));
            }
        });

        scoredTiles.add(hex);
    }

    private void finalScoring() {
        log.debug("Final scoring");

        fireEvent(Event.Type.FINAL_SCORING, List.of(round));
        tiles.keySet().forEach(this::scoreTile);
    }

    private void reseed(@NonNull Random random) {
        drawCards();

        actionDisplay.slideGlaciationActionPawnsLeft();

        drawBag.addAll(actionDisplay.removeAllElements(ActionType.REGRESSION));
        drawBag.addAll(actionDisplay.removeAllElements(ActionType.DEPLETION));
        drawBag.addAll(actionDisplay.removeAllElements(ActionType.WANDERLUST));

        actionDisplay.slideElementsDown();
        actionDisplay.drawElements(drawBag, random);

        actionDisplay.resetFreeActionPawns(initiativeTrack);

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

    private void completeResetPhase(Random random) {
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

    private void survival() {
        getSurvivalCard().ifPresent(animalWithMostSpeciesOnTundraTiles -> {
            var numberOfTundraTilesOccupied = (int) tiles.values().stream()
                    .filter(Tile::isTundra)
                    .filter(tile -> tile.hasSpecies(animalWithMostSpeciesOnTundraTiles))
                    .count();

            var bonusVPs = bonusVPs(numberOfTundraTilesOccupied);
            getAnimal(animalWithMostSpeciesOnTundraTiles).addVPs(bonusVPs);

            fireEvent(animalWithMostSpeciesOnTundraTiles, Event.Type.GAIN_BONUS_VPS, List.of(bonusVPs));
        });
    }

    void extinction(Hex saveMammalSpeciesOn) {
        tiles.forEach((hex, tile) ->
                animals.values().forEach(animal -> {
                    var speciesToSave = animal.getType() == AnimalType.MAMMALS
                            && hex.equals(saveMammalSpeciesOn) ? 1 : 0;

                    var eliminatedSpecies = tile.extinction(animal, getAdjacentElementTypes(hex), speciesToSave);

                    if (eliminatedSpecies > 0) {
                        animal.addEliminatedSpecies(eliminatedSpecies);

                        fireEvent(animal.getType(), Event.Type.EXTINCTION, List.of(eliminatedSpecies, hex, tile.getType()));
                    }
                }));
    }

    private Stream<Hex> getTilesWithEndangeredSpecies(AnimalType animalType) {
        var animal = animals.get(animalType);

        return tiles.entrySet().stream()
                .filter(tile -> tile.getValue().hasSpecies(animalType))
                .filter(tile -> tile.getValue().getEndangeredSpecies(animal, getAdjacentElementTypes(tile.getKey())) > 0)
                .map(Map.Entry::getKey);
    }

    public List<Class<? extends Action>> possibleActions() {
        return actionQueue.getNextPossibleAction()
                .filter(possibleAction -> possibleAction.canBePerformedBy(currentAnimal))
                .map(PossibleAction::getActions)
                .orElse(Collections.emptyList());
    }

    int moveForwardOnInitiative(@NonNull AnimalType animalType) {
        var index = initiativeTrack.indexOf(animalType);
        if (index > 0) {
            initiativeTrack.set(index, initiativeTrack.get(index - 1));
            initiativeTrack.set(index - 1, animalType);
            return index - 1;
        }
        return index;
    }

    void addElement(@NonNull Corner corner, @NonNull ElementType elementType) {
        if (hasElement(corner)) {
            throw new DominantSpeciesException(DominantSpeciesError.ALREADY_ELEMENT_AT_CORNER);
        }

        if (!tiles.containsKey(corner.getA())
                && !tiles.containsKey(corner.getB())
                && !tiles.containsKey(corner.getC())) {
            throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TILE);
        }

        elements.put(corner, elementType);
    }

    Player getCurrentPlayer() {
        return animals.get(currentAnimal).getPlayer();
    }

    Animal getAnimal(@NonNull AnimalType animalType) {
        var animal = animals.get(animalType);

        if (animal == null) {
            throw new DominantSpeciesException(DominantSpeciesError.ANIMAL_NOT_FOUND);
        }

        return animal;
    }

    Optional<Tile> getTile(@NonNull Hex hex) {
        return Optional.ofNullable(tiles.get(hex));
    }

    Optional<ElementType> getElement(@NonNull Corner corner) {
        return Optional.ofNullable(elements.get(corner));
    }

    ElementType removeElement(@NonNull Corner corner) {
        var elementType = elements.remove(corner);

        if (elementType == null) {
            throw new DominantSpeciesException(DominantSpeciesError.NO_ELEMENT_AT_CORNER);
        }

        return elementType;
    }

    List<Corner> getAdjacentElements(@NonNull Hex tile) {
        return elements.keySet().stream()
                .filter(corner -> corner.isAdjacent(tile))
                .collect(Collectors.toList());
    }

    List<ElementType> getAdjacentElementTypes(@NonNull Hex tile) {
        return elements.entrySet().stream()
                .filter(element -> element.getKey().isAdjacent(tile))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    Stream<Tile> getAdjacentTiles(@NonNull Hex hex) {
        return tiles.entrySet().stream().filter(entry -> entry.getKey().isAdjacent(hex)).map(Map.Entry::getValue);
    }

    Stream<Tile> getAdjacentTiles(@NonNull Corner corner) {
        return tiles.entrySet().stream().filter(entry -> corner.isAdjacent(entry.getKey())).map(Map.Entry::getValue);
    }

    Stream<Hex> getAdjacentHexes(@NonNull Hex hex) {
        return HEXES.stream().filter(hex::isAdjacent);
    }

    boolean hasTile(Hex hex) {
        return tiles.containsKey(hex);
    }

    Set<Hex> getTilesWithSpecies(@NonNull AnimalType animalType) {
        return tiles.entrySet().stream()
                .filter(tile -> tile.getValue().hasSpecies(animalType))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    Optional<Tile> getLastScoredTile() {
        return scoredTiles.isEmpty() ? Optional.empty()
                : getTile(scoredTiles.get(scoredTiles.size() - 1));
    }

    void removeAvailableCard(@NonNull Card card) {
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

    void addTile(@NonNull Hex hex, @NonNull Tile tile) {
        if (tiles.containsKey(hex)) {
            throw new DominantSpeciesException(DominantSpeciesError.ALREADY_TILE_AT_HEX);
        }

        if (getAdjacentTiles(hex).count() == 0) {
            throw new DominantSpeciesException(DominantSpeciesError.MUST_BE_ADJACENT_TO_TILE);
        }

        if (!HEXES.contains(hex)) {
            throw new DominantSpeciesException(DominantSpeciesError.INVALID_HEX);
        }

        tiles.put(hex, tile);

        lastPlacedTile = hex;
    }

    public Optional<Hex> getLastPlacedTile() {
        return Optional.ofNullable(lastPlacedTile);
    }

    boolean isAnimalPlaying(@NonNull AnimalType animalType) {
        return initiativeTrack.contains(animalType);
    }

    public int getDeckSize() {
        return deck.size();
    }

    // State

    @Override
    public boolean isEnded() {
        return !deck.contains(Card.ICE_AGE) && !availableCards.contains(Card.ICE_AGE)
                && !actionDisplay.hasActionPawn(ActionType.DOMINATION)
                && actionQueue.isEmpty()
                && phase == Phase.RESET;
    }

    @Override
    public void perform(@NonNull Player player, @NonNull com.boardgamefiesta.api.domain.Action action, Random random) {
        if (!getCurrentPlayers().contains(player)) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_CURRENT_PLAYER);
        }

        perform((Action) action, random);
    }

    @Override
    public void addEventListener(@NonNull InGameEventListener eventListener) {
        eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(@NonNull InGameEventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    @Override
    public void skip(@NonNull Player player, @NonNull Random random) {
        skip(random);
    }

    @Override
    public void endTurn(@NonNull Player player, @NonNull Random random) {
        if (!getAnimal(currentAnimal).getPlayer().equals(player)) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_CURRENT_PLAYER);
        }
        endTurn(random);
    }

    @Override
    public void forceEndTurn(@NonNull Player player, @NonNull Random random) {
        if (!getAnimal(currentAnimal).getPlayer().equals(player)) {
            throw new DominantSpeciesException(DominantSpeciesError.NOT_CURRENT_PLAYER);
        }

        while (actionQueue.hasActions(currentAnimal)) {
            if (canSkip()) {
                skip(random);
            } else {
                new Automa().perform(this, player, random);
            }
        }

        endTurn(player, random);
    }

    public List<Player> getPlayers() {
        return initiativeTrack.stream()
                .map(animals::get)
                .map(Animal::getPlayer)
                .collect(Collectors.toList());
    }

    @Override
    public int getScore(@NonNull Player player) {
        return animals.values().stream()
                .filter(animal -> animal.getPlayer().equals(player))
                .mapToInt(Animal::getScore)
                .min()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.INVALID_PLAYER));
    }

    @Override
    public List<Player> getRanking() {
        return animals.values().stream()
                .sorted(Comparator.comparingInt(Animal::getScore).reversed()
                        .thenComparingInt(animal -> AnimalType.FOOD_CHAIN_ORDER.indexOf(animal.getType())))
                .map(Animal::getPlayer)
                .collect(Collectors.toList());
    }

    @Override
    public boolean canUndo() {
        return canUndo;
    }

    @Override
    public Set<Player> getCurrentPlayers() {
        return isEnded() ? Collections.emptySet() : Collections.singleton(animals.get(currentAnimal).getPlayer());
    }

    @Override
    public void leave(@NonNull Player player, @NonNull Random random) {
        var animal = animals.values().stream().filter(a -> a.getPlayer().equals(player)).findAny()
                .orElseThrow(() -> new DominantSpeciesException(DominantSpeciesError.INVALID_PLAYER));

        animal.leave();

        initiativeTrack.remove(animal.getType());
        actionQueue.removeAll(animal.getType());
        actionDisplay.removeAllActionPawns(animal.getType());

        tiles.values().forEach(tile -> tile.removeAllSpecies(animal.getType()));
        recalculateDominance();

        if (currentAnimal == animal.getType()) {
            endTurn(random);
        }
    }

    @Override
    public Stats getStats(@NonNull Player player) {
        // TODO
        return Stats.builder().build();
    }

    @Override
    public int getProgress() {
        return Math.round((float) (deck.size() + availableCards.size()) / (float) Card.INITIAL_DECK_SIZE);
    }

    Stream<Corner> getVacantCorners() {
        return tiles.keySet().stream() // start from the existing tiles so we at least have an adjacent tile (reduces search space)
                .flatMap(a -> HEXES.stream()
                        .filter(b -> !b.equals(a) && b.isAdjacent(a)) // if all 3 hexes that make a corner must be adjacent, then at least 2 must be adjacent as well
                        .flatMap(b -> HEXES.stream()
                                .filter(c -> !c.equals(b) && c.isAdjacent(b) && c.isAdjacent(a))// 3rd hex must be adjacent to first two
                                .map(c -> new Corner(a, b, c))
                                .filter(this::isVacant) // corner must not have an element yet
                        ));
    }

    boolean isVacant(Corner corner) {
        return !hasElement(corner);
    }

    boolean hasVacantCorner() {
        return getVacantCorners().findAny().isPresent();
    }

    boolean hasElement(Corner corner) {
        return elements.containsKey(corner);
    }

    Stream<Hex> getVacantHexes() {
        return HEXES.stream().filter(this::isVacant);
    }

    boolean isVacant(Hex hex) {
        return !hasTile(hex) && hasAdjacentTile(hex);
    }

    boolean hasAdjacentTile(Hex hex) {
        return tiles.keySet().stream().anyMatch(hex::isAdjacent);
    }

    boolean canMoveThroughAdjacentTiles(Hex from, Hex to) {
        if (from.isAdjacent(to)) {
            return true;
        }
        return getAdjacentHexes(from)
                .filter(this::hasTile)
                .anyMatch(adjacent -> canMoveThroughAdjacentTiles(adjacent, to));
    }

    void fireEvent(Event.Type type, Collection<?>... values) {
        fireEvent(currentAnimal, type, values);
    }

    void fireEvent(AnimalType animalType, Event.Type type, Collection<?>... values) {
        var parameters = Stream.concat(Stream.of(animalType.name()),
                        Arrays.stream(values)
                                .flatMap(Collection::stream)
                                .map(o -> o != null ? o.toString() : ""))
                .collect(Collectors.toList());

        var event = new Event(getAnimal(animalType).getPlayer(), animalType, type, parameters);

        fireEvent(event);
    }

    private void fireEvent(Event event) {
        eventListeners.forEach(eventListener -> eventListener.event(event));
    }

}
