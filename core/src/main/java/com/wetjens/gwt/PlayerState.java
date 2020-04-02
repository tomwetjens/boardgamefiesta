package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final List<Card> discardPile = new LinkedList<>();
    private final Map<Worker, Integer> workers = new EnumMap<>(Worker.class);
    private final Set<PlayerBuilding> buildings;
    private final Map<Unlockable, Integer> unlocked = new EnumMap<>(Unlockable.class);
    private final Set<ObjectiveCard> objectives = new HashSet<>();
    private final Set<StationMaster> stationMasters = new HashSet<>();
    private final List<Teepee> teepees = new LinkedList<>();
    private final Set<Hazard> hazards = new HashSet<>();

    @Getter
    private int stepLimit = 3;
    @Getter
    private int handLimit = 4;
    @Getter
    private int certificateLimit = 4;
    @Getter
    private int certificates = 0;
    @Getter
    private int balance;
    @Getter
    private boolean jobMarketToken;

    public PlayerState(@NonNull Player player, int balance, @NonNull Random random, PlayerBuilding.VariantSet buildings) {
        this.player = player;
        this.balance = balance;

        LinkedList<Card> startingDeck = new LinkedList<>(createStartingDeck());
        Collections.shuffle(startingDeck, random);
        this.drawStack = startingDeck;

        this.hand = new HashSet<>();

        this.workers.put(Worker.COWBOY, 1);
        this.workers.put(Worker.CRAFTSMAN, 1);
        this.workers.put(Worker.ENGINEER, 1);

        this.buildings = new HashSet<>(buildings.createPlayerBuildings(player));

        // TODO Starting objective card

        drawUpToHandLimit();
    }

    void gainCertificates(int amount) {
        int step = Math.min(CERTIFICATE_STEPS.size() - 1, CERTIFICATE_STEPS.indexOf(certificates) + amount);
        certificates = CERTIFICATE_STEPS.get(Math.min(certificateLimit, step));
    }

    void drawUpToHandLimit() {
        while (hand.size() < handLimit && drawStack.size() + discardPile.size() > 0) {
            drawCard();
        }
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

    void gainCard(Card card) {
        discardPile.add(card);
    }

    Set<Card.CattleCard> discardCattleCards(CattleType type, int amount) {
        Set<Card.CattleCard> cattleCards = hand.stream().filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                .limit(amount)
                .collect(Collectors.toSet());

        if (cattleCards.size() != amount) {
            throw new IllegalArgumentException("Player hand does not contain " + amount + " cattle cards of type " + type);
        }

        hand.removeAll(cattleCards);

        discardPile.addAll(cattleCards);

        return Collections.unmodifiableSet(cattleCards);
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

    public int getNumberOfCowboys() {
        return workers.get(Worker.COWBOY);
    }

    public int getNumberOfCraftsmen() {
        return workers.get(Worker.CRAFTSMAN);
    }

    public boolean hasAvailable(PlayerBuilding building) {
        return buildings.contains(building);
    }

    void removeBuilding(PlayerBuilding building) {
        if (!buildings.remove(building)) {
            throw new IllegalStateException("Building not available for player");
        }
    }

    public int getNumberOfEngineers() {
        return workers.get(Worker.ENGINEER);
    }

    void discardCard(Card card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException("Card must be in hand");
        }

        discardPile.add(card);
    }

    void unlockExtraCard() {
        if (handLimit == 6) {
            throw new IllegalStateException("Already at max hand limit");
        }

        payDollars(5);

        handLimit++;
    }

    public Set<Card> getHand() {
        return Collections.unmodifiableSet(hand);
    }

    public boolean hasUnlocked(Unlockable unlockable) {
        return unlocked.getOrDefault(unlockable, 0) > 0;
    }

    public boolean hasAllUnlocked(Unlockable unlockable) {
        return unlocked.getOrDefault(unlockable, 0) == unlockable.getCount();
    }

    public Set<Class<? extends Action>> unlockedSingleAuxiliaryActions() {
        Set<Class<? extends Action>> actions = new HashSet<>();
        if (hasUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(Action.Gain1Dollars.class);
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

    ImmediateActions playObjectiveCard(ObjectiveCard objectiveCard) {
        if (!hand.remove(objectiveCard)) {
            throw new IllegalStateException("Objective card not in hand");
        }
        objectives.add(objectiveCard);
        return ImmediateActions.of(objectiveCard.getPossibleAction());
    }

    boolean canPlayObjectiveCard() {
        return hand.stream().anyMatch(card -> card instanceof ObjectiveCard);
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
        if (!teepees.add(teepee)) {
            throw new IllegalArgumentException("Already has teepee");
        }
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

    public void spendCertificates(int amount) {
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

    int permanentCertificates() {
        return (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_CERTS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_EACH_2_HAZARDS) ? 1 : 0)
                + (stationMasters.contains(StationMaster.PERM_CERT_POINTS_FOR_TEEPEE_PAIRS) ? 1 : 0);
    }

    public void discardAllCards() {
        discardPile.addAll(hand);
        hand.clear();
    }

    public List<Teepee> getTeepees() {
        return Collections.unmodifiableList(teepees);
    }

    public Set<Hazard> getHazards() {
        return Collections.unmodifiableSet(hazards);
    }

    public void deliverToCity(Unlockable unlockable, City city) {
        if (!city.accepts(unlockable.getDiscColor())) {
            throw new IllegalArgumentException("City does not accept: " + unlockable.getDiscColor());
        }

        if (city == City.KANSAS_CITY) {
            balance += 6;
        }

        // TODO
    }

    public void gainMaxCertificates() {
        certificates = Math.min(certificateLimit, CERTIFICATE_STEPS.get(CERTIFICATE_STEPS.size() - 1));
    }

    public void addCardToHand(Card card) {
        hand.add(card);
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

    public int numberOfCattleCards(int breedingValue) {
        return (int) getCattleCards()
                .stream()
                .map(card -> ((Card.CattleCard) card).getType().getValue() == breedingValue)
                .count();
    }

    private Set<Card.CattleCard> getCattleCards() {
        return Stream.concat(Stream.concat(hand.stream(), discardPile.stream()), drawStack.stream())
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> ((Card.CattleCard) card))
                .collect(Collectors.toSet());
    }

    int score(Game game) {
        return balance / 5
                + scoreCattleCards()
                + scoreObjectiveCards(game)
                + scoreStationMasters()
                + (hasUnlocked(Unlockable.EXTRA_STEP_POINTS) ? 3 : 0)
                + (jobMarketToken ? 2 : 0);
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

    void gainJobMarketToken() {
        jobMarketToken = true;
    }

    public boolean hasJobMarketToken() {
        return jobMarketToken;
    }
}
