package com.wetjens.gwt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerState implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final List<Integer> CERTIFICATE_STEPS = Arrays.asList(0, 1, 2, 3, 4, 6);

    static final int MAX_HAND_LIMIT = 6;
    static final int MAX_STEP_LIMIT = 5;

    @Getter
    private final Player player;
    private final Queue<Card> drawStack;
    private final Set<Card> hand;
    private final List<Card> discardPile = new LinkedList<>();
    private final Map<Worker, Integer> workers = new EnumMap<>(Worker.class);
    private final Set<PlayerBuilding> buildings;
    @Singular("unlocked")
    private final Map<Unlockable, Integer> unlocked;
    private final Set<ObjectiveCard> objectives = new HashSet<>();
    private final Set<StationMaster> stationMasters = new HashSet<>();
    private final List<Teepee> teepees = new LinkedList<>();
    private final Set<Hazard> hazards = new HashSet<>();

    @Getter
    private int tempCertificates;
    @Getter
    private int balance;
    @Getter
    private boolean jobMarketToken;

    PlayerState(@NonNull Player player, int balance, @NonNull ObjectiveCard startingObjectiveCard, @NonNull Random random, PlayerBuilding.BuildingSet buildings) {
        this.player = player;
        this.balance = balance;
        this.tempCertificates = 0;

        this.drawStack = createDrawStack(random);

        this.hand = new HashSet<>();

        this.workers.put(Worker.COWBOY, 1);
        this.workers.put(Worker.CRAFTSMAN, 1);
        this.workers.put(Worker.ENGINEER, 1);

        this.buildings = new HashSet<>(buildings.createPlayerBuildings(player));
        this.unlocked = new EnumMap<>(Unlockable.class);
        this.unlocked.put(Unlockable.AUX_GAIN_DOLLAR, 1);
        this.unlocked.put(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD, 1);

        this.objectives.add(startingObjectiveCard);

        drawUpToHandLimit(random);
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
            throw new GWTException(GWTError.CATTLE_CARDS_NOT_IN_HAND, type, amount);
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
        balance += amount;
    }

    void payDollars(int amount) {
        if (balance < amount) {
            throw new GWTException(GWTError.NOT_ENOUGH_BALANCE_TO_PAY, amount);
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
                game.fireEvent(player, GWTEvent.Type.MAY_TRADE_WITH_INDIANS, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.optional(Action.TradeWithIndians.class));
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
                return ImmediateActions.of(PossibleAction.optional(Action.HireCheapWorker.class));
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

        objectives.add(objectiveCard);

        return objectiveCard.getPossibleAction()
                .map(PossibleAction::clone)
                .map(ImmediateActions::of)
                .orElse(ImmediateActions.none());
    }

    void removeWorker(Worker worker) {
        if (workers.get(worker) <= 1) {
            throw new GWTException(GWTError.NOT_ENOUGH_WORKERS, worker);
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
            throw new GWTException(GWTError.ALREADY_UNLOCKED, unlockable);
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

    public Set<ObjectiveCard> getObjectives() {
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

    public Set<RailroadTrack.PossibleDelivery> possibleDeliveries(RailroadTrack railroadTrack) {
        return railroadTrack.possibleDeliveries(player, handValue(), tempCertificates + permanentCertificates());
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
        actions.add(PossibleAction.whenThen(0, 1, Action.DrawCard.class, Action.DiscardCard.class));

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
            actions.add(PossibleAction.whenThen(0, 2, Action.DrawCard.class, Action.DiscardCard.class));
        } else if (hasUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(PossibleAction.whenThen(0, 1, Action.DrawCard.class, Action.DiscardCard.class));
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

    public int getDrawStackSize() {
        return drawStack.size();
    }

    boolean canPlayObjectiveCard() {
        return hand.stream().anyMatch(card -> card instanceof ObjectiveCard);
    }

    int numberOfCattleCards(int breedingValue) {
        return (int) getCattleCards()
                .stream()
                .map(card -> card.getType().getValue() == breedingValue)
                .count();
    }

    int numberOfTeepeePairs() {
        int blueTeepees = (int) teepees.stream()
                .filter(teepee -> teepee == Teepee.BLUE)
                .count();
        int greenTeepees = teepees.size() - blueTeepees;

        return Math.max(blueTeepees, greenTeepees) / Math.min(blueTeepees, greenTeepees);
    }

    int numberOfObjectiveCards() {
        return objectives.size() + (int) Stream.concat(Stream.concat(hand.stream(), discardPile.stream()), drawStack.stream())
                .filter(card -> card instanceof ObjectiveCard)
                .map(card -> (ObjectiveCard) card)
                .count();
    }

    int score(Game game) {
        return balance / 5
                + scoreCattleCards()
                + scoreObjectiveCards(game)
                + scoreStationMasters()
                + scoreWorkers()
                + scoreHazards()
                + (hasUnlocked(Unlockable.EXTRA_STEP_POINTS) ? 3 : 0)
                + (jobMarketToken ? 2 : 0);
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

    private boolean hasAllUnlocked(Unlockable unlockable) {
        return unlocked.getOrDefault(unlockable, 0) == unlockable.getCount();
    }

    private Set<Card.CattleCard> getCattleCards() {
        return Stream.concat(Stream.concat(hand.stream(), discardPile.stream()), drawStack.stream())
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> ((Card.CattleCard) card))
                .collect(Collectors.toSet());
    }

    private int scoreStationMasters() {
        return stationMasters.stream().mapToInt(stationMaster -> stationMaster.score(this)).sum();
    }

    private int scoreCattleCards() {
        return getCattleCards().stream()
                .mapToInt(Card.CattleCard::getPoints)
                .sum();
    }

    private int scoreObjectiveCards(Game game) {
        Set<ObjectiveCard> otherObjectiveCards = Stream.concat(Stream.concat(hand.stream(), discardPile.stream()), drawStack.stream())
                .filter(card -> card instanceof ObjectiveCard)
                .map(card -> (ObjectiveCard) card)
                .collect(Collectors.toSet());

        return ObjectiveCard.score(objectives, otherObjectiveCards, game, player);
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

    boolean canUnlock(Collection<DiscColor> discColors) {
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> unlocked.getOrDefault(unlockable, 0) < unlockable.getCount())
                .filter(unlockable -> balance >= unlockable.getCost())
                .anyMatch(unlockable -> discColors.contains(unlockable.getDiscColor()));
    }

}
