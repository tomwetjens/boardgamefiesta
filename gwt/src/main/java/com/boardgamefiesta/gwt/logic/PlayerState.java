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
    private final List<Location> locationsActivatedInTurn;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private int lastEngineMove;
    @Getter
    private Optional<Station> lastUpgradedStation;

    PlayerState(@NonNull Player player, int balance, @NonNull Random random, PlayerBuilding.BuildingSet buildings) {
        this.player = player;
        this.balance = balance;
        this.tempCertificates = 0;

        this.drawStack = createDrawStack(random);
        this.discardPile = new LinkedList<>();

        this.hand = new HashSet<>();

        this.workers = new EnumMap<>(Worker.class);
        this.workers.put(Worker.COWBOY, 1);
        this.workers.put(Worker.CRAFTSMAN, 1);
        this.workers.put(Worker.ENGINEER, 1);

        this.buildings = new HashSet<>(buildings.createPlayerBuildings(player));
        this.unlocked = new EnumMap<>(Unlockable.class);
        this.unlocked.put(Unlockable.AUX_GAIN_DOLLAR, 1);
        this.unlocked.put(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD, 1);

        this.objectives = new HashSet<>();

        this.stationMasters = new HashSet<>();
        this.teepees = new LinkedList<>();
        this.hazards = new HashSet<>();

        this.locationsActivatedInTurn = new LinkedList<>();

        this.lastUpgradedStation = Optional.empty();

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
                .build();
    }

    static PlayerState deserialize(Player player, RailroadTrack railroadTrack, Trail trail, JsonObject jsonObject) {
        return new PlayerState(player,
                jsonObject.getJsonArray("drawStack").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("hand").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card::deserialize)
                        .collect(Collectors.toSet()),
                jsonObject.getJsonArray("discardPile").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                JsonDeserializer.forObject(jsonObject.getJsonObject("workers")).<Worker>asIntegerMap(Worker::valueOf),
                jsonObject.getJsonArray("buildings")
                        .getValuesAs(JsonString::getString).stream()
                        .map(name -> PlayerBuilding.forName(name, player))
                        .collect(Collectors.toSet()),
                JsonDeserializer.forObject(jsonObject.getJsonObject("unlocked")).<Unlockable>asIntegerMap(Unlockable::valueOf),
                jsonObject.getJsonArray("objectives").stream().map(JsonValue::asJsonObject).map(ObjectiveCard::deserialize).collect(Collectors.toSet()),
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
                JsonDeserializer.getInt("lastUpgradedStation", jsonObject).filter(index -> index >= 0).map(railroadTrack.getStations()::get));
    }

    void placeBid(Bid bid) {
        this.bid = bid;
    }

    void drawCard(Random random) {
        if (drawStack.isEmpty()) {
            Collections.shuffle(discardPile, random);
            drawStack.addAll(discardPile);
            discardPile.clear();
        }

        if (!drawStack.isEmpty()) {
            hand.add(drawStack.poll());
        }
    }

    void drawCards(int count, Random random) {
        for (int n = 0; n < count; n++) {
            drawCard(random);
        }
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
                .limit(amount)
                .collect(Collectors.toSet());

        if (cattleCards.size() != amount) {
            throw new GWTException(GWTError.CATTLE_CARDS_NOT_IN_HAND);
        }

        hand.removeAll(cattleCards);

        discardPile.addAll(0, cattleCards);

        return Collections.unmodifiableSet(cattleCards);
    }

    void discardHand() {
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

    ImmediateActions gainWorker(Worker worker, Game game) {
        if (workers.get(worker) == 6) {
            throw new GWTException(GWTError.WORKERS_EXCEED_LIMIT);
        }

        int count = workers.compute(worker, (k, v) -> v + 1);

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
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_TEEPEE_PAIRS) ? 1 : 0);
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

    public int handValue() {
        return hand.stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .map(Card.CattleCard::getType)
                .distinct()
                .mapToInt(CattleType::getValue)
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

    Set<PossibleAction> unlockedSingleAuxiliaryActions() {
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
            actions.add(PossibleAction.optional(Action.MoveEngine1BackwardsToRemove1Card.class));
        }
        return actions;
    }

    Set<PossibleAction> unlockedSingleOrDoubleAuxiliaryActions() {
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
            actions.add(PossibleAction.optional(Action.MoveEngine1BackwardsToRemove1Card.class));
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            actions.add(PossibleAction.optional(Action.MoveEngine2BackwardsToRemove2Cards.class));
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

    int numberOfCattleCards(int breedingValue) {
        return (int) getCattleCards()
                .stream()
                .filter(card -> card.getType().getValue() == breedingValue)
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

    Score score(Game game) {
        var objectives = scoreObjectives(game);
        return new Score(Map.of(
                ScoreCategory.BID, bid != null ? -bid.getPoints() : 0,
                ScoreCategory.DOLLARS, balance / 5,
                ScoreCategory.CATTLE_CARDS, scoreCattleCards(),
                ScoreCategory.OBJECTIVE_CARDS, objectives.getTotal(),
                ScoreCategory.STATION_MASTERS, scoreStationMasters(objectives.getCommitted()),
                ScoreCategory.WORKERS, scoreWorkers(),
                ScoreCategory.HAZARDS, scoreHazards(),
                ScoreCategory.EXTRA_STEP_POINTS, hasUnlocked(Unlockable.EXTRA_STEP_POINTS) ? 3 : 0,
                ScoreCategory.JOB_MARKET_TOKEN, jobMarketToken ? 2 : 0));
    }

    public ObjectiveCard.Score scoreObjectives(Game game) {
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

    private int scoreStationMasters(Set<ObjectiveCard> committed) {
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
                new Card.CattleCard(CattleType.JERSEY, 0),
                new Card.CattleCard(CattleType.JERSEY, 0),
                new Card.CattleCard(CattleType.JERSEY, 0),
                new Card.CattleCard(CattleType.JERSEY, 0),
                new Card.CattleCard(CattleType.JERSEY, 0),
                new Card.CattleCard(CattleType.DUTCH_BELT, 0),
                new Card.CattleCard(CattleType.DUTCH_BELT, 0),
                new Card.CattleCard(CattleType.DUTCH_BELT, 0),
                new Card.CattleCard(CattleType.BLACK_ANGUS, 0),
                new Card.CattleCard(CattleType.BLACK_ANGUS, 0),
                new Card.CattleCard(CattleType.BLACK_ANGUS, 0),
                new Card.CattleCard(CattleType.GUERNSEY, 0),
                new Card.CattleCard(CattleType.GUERNSEY, 0),
                new Card.CattleCard(CattleType.GUERNSEY, 0)));
    }

    boolean canRemoveDisc(Collection<DiscColor> discColors) {
        return Arrays.stream(Unlockable.values())
                .filter(this::canUnlock)
                .anyMatch(unlockable -> discColors.contains(unlockable.getDiscColor()));
    }

    public boolean canUnlock(Unlockable unlockable) {
        return unlocked.getOrDefault(unlockable, 0) < unlockable.getCount()
                && balance >= unlockable.getCost();
    }

    void beginTurn() {
        numberOfCowboysUsedInTurn = 0;
        locationsActivatedInTurn.clear();
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
        if (locationsActivatedInTurn.contains(location)) {
            throw new GWTException(GWTError.CANNOT_PERFORM_ACTION);
        }

        locationsActivatedInTurn.add(location);
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
}
