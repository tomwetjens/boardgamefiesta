package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

public class PlayerState {

    @Getter
    private final Player player;
    private final List<Card> drawStack;
    private final Set<Card> hand = new HashSet<>();
    private final List<Card> discardPile = new LinkedList<>();
    private final Map<Worker, Integer> workers = new EnumMap<>(Worker.class);
    private final Set<PlayerBuilding> buildings;
    private final Map<Unlockable, Integer> unlocked = new EnumMap<>(Unlockable.class);
    private final Set<ObjectiveCard> objectives = new HashSet<>();

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

        this.drawStack = createStartingDeck();
        Collections.shuffle(this.drawStack, random);

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

    private void drawUpToHandLimit() {
        draw(handLimit - hand.size());
    }

    private void draw(int count) {
        if (hand.size() + count > handLimit) {
            throw new IllegalArgumentException("Drawing " + count + " cards would exceed hand limit");
        }

        List<Card> take = drawStack.subList(0, 4);
        hand.addAll(take);
        take.clear(); // removes from draw stack
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

    public void gainCard(Card card) {
        discardPile.add(card);
    }

    public void discardCattleCards(CattleType type, int amount) {
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

    public void gainDollars(int amount) {
        balance += amount;
    }

    public void payDollars(int amount) {
        if (balance < amount) {
            throw new IllegalArgumentException("Not enough balance to pay " + amount);
        }
        balance -= amount;
    }

    public ImmediateActions gainWorker(Worker worker) {
        int count = workers.computeIfPresent(worker, (k, v) -> v + 1);

        if (worker == Worker.COWBOY) {
            if (count == 4) {
                return ImmediateActions.of(PossibleAction.of(RemoveHazardForFree.class));
            } else if (count == 6) {
                return ImmediateActions.of(PossibleAction.of(TradeWithIndians.class));
            }
        } else if (worker == Worker.CRAFTSMAN) {
            if (count == 4 || count == 6) {
                return ImmediateActions.of(PossibleAction.of(PlaceCheapBuilding.class));
            }
        } else {
            if (count == 2) {
                return ImmediateActions.of(PossibleAction.of(DiscardOneJerseyToGainCertificate.class));
            } else if (count == 3) {
                return ImmediateActions.of(PossibleAction.of(DiscardOneJerseyToGainTwoDollars.class));
            } else if (count == 4) {
                return ImmediateActions.of(PossibleAction.of(HireCheapWorker.class));
            } else if (count == 5) {
                return ImmediateActions.of(PossibleAction.of(DiscardOneJerseyToGainTwoCertificates.class));
            } else if (count == 6) {
                return ImmediateActions.of(PossibleAction.of(DiscardOneJerseyToGainFourDollars.class));
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

    public void removeBuilding(PlayerBuilding building) {
        if (!buildings.remove(building)) {
            throw new IllegalStateException("Building not available for player");
        }
    }

    public int getNumberOfEngineers() {
        return workers.get(Worker.ENGINEER);
    }

    public void discardCard(Card card) {
        // TODO
    }

    public void drawCard() {
        // TODO
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

    public ImmediateActions playObjectiveCard(ObjectiveCard objectiveCard) {
        if (!hand.remove(objectiveCard)) {
            throw new IllegalStateException("Objective card not in hand");
        }
        objectives.add(objectiveCard);
        return ImmediateActions.of(objectiveCard.getPossibleAction());
    }

    public boolean canPlayObjectiveCard() {
        return hand.stream().anyMatch(card -> card instanceof ObjectiveCard);
    }

    public class RemoveHazardForFree extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }

    public class PlaceCheapBuilding extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }

    public class DiscardOneJerseyToGainCertificate extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }

    public class DiscardOneJerseyToGainTwoDollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }

    public class HireCheapWorker extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }

    public class DiscardOneJerseyToGainTwoCertificates extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }

    public class DiscardOneJerseyToGainFourDollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            // TODO
            return null;
        }
    }
}
