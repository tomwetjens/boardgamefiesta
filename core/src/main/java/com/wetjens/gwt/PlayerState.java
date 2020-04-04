package com.wetjens.gwt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    private int certificates;
    @Getter
    private int balance;
    @Getter
    private boolean jobMarketToken;

    PlayerState(@NonNull Player player, int balance, @NonNull Random random, PlayerBuilding.BuildingSet buildings) {
        this.player = player;
        this.balance = balance;
        this.certificates = 0;

        this.drawStack = createDrawStack(random);

        this.hand = new HashSet<>();

        this.workers.put(Worker.COWBOY, 1);
        this.workers.put(Worker.CRAFTSMAN, 1);
        this.workers.put(Worker.ENGINEER, 1);

        this.buildings = new HashSet<>(buildings.createPlayerBuildings(player));
        this.unlocked = new EnumMap<>(Unlockable.class);

        // TODO Starting objective card

        drawUpToHandLimit();
    }

    void drawCard() {
        if (drawStack.isEmpty()) {
            Collections.shuffle(discardPile);
            drawStack.addAll(discardPile);
            discardPile.clear();
        }

        if (!drawStack.isEmpty()) {
            hand.add(drawStack.poll());
        }
    }

    void drawUpToHandLimit() {
        int handLimit = getHandLimit();
        while (hand.size() < handLimit && drawStack.size() + discardPile.size() > 0) {
            drawCard();
        }
    }

    void gainCard(Card card) {
        discardPile.add(card);
    }

    void gainCards(Set<? extends Card> cards) {
        discardPile.addAll(cards);
    }

    Set<Card.CattleCard> discardCattleCards(CattleType type, int amount) {
        Set<Card.CattleCard> cattleCards = hand.stream().filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                // Assumed for now player wants to discard the lowest points to keep a good hand value
                .sorted(Comparator.comparingInt(Card.CattleCard::getPoints))
                .limit(amount)
                .collect(Collectors.toSet());

        if (cattleCards.size() != amount) {
            throw new IllegalArgumentException("Player hand does not contain " + amount + " cattle cards of type " + type);
        }

        hand.removeAll(cattleCards);

        discardPile.addAll(cattleCards);

        return Collections.unmodifiableSet(cattleCards);
    }

    void discardAllCards() {
        discardPile.addAll(hand);
        hand.clear();
    }

    void gainDollars(int amount) {
        balance += amount;
    }

    void payDollars(int amount) {
        if (balance < amount) {
            throw new IllegalArgumentException("Not enough balance to pay " + amount);
        }
        balance -= amount;
    }

    ImmediateActions gainWorker(Worker worker) {
        int count = workers.compute(worker, (k, v) -> v + 1);

        if (worker == Worker.COWBOY) {
            if (count == 4) {
                return ImmediateActions.of(PossibleAction.optional(Action.RemoveHazardForFree.class));
            } else if (count == 6) {
                return ImmediateActions.of(PossibleAction.optional(Action.TradeWithIndians.class));
            }
        } else if (worker == Worker.CRAFTSMAN) {
            if (count == 4 || count == 6) {
                return ImmediateActions.of(PossibleAction.optional(Action.PlaceCheapBuilding.class));
            }
        } else {
            if (count == 2) {
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain1Certificate.class));
            } else if (count == 3) {
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain2Dollars.class));
            } else if (count == 4) {
                return ImmediateActions.of(PossibleAction.optional(Action.HireCheapWorker.class));
            } else if (count == 5) {
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain2Certificates.class));
            } else if (count == 6) {
                return ImmediateActions.of(PossibleAction.optional(Action.Discard1JerseyToGain4Dollars.class));
            }
        }
        return ImmediateActions.none();
    }

    void removeBuilding(PlayerBuilding building) {
        if (!buildings.remove(building)) {
            throw new IllegalStateException("Building not available for player");
        }
    }

    void discardCard(Card card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException("Card must be in hand");
        }

        discardPile.add(card);
    }

    ImmediateActions playObjectiveCard(ObjectiveCard objectiveCard) {
        if (!hand.remove(objectiveCard)) {
            throw new IllegalStateException("Objective card not in hand");
        }
        objectives.add(objectiveCard);
        return ImmediateActions.of(objectiveCard.getPossibleAction());
    }

    void removeWorker(Worker worker) {
        if (workers.get(worker) <= 1) {
            throw new IllegalStateException("Not enough workers");
        }

        workers.computeIfPresent(worker, (k, v) -> v - 1);
    }

    void addStationMaster(StationMaster stationMaster) {
        if (!stationMasters.add(stationMaster)) {
            throw new IllegalArgumentException("Already has station master");
        }
    }

    void addTeepee(Teepee teepee) {
        teepees.add(teepee);
    }

    void removeCards(Set<Card> cards) {
        if (!hand.containsAll(cards)) {
            throw new IllegalArgumentException("Cards not in hand");
        }
        hand.removeAll(cards);
    }

    void addHazard(Hazard hazard) {
        if (!hazards.add(hazard)) {
            throw new IllegalArgumentException("Already has hazard");
        }
    }

    void gainCertificates(int amount) {
        int step = Math.min(CERTIFICATE_STEPS.size() - 1, CERTIFICATE_STEPS.indexOf(certificates) + amount);
        certificates = CERTIFICATE_STEPS.get(Math.min(getCertificateLimit(), step));
    }

    void spendCertificates(int amount) {
        if (amount - permanentCertificates() > certificates) {
            throw new IllegalArgumentException("Not enough certificates");
        }
        int remaining = amount - permanentCertificates();
        while (remaining > 0) {
            int step = Math.max(0, CERTIFICATE_STEPS.indexOf(certificates) - 1);
            int spent = certificates - CERTIFICATE_STEPS.get(step);
            certificates -= spent;
            remaining -= spent;
        }
    }

    void deliverToCity(Unlockable unlockable, City city) {
        if (!city.accepts(unlockable.getDiscColor())) {
            throw new IllegalArgumentException("City does not accept: " + unlockable.getDiscColor());
        }

        if (unlocked.getOrDefault(unlockable, 0) == unlockable.getCount()) {
            throw new IllegalArgumentException("Already unlocked all " + unlockable);
        }

        unlocked.compute(unlockable, (k, v) -> v != null ? v + 1 : 1);

        if (city == City.KANSAS_CITY) {
            balance += 6;
        }
    }

    void gainJobMarketToken() {
        jobMarketToken = true;
    }

    void gainMaxCertificates() {
        certificates = Math.min(getCertificateLimit(), CERTIFICATE_STEPS.get(CERTIFICATE_STEPS.size() - 1));
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
        return railroadTrack.possibleDeliveries(player, handValue(), certificates + permanentCertificates());
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

    public Set<Class<? extends Action>> unlockedSingleAuxiliaryActions() {
        Set<Class<? extends Action>> actions = new HashSet<>();
        if (hasUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(Action.Gain1Dollar.class);
        }
        if (hasUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(Action.DrawCardsThenDiscardCards.exactly(1));
        }
        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class);
        }
        if (hasUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(Action.Pay1DollarToMoveEngine1Forward.class);
        }
        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            actions.add(Action.MoveEngine1BackwardsToRemove1Card.class);
        }
        return actions;
    }

    public Set<Class<? extends Action>> unlockedDoubleAuxiliaryActions() {
        Set<Class<? extends Action>> actions = new HashSet<>(unlockedSingleAuxiliaryActions());

        if (hasAllUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(Action.Gain2Dollars.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(Action.DrawCardsThenDiscardCards.exactly(2));
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(Action.Pay2DollarsToMoveEngine2Forward.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            actions.add(Action.MoveEngine2BackwardsToRemove2Cards.class);
        }

        return actions;
    }

    public int getHandLimit() {
        return 3 + unlocked.getOrDefault(Unlockable.EXTRA_CARD, 0);
    }

    public int getCertificateLimit() {
        if (hasUnlocked(Unlockable.CERT_LIMIT_6) && hasUnlocked(Unlockable.CERT_LIMIT_4)) {
            return 6;
        } else if (hasUnlocked(Unlockable.CERT_LIMIT_4)) {
            return 4;
        }
        return 3;
    }

    public int getStepLimit() {
        return 3 + unlocked.getOrDefault(Unlockable.EXTRA_STEP_DOLLARS,0) + unlocked.getOrDefault(Unlockable.EXTRA_STEP_POINTS,0);
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

    int permanentCertificates() {
        return (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_CERTS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_HAZARDS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_TEEPEE_PAIRS) ? 1 : 0);
    }

    int score(Game game) {
        return balance / 5
                + scoreCattleCards()
                + scoreObjectiveCards(game)
                + scoreStationMasters()
                + (hasUnlocked(Unlockable.EXTRA_STEP_POINTS) ? 3 : 0)
                + (jobMarketToken ? 2 : 0);
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
}
