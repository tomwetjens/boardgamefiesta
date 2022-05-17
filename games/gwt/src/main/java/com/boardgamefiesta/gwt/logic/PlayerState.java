/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerState {

    private static final List<Integer> CERTIFICATE_STEPS = Arrays.asList(0, 1, 2, 3, 4, 6);

    static final int MAX_HAND_LIMIT = 6;
    static final int MAX_STEP_LIMIT = 5;

    @Getter
    private final Player player;
    private final Queue<Card> drawStack;
    private final Set<Card> hand;
    private final List<Card> discardPile;
    private final Map<Worker, Integer> workers;
    private final Set<PlayerBuilding> buildings;
    @Singular("unlocked")
    private final Map<Unlockable, Integer> unlocked;
    private final Set<ObjectiveCard> objectives;
    private final Set<StationMaster> stationMasters;
    private final List<Teepee> teepees;
    private final Set<Hazard> hazards;

    @Getter
    private Bid bid;
    @Getter
    private int tempCertificates;
    @Getter
    private int balance;
    @Getter
    private boolean jobMarketToken;

    @Getter
    private int numberOfCowboysUsedInTurn;
    @Getter
    private final List<Location> locationsActivatedInTurn;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private int lastEngineMove;
    @Getter
    private Optional<Station> lastUpgradedStation;
    @Getter
    private Optional<RailroadTrack.Town> lastPlacedBranchlet;

    @Getter
    private int exchangeTokens;

    @Getter
    private int branchlets;

    private Garth automaState;

    @Getter
    @Setter
    private int turns;

    @Setter
    @Getter
    private Map<Location, Integer> stops = new HashMap<>();

    PlayerState(@NonNull Player player, @NonNull GWT.Options options, int balance, @NonNull Random random, PlayerBuilding.BuildingSet buildingSet) {
        this.player = player;
        this.balance = player.getType() == Player.Type.COMPUTER ? 999 : balance;
        this.tempCertificates = 0;

        this.drawStack = createDrawStack(random);
        this.discardPile = new LinkedList<>();

        this.hand = new HashSet<>();

        this.workers = new EnumMap<>(Worker.class);
        this.workers.put(Worker.COWBOY, 1);
        this.workers.put(Worker.CRAFTSMAN, 1);
        this.workers.put(Worker.ENGINEER, 1);

        this.buildings = new HashSet<>(player.getType() == Player.Type.COMPUTER
                ? buildingSet.createSide(PlayerBuilding.Side.B, player)
                : buildingSet.createBuildings(player));
        this.unlocked = new EnumMap<>(Unlockable.class);
        this.unlocked.put(Unlockable.AUX_GAIN_DOLLAR, 1);
        this.unlocked.put(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD, 1);
        this.unlocked.put(Unlockable.AUX_DISCARD_CATTLE_CARD_TO_PLACE_BRANCHLET, 1);

        this.objectives = new HashSet<>();

        this.stationMasters = new HashSet<>();
        this.teepees = new LinkedList<>();
        this.hazards = new HashSet<>();

        this.locationsActivatedInTurn = new LinkedList<>();

        this.lastUpgradedStation = Optional.empty();

        this.lastPlacedBranchlet = Optional.empty();

        this.exchangeTokens = 1;

        this.branchlets = 15;

        if (player.getType() == Player.Type.COMPUTER) {
            this.automaState = Garth.create(this, random, options.getDifficulty() != null ? options.getDifficulty() : Garth.Difficulty.EASY);
        }

        drawUpToHandLimit(random);
    }

    JsonObject serialize(JsonBuilderFactory factory, RailroadTrack railroadTrack) {
        var serializer = JsonSerializer.forFactory(factory);

        return factory.createObjectBuilder()
                .add("drawStack", serializer.fromCollection(drawStack, Card::serialize))
                .add("hand", serializer.fromCollection(hand, Card::serialize))
                .add("discardPile", serializer.fromCollection(discardPile, Card::serialize))
                .add("workers", serializer.fromIntegerMap(workers, Worker::name))
                .add("buildings", serializer.fromStrings(buildings, Building::getName))
                .add("unlocked", serializer.fromIntegerMap(unlocked, Unlockable::name))
                .add("objectives", serializer.fromCollection(objectives, ObjectiveCard::serialize))
                .add("stationMasters", serializer.fromStrings(stationMasters, StationMaster::name))
                .add("teepees", serializer.fromStrings(teepees, Teepee::name))
                .add("hazards", serializer.fromCollection(hazards, Hazard::serialize))
                .add("tempCertificates", tempCertificates)
                .add("bid", bid != null ? bid.serialize(factory) : JsonValue.NULL)
                .add("balance", balance)
                .add("jobMarketToken", jobMarketToken)
                .add("usedCowboys", numberOfCowboysUsedInTurn)
                .add("locationsActivatedInTurn", serializer.fromStrings(locationsActivatedInTurn.stream().map(Location::getName)))
                .add("lastEngineMove", lastEngineMove)
                .add("lastUpgradedStation", lastUpgradedStation.map(railroadTrack.getStations()::indexOf).orElse(-1))
                .add("lastPlacedBranchlet", lastPlacedBranchlet.map(RailroadTrack.Town::getName).orElse(null))
                .add("exchangeTokens", exchangeTokens)
                .add("branchlets", branchlets)
                .add("automaState", automaState != null ? automaState.serialize(factory) : null)
                .add("turns", turns)
                .add("stops", serializer.fromIntegerMap(stops, Location::getName))
                .build();
    }

    static PlayerState deserialize(GWT.Edition edition, Player player, RailroadTrack railroadTrack, Trail trail, JsonObject jsonObject) {
        return new PlayerState(player,
                jsonObject.getJsonArray("drawStack").stream()
                        .map(Card::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("hand").stream()
                        .map(Card::deserialize)
                        .collect(Collectors.toSet()),
                jsonObject.getJsonArray("discardPile").stream()
                        .map(Card::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                JsonDeserializer.forObject(jsonObject.getJsonObject("workers")).<Worker>asIntegerMap(Worker::valueOf),
                jsonObject.getJsonArray("buildings")
                        .getValuesAs(JsonString::getString).stream()
                        .map(name -> PlayerBuilding.forName(edition, name, player))
                        .collect(Collectors.toSet()),
                JsonDeserializer.forObject(jsonObject.getJsonObject("unlocked")).<Unlockable>asIntegerMap(Unlockable::valueOf),
                jsonObject.getJsonArray("objectives").stream().map(ObjectiveCard::deserialize).collect(Collectors.toSet()),
                jsonObject.getJsonArray("stationMasters").getValuesAs(JsonString::getString).stream().map(StationMaster::valueOf).collect(Collectors.toSet()),
                jsonObject.getJsonArray("teepees").getValuesAs(JsonString::getString).stream().map(Teepee::valueOf).collect(Collectors.toList()),
                jsonObject.getJsonArray("hazards").stream().map(JsonValue::asJsonObject).map(Hazard::deserialize).collect(Collectors.toSet()),
                Bid.deserialize(jsonObject.get("bid")),
                jsonObject.getInt("tempCertificates", 0),
                jsonObject.getInt("balance", 0),
                jsonObject.getBoolean("jobMarketToken", false),
                jsonObject.getInt("usedCowboys", 0),
                jsonObject.containsKey("locationsActivatedInTurn") ?
                        jsonObject.getJsonArray("locationsActivatedInTurn").stream()
                                .map(jsonValue -> (JsonString) jsonValue)
                                .map(JsonString::getString)
                                .map(trail::getLocation)
                                .collect(Collectors.toCollection(LinkedList::new)) : new LinkedList<>(),
                jsonObject.getInt("lastEngineMove", 0),
                JsonDeserializer.getInt("lastUpgradedStation", jsonObject).filter(index -> index >= 0).map(railroadTrack.getStations()::get),
                Optional.ofNullable(jsonObject.getString("lastPlacedBranchlet")).map(railroadTrack::getTown),
                jsonObject.getInt("exchangeTokens", 0),
                jsonObject.getInt("branchlets", 0),
                jsonObject.get("automaState") != null && jsonObject.get("automaState") != JsonValue.NULL
                        ? Garth.deserialize(player, jsonObject.getJsonObject("automaState")) : null,
                jsonObject.getInt("turns", 0),
                jsonObject.containsKey("stops") ? JsonDeserializer.forObject(jsonObject.getJsonObject("stops")).asIntegerMap(trail::getLocation) : new HashMap<>());
    }

    void placeBid(Bid bid) {
        this.bid = bid;
    }

    Optional<Card> drawCard(Random random) {
        if (drawStack.isEmpty()) {
            Collections.shuffle(discardPile, random);
            drawStack.addAll(discardPile);
            discardPile.clear();
        }

        if (!drawStack.isEmpty()) {
            var card = drawStack.poll();
            hand.add(card);
            return Optional.of(card);
        }

        return Optional.empty();
    }

    int drawCards(int count, Random random) {
        return (int) IntStream.range(0, count)
                .mapToObj(i -> drawCard(random))
                .flatMap(Optional::stream)
                .count();
    }

    void drawUpToHandLimit(Random random) {
        int handLimit = getHandLimit();
        while (hand.size() < handLimit && drawStack.size() + discardPile.size() > 0) {
            drawCard(random);
        }
    }

    void gainCard(Card card) {
        discardPile.add(0, card);
    }

    void gainCards(Set<? extends Card> cards) {
        discardPile.addAll(0, cards);
    }

    Set<Card.CattleCard> discardCattleCards(CattleType type, int amount) {
        Set<Card.CattleCard> cattleCards = hand.stream().filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                // Assume lowest value cards for now
                .sorted(Comparator.comparingInt(Card.CattleCard::getValue))
                .limit(amount)
                .collect(Collectors.toSet());

        if (cattleCards.size() != amount) {
            throw new GWTException(GWTError.CATTLE_CARDS_NOT_IN_HAND);
        }

        hand.removeAll(cattleCards);

        discardPile.addAll(0, cattleCards);

        return Collections.unmodifiableSet(cattleCards);
    }

    void discardHand(GWT game) {
        for (var card : hand) {
            Action.DiscardCard.fireEvent(game, card);
        }

        discardPile.addAll(0, hand);
        hand.clear();
    }

    void gainDollars(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be negative: " + amount);
        }

        balance += amount;
    }

    void payDollars(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be negative: " + amount);
        }

        if (balance < amount) {
            throw new GWTException(GWTError.NOT_ENOUGH_BALANCE_TO_PAY);
        }

        balance -= amount;
    }

    ImmediateActions gainWorker(Worker worker, GWT game) {
        addWorker(worker);

        var count = workers.get(worker);

        if (worker == Worker.COWBOY) {
            if (count == 4) {
                game.fireEvent(player, GWTEvent.Type.MAY_REMOVE_HAZARD_FOR_FREE, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.RemoveHazardForFree.class));
            } else if (count == 6) {
                game.fireEvent(player, GWTEvent.Type.MAY_TRADE_WITH_TRIBES, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.TradeWithTribes.class));
            }
        } else if (worker == Worker.CRAFTSMAN) {
            if (count == 4 || count == 6) {
                game.fireEvent(player, GWTEvent.Type.MAY_PLACE_CHEAP_BUILDING, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.PlaceCheapBuilding.class));
            }
        } else {
            if (count == 2) {
                game.fireEvent(player, GWTEvent.Type.MAY_DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain1Certificate.class));
            } else if (count == 3) {
                game.fireEvent(player, GWTEvent.Type.MAY_DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain2Dollars.class));
            } else if (count == 4) {
                game.fireEvent(player, GWTEvent.Type.MAY_HIRE_CHEAP_WORKER, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.HireWorkerMinus2.class));
            } else if (count == 5) {
                game.fireEvent(player, GWTEvent.Type.MAY_DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain2Certificates.class));
            } else if (count == 6) {
                game.fireEvent(player, GWTEvent.Type.MAY_DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain4Dollars.class));
            }
        }
        return ImmediateActions.none();
    }

    void addWorker(Worker worker) {
        if (hasMaxWorkers(worker)) {
            throw new GWTException(GWTError.WORKERS_EXCEED_LIMIT);
        }

        workers.compute(worker, (k, v) -> v + 1);
    }

    void removeBuilding(PlayerBuilding building) {
        if (!buildings.remove(building)) {
            throw new GWTException(GWTError.BUILDING_NOT_AVAILABLE);
        }
    }

    void discardCard(Card card) {
        if (!hand.remove(card)) {
            throw new GWTException(GWTError.CARD_NOT_IN_HAND);
        }

        discardPile.add(0, card);
    }

    ImmediateActions playObjectiveCard(ObjectiveCard objectiveCard) {
        if (!hand.remove(objectiveCard)) {
            throw new GWTException(GWTError.CARD_NOT_IN_HAND);
        }

        commitToObjectiveCard(objectiveCard);

        return objectiveCard.getPossibleAction()
                .map(PossibleAction::clone)
                .map(ImmediateActions::of)
                .orElse(ImmediateActions.none());
    }

    void commitToObjectiveCard(ObjectiveCard objectiveCard) {
        objectives.add(objectiveCard);
    }

    void removeWorker(Worker worker) {
        if (workers.get(worker) <= 1) {
            throw new GWTException(GWTError.NOT_ENOUGH_WORKERS);
        }

        workers.computeIfPresent(worker, (k, v) -> v - 1);
    }

    void addStationMaster(StationMaster stationMaster) {
        if (!stationMasters.add(stationMaster)) {
            throw new GWTException(GWTError.ALREADY_HAS_STATION_MASTER);
        }
    }

    void addTeepee(Teepee teepee) {
        teepees.add(teepee);
    }

    void removeCards(Set<Card> cards) {
        if (!hand.containsAll(cards)) {
            throw new GWTException(GWTError.CARD_NOT_IN_HAND);
        }
        hand.removeAll(cards);
    }

    void removeCard(Card card) {
        if (!hand.remove(card)) {
            throw new GWTException(GWTError.CARD_NOT_IN_HAND);
        }
    }

    void addHazard(Hazard hazard) {
        if (!hazards.add(hazard)) {
            throw new GWTException(GWTError.ALREADY_HAS_HAZARD);
        }
    }

    void gainTempCertificates(int steps) {
        int newIndex = Math.min(CERTIFICATE_STEPS.size() - 1, CERTIFICATE_STEPS.indexOf(tempCertificates) + steps);
        tempCertificates = Math.min(getTempCertificateLimit(), CERTIFICATE_STEPS.get(newIndex));
    }

    void gainMaxTempCertificates() {
        tempCertificates = getTempCertificateLimit();
    }

    public int getTempCertificateLimit() {
        if (hasUnlocked(Unlockable.CERT_LIMIT_6) && hasUnlocked(Unlockable.CERT_LIMIT_4)) {
            return 6;
        } else if (hasUnlocked(Unlockable.CERT_LIMIT_4)) {
            return 4;
        }
        return 3;
    }

    void spendTempCertificates(int amount) {
        if (amount > tempCertificates) {
            throw new GWTException(GWTError.NOT_ENOUGH_CERTIFICATES);
        }
        int remaining = amount;
        while (remaining > 0) {
            // Move up one step
            int index = Math.max(0, CERTIFICATE_STEPS.indexOf(tempCertificates) - 1);
            int spent = tempCertificates - CERTIFICATE_STEPS.get(index);
            tempCertificates -= spent;
            remaining -= spent;
        }
    }

    public int permanentCertificates() {
        return (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_CERTS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_HAZARDS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_TEEPEE_PAIRS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.TWO_PERM_CERTS) ? 2 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_PER_2_STATIONS) ? 1 : 0);
    }

    void unlock(Unlockable unlockable) {
        if (unlocked.getOrDefault(unlockable, 0) == unlockable.getCount()) {
            throw new GWTException(GWTError.ALREADY_UNLOCKED);
        }

        if (unlockable.getCost() > 0) {
            payDollars(unlockable.getCost());
        }

        unlocked.compute(unlockable, (k, v) -> v != null ? v + 1 : 1);

        if (unlockable == Unlockable.EXTRA_STEP_DOLLARS) {
            gainDollars(3);
        }
    }

    void gainJobMarketToken() {
        jobMarketToken = true;
    }

    void addCardToHand(Card card) {
        hand.add(card);
    }

    public Set<Card> getHand() {
        return Collections.unmodifiableSet(hand);
    }

    public List<Teepee> getTeepees() {
        return Collections.unmodifiableList(teepees);
    }

    public Set<Hazard> getHazards() {
        return Collections.unmodifiableSet(hazards);
    }

    public Set<ObjectiveCard> getCommittedObjectives() {
        return Collections.unmodifiableSet(objectives);
    }

    public Set<StationMaster> getStationMasters() {
        return Collections.unmodifiableSet(stationMasters);
    }

    public Set<PlayerBuilding> getBuildings() {
        return Collections.unmodifiableSet(buildings);
    }

    public List<Card> getDiscardPile() {
        return Collections.unmodifiableList(discardPile);
    }

    public boolean hasJobMarketToken() {
        return jobMarketToken;
    }

    /**
     * "best" because player may have multiple of the same type with different breeding values.
     * This methods determine the maximum breeding value.
     */
    public int handValue() {
        return hand.stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .collect(Collectors.toMap(Card.CattleCard::getType, Card.CattleCard::getValue,
                        // In case more than 1 card of the same type is encountered, take the higher value
                        Integer::max))
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getNumberOfCowboys() {
        return workers.get(Worker.COWBOY);
    }

    public int getNumberOfCraftsmen() {
        return workers.get(Worker.CRAFTSMAN);
    }

    public int getNumberOfEngineers() {
        return workers.get(Worker.ENGINEER);
    }

    public boolean hasAvailable(PlayerBuilding building) {
        return buildings.contains(building);
    }

    Set<PossibleAction> unlockedSingleAuxiliaryActions(GWT game) {
        Set<PossibleAction> actions = new HashSet<>();

        actions.add(PossibleAction.optional(Action.Gain1Dollar.class));
        actions.add(PossibleAction.optional(Action.DrawCard.class));

        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(PossibleAction.optional(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class));
        }
        if (hasUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(PossibleAction.optional(Action.Pay1DollarToMoveEngine1Forward.class));
        }
        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            if (game.getEdition() == GWT.Edition.SECOND) {
                actions.add(PossibleAction.optional(Action.MoveEngine1BackwardsToRemove1CardAndGain1Dollar.class));
            } else {
                actions.add(PossibleAction.optional(Action.MoveEngine1BackwardsToRemove1Card.class));
            }
        }

        if (game.isRailsToTheNorth()) {
            actions.add(PossibleAction.optional(Action.DiscardCattleCardToPlaceBranchlet.class));
        }

        return actions;
    }

    Set<PossibleAction> unlockedSingleOrDoubleAuxiliaryActions(GWT game) {
        Set<PossibleAction> actions = new HashSet<>();

        if (hasUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(PossibleAction.optional(Action.Gain1Dollar.class));
        }
        if (hasAllUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(PossibleAction.optional(Action.Gain2Dollars.class));
        }

        if (hasAllUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(PossibleAction.choice(Action.DrawCard.class, Action.Draw2Cards.class));
        } else if (hasUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(PossibleAction.optional(Action.DrawCard.class));
        }

        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(PossibleAction.optional(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class));
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(PossibleAction.optional(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class));
        }

        if (hasUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(PossibleAction.optional(Action.Pay1DollarToMoveEngine1Forward.class));
        }
        if (hasAllUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(PossibleAction.optional(Action.Pay2DollarsToMoveEngine2Forward.class));
        }

        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            if (game.getEdition() == GWT.Edition.SECOND) {
                actions.add(PossibleAction.optional(Action.MoveEngine1BackwardsToRemove1CardAndGain1Dollar.class));
            } else {
                actions.add(PossibleAction.optional(Action.MoveEngine1BackwardsToRemove1Card.class));
            }
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            if (game.getEdition() == GWT.Edition.SECOND) {
                actions.add(PossibleAction.optional(Action.MoveEngine2BackwardsToRemove2CardsAndGain2Dollars.class));
            } else {
                actions.add(PossibleAction.optional(Action.MoveEngine2BackwardsToRemove2Cards.class));
            }
        }

        if (game.isRailsToTheNorth()) {
            if (hasAllUnlocked(Unlockable.AUX_DISCARD_CATTLE_CARD_TO_PLACE_BRANCHLET)) {
                actions.add(PossibleAction.repeat(0, 2, Action.DiscardCattleCardToPlaceBranchlet.class));
            } else {
                actions.add(PossibleAction.optional(Action.DiscardCattleCardToPlaceBranchlet.class));
            }
        }

        return actions;
    }

    public Map<Unlockable, Integer> getUnlocked() {
        return Collections.unmodifiableMap(unlocked);
    }

    public int getHandLimit() {
        return 4 + unlocked.getOrDefault(Unlockable.EXTRA_CARD, 0);
    }

    public int getStepLimit(int playerCount) {
        if (playerCount == 2) {
            return 3 + unlocked.getOrDefault(Unlockable.EXTRA_STEP_DOLLARS, 0) + unlocked.getOrDefault(Unlockable.EXTRA_STEP_POINTS, 0);
        } else if (playerCount == 3) {
            return 3 + unlocked.getOrDefault(Unlockable.EXTRA_STEP_DOLLARS, 0) * 2 + unlocked.getOrDefault(Unlockable.EXTRA_STEP_POINTS, 0);
        } else {
            return 4 + unlocked.getOrDefault(Unlockable.EXTRA_STEP_DOLLARS, 0) * 2 + unlocked.getOrDefault(Unlockable.EXTRA_STEP_POINTS, 0);
        }
    }

    public Collection<Card> getDrawStack() {
        return Collections.unmodifiableCollection(drawStack);
    }

    boolean hasObjectiveCardInHand() {
        return hand.stream().anyMatch(card -> card instanceof ObjectiveCard);
    }

    int numberOfCattleCards(Set<CattleType> cattleTypes) {
        return (int) getCattleCards()
                .stream()
                .filter(card -> cattleTypes.contains(card.getType()))
                .count();
    }

    int numberOfTeepeePairs() {
        if (teepees.size() < 2) {
            return 0;
        }

        int blueTeepees = (int) teepees.stream()
                .filter(teepee -> teepee == Teepee.BLUE)
                .count();
        int greenTeepees = teepees.size() - blueTeepees;

        return Math.min(blueTeepees, greenTeepees);
    }

    int numberOfObjectiveCards() {
        return objectives.size() + (int) Stream.concat(Stream.concat(hand.stream(), discardPile.stream()), drawStack.stream())
                .filter(card -> card instanceof ObjectiveCard)
                .map(card -> (ObjectiveCard) card)
                .count();
    }

    Score score(GWT game) {
        var objectives = scoreObjectives(game);
        var score = new Score(Map.of(
                ScoreCategory.BID, bid != null ? -bid.getPoints() : 0,
                ScoreCategory.DOLLARS, balance / 5,
                ScoreCategory.CATTLE_CARDS, scoreCattleCards(),
                ScoreCategory.OBJECTIVE_CARDS, objectives.getTotal(),
                ScoreCategory.STATION_MASTERS, scoreStationMasters(game, objectives.getCommitted()),
                ScoreCategory.WORKERS, scoreWorkers(),
                ScoreCategory.HAZARDS, scoreHazards(),
                ScoreCategory.EXTRA_STEP_POINTS, hasUnlocked(Unlockable.EXTRA_STEP_POINTS) ? 3 : 0,
                ScoreCategory.JOB_MARKET_TOKEN, jobMarketToken ? 2 : 0));

        if (automaState != null) {
            score = automaState.adjustScore(score, game, this);
        }

        return score;
    }

    public ObjectiveCard.Score scoreObjectives(GWT game) {
        return ObjectiveCard.score(objectives, getOptionalObjectives().collect(Collectors.toSet()), game, player,
                stationMasters.contains(StationMaster.REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS));
    }

    private int scoreHazards() {
        return hazards.stream()
                .mapToInt(Hazard::getPoints)
                .sum();
    }

    private int scoreWorkers() {
        return workers.values().stream()
                .mapToInt(count -> count == 6 ? 8 : count == 5 ? 4 : 0)
                .sum();
    }

    private boolean hasUnlocked(Unlockable unlockable) {
        return unlocked.getOrDefault(unlockable, 0) > 0;
    }

    public boolean hasAllUnlocked(Unlockable unlockable) {
        return unlocked.getOrDefault(unlockable, 0) == unlockable.getCount();
    }

    private Set<Card.CattleCard> getCattleCards() {
        return Stream.concat(Stream.concat(hand.stream(), discardPile.stream()), drawStack.stream())
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> ((Card.CattleCard) card))
                .collect(Collectors.toSet());
    }

    private int scoreStationMasters(GWT game, Set<ObjectiveCard> committed) {
        int result = 0;
        if (stationMasters.contains(StationMaster.GAIN_2_DOLLARS_POINT_FOR_EACH_WORKER)) {
            result += getNumberOfCowboys() + getNumberOfCraftsmen() + getNumberOfEngineers();
        }
        if (stationMasters.contains(StationMaster.REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS)) {
            result += (committed.size() / 2) * 3;
        }
        if (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_HAZARDS)) {
            result += (hazards.size() / 2) * 3;
        }
        if (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_TEEPEE_PAIRS)) {
            result += numberOfTeepeePairs() * 3;
        }
        if (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_CERTS)) {
            result += ((tempCertificates + permanentCertificates()) / 2) * 3;
        }

        if (stationMasters.contains(StationMaster.PERM_CERT_POINTS_PER_2_STATIONS)) {
            result += (game.getRailroadTrack().numberOfUpgradedStations(player) / 2) * 3;
        }
        if (stationMasters.contains(StationMaster.GAIN_2_CERTS_POINTS_PER_BUILDING)) {
            result += game.getTrail().numberOfBuildings(player) * 2;
        }
        if (stationMasters.contains(StationMaster.PLACE_BRANCHLET_POINTS_PER_2_EXCHANGE_TOKENS)) {
            result += (exchangeTokens / 2) * 5;
        }
        if (stationMasters.contains(StationMaster.GAIN_EXCHANGE_TOKEN_POINTS_PER_AREA)) {
            result += game.getRailroadTrack().numberOfAreas(player) * 2;
        }

        return result;
    }

    private int scoreCattleCards() {
        return getCattleCards().stream()
                .mapToInt(Card.CattleCard::getPoints)
                .sum();
    }

    private static LinkedList<Card> createDrawStack(@NonNull Random random) {
        LinkedList<Card> startingDeck = new LinkedList<>(createStartingDeck());
        Collections.shuffle(startingDeck, random);
        return startingDeck;
    }

    private static List<Card> createStartingDeck() {
        return new ArrayList<>(Arrays.asList(
                new Card.CattleCard(CattleType.JERSEY, 0, 1),
                new Card.CattleCard(CattleType.JERSEY, 0, 1),
                new Card.CattleCard(CattleType.JERSEY, 0, 1),
                new Card.CattleCard(CattleType.JERSEY, 0, 1),
                new Card.CattleCard(CattleType.JERSEY, 0, 1),
                new Card.CattleCard(CattleType.DUTCH_BELT, 0, 2),
                new Card.CattleCard(CattleType.DUTCH_BELT, 0, 2),
                new Card.CattleCard(CattleType.DUTCH_BELT, 0, 2),
                new Card.CattleCard(CattleType.BLACK_ANGUS, 0, 2),
                new Card.CattleCard(CattleType.BLACK_ANGUS, 0, 2),
                new Card.CattleCard(CattleType.BLACK_ANGUS, 0, 2),
                new Card.CattleCard(CattleType.GUERNSEY, 0, 2),
                new Card.CattleCard(CattleType.GUERNSEY, 0, 2),
                new Card.CattleCard(CattleType.GUERNSEY, 0, 2)));
    }

    boolean canRemoveDisc(Collection<DiscColor> discColors, GWT game) {
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> canUnlock(unlockable, game))
                .anyMatch(unlockable -> discColors.contains(unlockable.getDiscColor()));
    }

    public boolean canUnlock(Unlockable unlockable, GWT game) {
        return (unlockable != Unlockable.AUX_DISCARD_CATTLE_CARD_TO_PLACE_BRANCHLET || game.isRailsToTheNorth())
                && unlocked.getOrDefault(unlockable, 0) < unlockable.getCount()
                && balance >= unlockable.getCost();
    }

    void beginTurn() {
        numberOfCowboysUsedInTurn = 0;
        locationsActivatedInTurn.clear();
        turns++;
    }

    void useCowboys(int amount) {
        if (getNumberOfCowboys() - numberOfCowboysUsedInTurn < amount) {
            throw new GWTException(GWTError.NOT_ENOUGH_COWBOYS);
        }
        numberOfCowboysUsedInTurn += amount;
    }

    void rememberLastUpgradedStation(@NonNull Station station) {
        this.lastUpgradedStation = Optional.of(station);
    }

    void activate(Location location) {
        locationsActivatedInTurn.add(location);

        stops.compute(location, (key, value) -> value != null ? value + 1 : 1);
    }

    Optional<Location> getLastActivatedLocation() {
        return locationsActivatedInTurn.isEmpty() ? Optional.empty() : Optional.of(locationsActivatedInTurn.get(locationsActivatedInTurn.size() - 1));
    }

    public Stream<ObjectiveCard> getOptionalObjectives() {
        return Stream.concat(Stream.concat(hand.stream().filter(card -> card instanceof ObjectiveCard).map(card -> (ObjectiveCard) card),
                        discardPile.stream().filter(card -> card instanceof ObjectiveCard).map(card -> (ObjectiveCard) card)),
                drawStack.stream().filter(card -> card instanceof ObjectiveCard).map(card -> (ObjectiveCard) card));
    }

    public int numberOfTeepees() {
        return teepees.size();
    }

    public int numberOfGreenTeepees() {
        return (int) teepees.stream().filter(teepee -> teepee == Teepee.GREEN).count();
    }

    public int numberOfHazards() {
        return hazards.size();
    }

    public Optional<Bid> getBid() {
        return Optional.ofNullable(bid);
    }

    void gainExchangeTokens(int amount) {
        exchangeTokens += amount;
    }

    void payExchangeTokens(int amount) {
        if (exchangeTokens < amount) {
            throw new GWTException(GWTError.NOT_ENOUGH_EXCHANGE_TOKENS);
        }
        exchangeTokens -= amount;
    }

    ImmediateActions removeBranchlet() {
        if (branchlets < 1) {
            throw new GWTException(GWTError.NO_BRANCHLETS);
        }
        branchlets--;

        return branchlets == 9 || branchlets == 0 ? ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class))
                : ImmediateActions.none();
    }

    public int numberOfBells() {
        return 5 - (int) Math.ceil((double) branchlets / 3);
    }

    public int getNumberOfWorkers() {
        return getNumberOfCowboys() + getNumberOfCraftsmen() + getNumberOfEngineers();
    }

    public int certificates() {
        return getTempCertificates() + permanentCertificates();
    }

    void rememberLastPlacedBranchlet(RailroadTrack.Town town) {
        lastPlacedBranchlet = Optional.of(town);
    }

    int getNumberOfWorkers(Worker worker) {
        return workers.getOrDefault(worker, 0);
    }

    boolean hasMaxWorkers(Worker worker) {
        return getNumberOfWorkers(worker) >= 6;
    }
    Set<Worker> getWorkersThatCanBeHired() {
        var result = new HashSet<Worker>();
        if (!hasMaxWorkers(Worker.COWBOY)) {
            result.add(Worker.COWBOY);
        }
        if (!hasMaxWorkers(Worker.CRAFTSMAN)) {
            result.add(Worker.CRAFTSMAN);
        }
        if (!hasMaxWorkers(Worker.ENGINEER)) {
            result.add(Worker.ENGINEER);
        }
        return result;
    }

    public Optional<Garth> getAutomaState() {
        return Optional.ofNullable(automaState);
    }

    public boolean hasUsedBuildingInTurn(Building building) {
        return locationsActivatedInTurn.stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .flatMap(location -> ((Location.BuildingLocation) location).getBuilding().stream())
                .anyMatch(usedBuilding -> usedBuilding.getName().equals(building.getName()));
    }

    Unlockable chooseAnyWhiteDisc(GWT game) {
        if (canUnlock(Unlockable.AUX_GAIN_DOLLAR, game)) {
            return Unlockable.AUX_GAIN_DOLLAR;
        }
        if (canUnlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD, game)) {
            return Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD;
        }
        if (canUnlock(Unlockable.CERT_LIMIT_4, game)) {
            return Unlockable.CERT_LIMIT_4;
        }

        // TODO Just pick any now
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> unlockable.getDiscColor() == DiscColor.WHITE)
                .filter(unlockable1 -> canUnlock(unlockable1, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    Unlockable chooseAnyBlackOrWhiteDisc(GWT game) {
        if (canUnlock(Unlockable.EXTRA_STEP_DOLLARS, game)) {
            return Unlockable.EXTRA_STEP_DOLLARS;
        }
        if (canUnlock(Unlockable.EXTRA_CARD, game)) {
            return Unlockable.EXTRA_CARD;
        }

        // TODO Just pick any now
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> canUnlock(unlockable, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    void endTurn(GWT game, Random random) {
        drawUpToHandLimit(random);

        numberOfCowboysUsedInTurn = 0;
        locationsActivatedInTurn.clear();
        lastUpgradedStation = Optional.empty();
        lastPlacedBranchlet = Optional.empty();
        lastEngineMove = 0;
    }

    int simmentalsToUpgrade() {
        return (int) hand
                .stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(card -> card.getType() == CattleType.SIMMENTAL)
                .filter(card -> card.getValue() < 5)
                .count();
    }
}
