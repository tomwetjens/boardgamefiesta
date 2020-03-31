package com.wetjens.gwt;

import lombok.Getter;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerState {

    @Getter
    private final Player player;
    private final Queue<Card> drawStack;
    private final Set<Card> hand = new HashSet<>();
    private final List<Card> discardPile = new LinkedList<>();
    private final Map<Worker, Integer> workers = new EnumMap<>(Worker.class);
    private final Set<PlayerBuilding> buildings;
    private final Map<Unlockable, Integer> unlocked = new EnumMap<>(Unlockable.class);
    private final Set<ObjectiveCard> objectives = new HashSet<>();
    private final Set<StationMaster> stationMasters = new HashSet<>();
    private final List<Teepee> teepees = new LinkedList<>();

    @Getter
    private int stepLimit = 3;
    private int handLimit = 4;
    private int certificateLimit = 4;
    private int certificates = 0;
    @Getter
    private int balance;

    public PlayerState(@NonNull Player player, int balance, @NonNull Random random, @NonNull Set<PlayerBuilding> playerBuildings) {
        this.player = player;
        this.balance = balance;

        LinkedList<Card> startingDeck = new LinkedList<>(createStartingDeck());
        Collections.shuffle(startingDeck, random);
        this.drawStack = startingDeck;

        this.workers.put(Worker.COWBOY, 1);
        this.workers.put(Worker.CRAFTSMAN, 1);
        this.workers.put(Worker.ENGINEER, 1);

        this.buildings = new HashSet<>(playerBuildings);

        drawUpToHandLimit();
    }

    void gainCertificates(int amount) {
        if (certificates + amount > certificateLimit) {
            throw new IllegalArgumentException("Gaining 1 certificate would exceed certificate limit");
        }

        certificates += amount;
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

    void discardCattleCards(CattleType type, int amount) {
        Set<Card.CattleCard> cattleCards = hand.stream().filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                .limit(amount)
                .collect(Collectors.toSet());

        if (cattleCards.size() != amount) {
            throw new IllegalArgumentException("Player hand does not contain  " + amount + " cattle cards of type " + type);
        }

        hand.removeAll(cattleCards);

        discardPile.addAll(cattleCards);
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
                return ImmediateActions.of(PossibleAction.optional(Action.DiscardOneJerseyToGainCertificate.class));
            } else if (count == 3) {
                return ImmediateActions.of(PossibleAction.optional(Action.DiscardOneJerseyToGainTwoDollars.class));
            } else if (count == 4) {
                return ImmediateActions.of(PossibleAction.optional(Action.HireCheapWorker.class));
            } else if (count == 5) {
                return ImmediateActions.of(PossibleAction.optional(Action.DiscardOneJerseyToGainTwoCertificates.class));
            } else if (count == 6) {
                return ImmediateActions.of(PossibleAction.optional(Action.DiscardOneJerseyToGainFourDollars.class));
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
            actions.add(Action.SingleAuxiliaryAction.Gain1Dollars.class);
        }
        if (hasUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(Action.DrawCardThenDiscardCard.Draw1CardThenDiscard1Card.class);
        }
        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(Action.SingleAuxiliaryAction.Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate.class);
        }
        if (hasUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(Action.SingleAuxiliaryAction.Pay1DollarToMoveEngine1SpaceForward.class);
        }
        if (hasUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            actions.add(Action.SingleAuxiliaryAction.MoveEngine1SpaceBackwardsToRemove1Card.class);
        }
        return actions;
    }

    public Set<Class<? extends Action>> unlockedDoubleAuxiliaryActions() {
        Set<Class<? extends Action>> actions = new HashSet<>(unlockedSingleAuxiliaryActions());

        if (hasAllUnlocked(Unlockable.AUX_GAIN_DOLLAR)) {
            actions.add(Action.SingleOrDoubleAuxiliaryAction.Gain2Dollars.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD)) {
            actions.add(Action.DrawCardThenDiscardCard.Draw2CardsThenDiscard2Cards.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_GAIN_CERT)) {
            actions.add(Action.SingleOrDoubleAuxiliaryAction.Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_PAY_TO_MOVE_ENGINE_FORWARD)) {
            actions.add(Action.Pay2DollarsToMoveEngine2SpacesForward.class);
        }
        if (hasAllUnlocked(Unlockable.AUX_MOVE_ENGINE_BACKWARDS_TO_REMOVE_CARD)) {
            actions.add(Action.SingleOrDoubleAuxiliaryAction.MoveEngine2SpacesBackwardsToRemove2Cards.class);
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
        stationMasters.add(stationMaster);
    }

    void addTeepee(Teepee teepee) {
        teepees.add(teepee);
    }

    void removeCards(Set<Card> cards) {
        hand.removeAll(cards);
    }

}
