package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

public abstract class Action {

    public abstract ImmediateActions perform(Game game);

    /**
     * Indicates whether this action can be played at any time before, between or after other actions in a players turn.
     *
     * @return <code>true</code> if action can be played at any time, <code>false</code> otherwise.
     */
    public boolean canPlayAnyTime() {
        return false;
    }

    public static final class BuyCattle extends Action {

        private final Set<Card.CattleCard> cattleCards;

        public BuyCattle(Set<Card.CattleCard> cattleCards) {
            this.cattleCards = cattleCards;
        }

        @Override
        public ImmediateActions perform(Game game) {
            int cost = game.getCattleMarket().cost(cattleCards, game.currentPlayerState().getNumberOfCowboys());

            game.currentPlayerState().payDollars(cost);

            return game.getCattleMarket().buy(cattleCards, game.currentPlayerState().getNumberOfCowboys());
        }
    }

    public static class SingleAuxiliaryAction extends Action {

        public ImmediateActions perform(Game game) {
            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(game.currentPlayerState().unlockedSingleAuxiliaryActions())));
        }
    }

    public static final class Gain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static final class SingleOrDoubleAuxiliaryAction extends Action {

        @Override
        public ImmediateActions perform(Game game) {
            PlayerState playerState = game.currentPlayerState();

            Set<Class<? extends Action>> actions = new HashSet<>(playerState.unlockedSingleAuxiliaryActions());
            actions.addAll(playerState.unlockedDoubleAuxiliaryActions());

            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(actions)));
        }
    }

    public static final class Pay2DollarsToMoveEngine2Forward extends Action {
        private final RailroadTrack.Space to;

        public Pay2DollarsToMoveEngine2Forward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(2);

            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2).getImmediateActions();
        }
    }

    @Value
    @NonFinal
    public static class DrawCardsThenDiscardCards extends Action {

        int atLeast;
        int atMost;
        int amount;

        DrawCardsThenDiscardCards(int atLeast, int atMost, int amount) {
            this.atLeast = atLeast;
            this.atMost = atMost;
            this.amount = amount;
        }

        @Override
        public ImmediateActions perform(Game game) {
            if (amount < atLeast || amount > atMost) {
                throw new IllegalArgumentException("Amount must be " + atLeast + ".." + atMost);
            }

            for (int i = 0; i < amount; i++) {
                game.currentPlayerState().drawCard();
            }

            return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.exactly(amount)));
        }

        public static Class<? extends Action> exactly(int amount) {
            return amount == 2 ? Draw1CardThenDiscard1Card.class :
                    Draw2CardsThenDiscard2Cards.class;
        }

        public static final class Draw1CardThenDiscard1Card extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().drawCard();
                return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.Discard1Card.class));
            }
        }

        public static final class Draw2CardsThenDiscard2Cards extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().drawCard();
                game.currentPlayerState().drawCard();
                return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.Discard2Cards.class));
            }
        }

        public static Class<? extends Action> upTo(int amount) {
            return amount == 6 ? DrawUpTo6CardsThenDiscardCards.class :
                    amount == 5 ? DrawUpTo5CardsThenDiscardCards.class :
                            amount == 4 ? DrawUpTo4CardsThenDiscardCards.class :
                                    amount == 3 ? DrawUpTo3CardsThenDiscardCards.class :
                                            amount == 2 ? DrawUpTo2CardsThenDiscardCards.class :
                                                    DrawUpTo1CardsThenDiscardCards.class;
        }

        @Value
        public static final class DrawUpTo1CardsThenDiscardCards extends DrawCardsThenDiscardCards {
            public DrawUpTo1CardsThenDiscardCards(int amount) {
                super(1, 1, amount);
            }
        }

        @Value
        public static final class DrawUpTo2CardsThenDiscardCards extends DrawCardsThenDiscardCards {
            public DrawUpTo2CardsThenDiscardCards(int amount) {
                super(1, 2, amount);
            }
        }

        @Value
        public static final class DrawUpTo3CardsThenDiscardCards extends DrawCardsThenDiscardCards {
            public DrawUpTo3CardsThenDiscardCards(int amount) {
                super(1, 3, amount);
            }
        }

        @Value
        public static final class DrawUpTo4CardsThenDiscardCards extends DrawCardsThenDiscardCards {
            public DrawUpTo4CardsThenDiscardCards(int amount) {
                super(1, 4, amount);
            }
        }

        @Value
        public static final class DrawUpTo5CardsThenDiscardCards extends DrawCardsThenDiscardCards {
            public DrawUpTo5CardsThenDiscardCards(int amount) {
                super(1, 5, amount);
            }
        }

        @Value
        public static final class DrawUpTo6CardsThenDiscardCards extends DrawCardsThenDiscardCards {
            public DrawUpTo6CardsThenDiscardCards(int amount) {
                super(1, 6, amount);
            }
        }
    }

    public static final class GainObjectiveCard extends Action {

        @Override
        public ImmediateActions perform(Game game) {
            ObjectiveCard objectiveCard = game.takeObjectiveCard();

            game.currentPlayerState().gainCard(objectiveCard);

            return ImmediateActions.none();
        }
    }

    public static class MoveEngineForward extends Action {

        private final RailroadTrack.Space to;

        public MoveEngineForward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, game.currentPlayerState().getNumberOfEngineers()).getImmediateActions();
        }
    }

    @AllArgsConstructor
    public static class Remove1Card extends Action {
        @NonNull
        Card card;

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().removeCards(Collections.singleton(card));
            return ImmediateActions.none();
        }
    }

    public static class Remove2Cards extends Action {
        Set<Card> cards;

        public Remove2Cards(@NonNull Set<Card> cards) {
            if (cards.size() != 2) {
                throw new IllegalArgumentException("Must specify 2 cards");
            }
            this.cards = new HashSet<>(cards);
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().removeCards(cards);
            return ImmediateActions.none();
        }
    }

    @AllArgsConstructor
    public static class TradeWithIndians extends Action {
        int cost;

        @Override
        public ImmediateActions perform(Game game) {
            Location.TeepeeLocation teepeeLocation = game.getTrail().getTeepeeLocation(cost);
            Teepee teepee = teepeeLocation.getTeepee()
                    .orElseThrow(() -> new IllegalStateException("No teepee at location"));

            if (teepeeLocation.getReward() > 0) {
                teepeeLocation.removeTeepee();
                game.currentPlayerState().addTeepee(teepee);
                game.currentPlayerState().gainDollars(teepeeLocation.getReward());
            } else {
                game.currentPlayerState().payDollars(teepeeLocation.getReward());
                teepeeLocation.removeTeepee();
                game.currentPlayerState().addTeepee(teepee);
            }

            return ImmediateActions.none();
        }
    }

    public abstract static class DiscardCards extends Action {

        private final int expected;
        private final Set<Card> cards;

        public DiscardCards(int expected, Set<Card> cards) {
            this.expected = expected;
            this.cards = cards;
        }

        @Override
        public ImmediateActions perform(Game game) {
            if (cards.size() != expected) {
                throw new IllegalStateException("Must discard " + expected + " cards");
            }

            cards.forEach(game.currentPlayerState()::discardCard);

            return ImmediateActions.none();
        }

        public static Class<? extends Action> exactly(int amount) {
            return amount == 6 ? DiscardCards.Discard6Cards.class :
                    amount == 5 ? DiscardCards.Discard5Cards.class :
                            amount == 4 ? DiscardCards.Discard4Cards.class :
                                    amount == 3 ? DiscardCards.Discard3Cards.class :
                                            amount == 2 ? DiscardCards.Discard2Cards.class :
                                                    DiscardCards.Discard1Card.class;
        }

        public static class Discard1Card extends DiscardCards {
            public Discard1Card(Card card) {
                super(1, Collections.singleton(card));
            }
        }

        public static class Discard2Cards extends DiscardCards {
            public Discard2Cards(Set<Card> cards) {
                super(2, cards);
            }
        }

        public static class Discard3Cards extends DiscardCards {
            public Discard3Cards(Set<Card> cards) {
                super(3, cards);
            }
        }

        public static class Discard4Cards extends DiscardCards {
            public Discard4Cards(Set<Card> cards) {
                super(4, cards);
            }
        }

        public static class Discard5Cards extends DiscardCards {
            public Discard5Cards(Set<Card> cards) {
                super(5, cards);
            }
        }

        public static class Discard6Cards extends DiscardCards {
            public Discard6Cards(Set<Card> cards) {
                super(6, cards);
            }
        }
    }

    public static class RemoveHazardForFree extends RemoveHazard {
        public RemoveHazardForFree(Hazard hazard) {
            super(hazard, 0);
        }
    }

    public static final class Discard1Guernsey extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            PlayerState playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.GUERNSEY, 1);
            playerState.gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static class HireWorker extends Action {

        private final Worker worker;
        private final int modifier;

        public HireWorker(Worker worker) {
            this(worker, 0);
        }

        protected HireWorker(Worker worker, int modifier) {
            this.worker = worker;
            this.modifier = modifier;
        }

        @Override
        public ImmediateActions perform(Game game) {
            int cost = game.getJobMarket().cost(worker) + modifier;

            game.currentPlayerState().payDollars(cost);

            game.getJobMarket().takeWorker(worker);

            return game.currentPlayerState().gainWorker(worker);
        }
    }

    public static final class HireSecondWorker extends HireWorker {
        public HireSecondWorker(Worker worker) {
            super(worker, 2);
        }
    }

    public static final class PlaceCheapBuilding extends PlaceBuilding {
        public PlaceCheapBuilding(Location.BuildingLocation location, PlayerBuilding building) {
            super(location, building, 1);
        }
    }

    public static final class Discard1JerseyToGain1Certificate extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(1);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static final class HireCheapWorker extends HireWorker {
        public HireCheapWorker(Worker worker) {
            super(worker, -1);
        }
    }

    public static final class Discard1JerseyToGain2Certificates extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(2);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain4Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(4);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1DutchBeltToGain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            PlayerState playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.DUTCH_BELT, 1);
            playerState.gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static class PlaceBuilding extends Action {
        private final int costPerCraftsman;
        private final Location.BuildingLocation location;
        private final PlayerBuilding building;

        public PlaceBuilding(Location.BuildingLocation location, PlayerBuilding building) {
            this(location, building, 2);
        }

        PlaceBuilding(Location.BuildingLocation location, PlayerBuilding building, int costPerCraftsman) {
            this.location = location;
            this.building = building;
            this.costPerCraftsman = costPerCraftsman;
        }

        @Override
        public ImmediateActions perform(Game game) {
            if (!game.currentPlayerState().hasAvailable(building)) {
                throw new IllegalStateException("Building not available for player");
            }

            int craftsmenNeeded = craftsmenNeeded();

            if (craftsmenNeeded > game.currentPlayerState().getNumberOfCraftsmen()) {
                throw new IllegalStateException("Not enough craftsmen");
            }

            int cost = craftsmenNeeded * costPerCraftsman;
            game.currentPlayerState().payDollars(cost);
            game.currentPlayerState().removeBuilding(building);

            location.placeBuilding(building);

            return ImmediateActions.none();
        }

        private int craftsmenNeeded() {
            return existingBuildingToReplace()
                    // If replacing an existing building, only the difference is needed
                    .filter(existingBuilding -> {
                        if (existingBuilding.getCraftsmen() > building.getCraftsmen()) {
                            throw new IllegalStateException("Replacement building must be higher valued that existing building");
                        }
                        return true;
                    })
                    .map(existingBuilding -> building.getCraftsmen() - existingBuilding.getCraftsmen())
                    .orElse(building.getCraftsmen());
        }

        private Optional<PlayerBuilding> existingBuildingToReplace() {
            return location.getBuilding()
                    .filter(existingBuilding -> {
                        if (!(existingBuilding instanceof PlayerBuilding)) {
                            throw new IllegalStateException("Can only replace a player building");
                        }
                        return true;
                    })
                    .map(existingBuilding -> (PlayerBuilding) existingBuilding)
                    .filter(existingBuilding -> {
                        if (existingBuilding.getPlayer() != building.getPlayer()) {
                            throw new IllegalStateException("Can only replace building of same player");
                        }
                        return true;
                    });
        }
    }

    @Value
    public static final class UpgradeStation extends Action {

        @Override
        public ImmediateActions perform(@NonNull Game game) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new IllegalStateException("Not at station"));

            return station.upgrade(game);
        }
    }

    @AllArgsConstructor
    public static final class AppointStationMaster extends Action {
        Worker worker;

        @Override
        public ImmediateActions perform(@NonNull Game game) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new IllegalStateException("Not at station"));

            return station.appointStationMaster(game, worker);
        }
    }

    public static final class ChooseForesights extends Action {

        private final List<Choice> choices;

        public ChooseForesights(@NonNull List<Choice> choices) {
            if (choices.size() != 3) {
                throw new IllegalArgumentException("Must have 3 choices");
            }
            this.choices = new ArrayList<>(choices);
        }

        @Override
        public ImmediateActions perform(Game game) {
            for (int columnIndex = 0; columnIndex < choices.size(); columnIndex++) {
                Choice choice = choices.get(columnIndex);

                KansasCitySupply.Tile tile = game.getForesights().take(columnIndex, choice.getRowIndex());

                if (tile.getWorker() != null) {
                    boolean fillUpCattleMarket = game.getJobMarket().addWorker(tile.getWorker());
                    if (fillUpCattleMarket) {
                        game.getCattleMarket().fillUp();
                    }
                } else if (tile.getHazard() != null) {
                    if (!(choice.getLocation() instanceof Location.HazardLocation)) {
                        throw new IllegalArgumentException("Must pick a hazard location");
                    }
                    ((Location.HazardLocation) choice.getLocation()).placeHazard(tile.getHazard());
                } else {
                    if (!(choice.getLocation() instanceof Location.TeepeeLocation)) {
                        throw new IllegalArgumentException("Must pick a teepee location");
                    }
                    ((Location.TeepeeLocation) choice.getLocation()).placeTeepee(tile.getTeepee());
                }
            }

            return ImmediateActions.none();
        }

        @Value
        public static final class Choice {
            private final int rowIndex;
            private final Location location;
        }
    }

    @Value
    public static final class DeliverToCity extends Action {

        City city;
        int certificates;
        Unlockable unlockable;

        @Override
        public ImmediateActions perform(Game game) {
            int breedingValue = game.currentPlayerState().handValue() + certificates;

            if (breedingValue < city.getValue()) {
                throw new IllegalArgumentException("Not enough hand value for city");
            }

            game.currentPlayerState().spendCertificates(certificates);
            game.currentPlayerState().gainDollars(breedingValue);
            game.currentPlayerState().discardAllCards();

            game.currentPlayerState().unlock(unlockable);

            int transportCosts = city.getSignals() + game.getRailroadTrack().signalsPassed(game.getCurrentPlayer());

            if (transportCosts > 0) {
                game.currentPlayerState().payDollars(transportCosts);
            }

            return game.getRailroadTrack().deliverToCity(game.getCurrentPlayer(), city);
        }
    }

    @Value
    public static final class MoveEngineAtLeast1BackwardsAndGain3Dollars extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE).getImmediateActions();

            game.currentPlayerState().gainDollars(3);

            return immediateActions;
        }
    }

    public static final class Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates extends Action {

        private final RailroadTrack.Space to;

        public Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(2);
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2).getImmediateActions();
            // TODO Check if gaining certificates AFTER possible immediate actions from railroad track is OK
            game.currentPlayerState().gainCertificates(2);
            return immediateActions;
        }
    }

    public static final class MoveEngine2BackwardsToRemove2Cards extends Action {

        private final RailroadTrack.Space to;

        public MoveEngine2BackwardsToRemove2Cards(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2)
                    .getImmediateActions()
                    .andThen(PossibleAction.mandatory(Remove2Cards.class));
        }
    }

    public static final class MoveEngine2Or3Forward extends Action {
        private final RailroadTrack.Space to;

        public MoveEngine2Or3Forward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 2, 3).getImmediateActions();
        }
    }

    public static final class Gain1Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainDollars(1);
            return ImmediateActions.none();
        }
    }

    public static final class Pay1DollarAndMoveEngine1BackwardsToGain1Certificate extends Action {
        private final RailroadTrack.Space to;

        public Pay1DollarAndMoveEngine1BackwardsToGain1Certificate(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(1);
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
            // TODO Check if gaining certificate AFTER possible immediate actions from railroad track is OK
            game.currentPlayerState().gainCertificates(1);
            return immediateActions;
        }
    }

    public static final class Pay1DollarToMoveEngine1Forward extends Action {
        private final RailroadTrack.Space to;

        public Pay1DollarToMoveEngine1Forward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(1);
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
        }
    }

    public static final class MoveEngine1BackwardsToRemove1Card extends Action {
        private final RailroadTrack.Space to;

        public MoveEngine1BackwardsToRemove1Card(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1)
                    .getImmediateActions()
                    .andThen(PossibleAction.mandatory(Remove1Card.class));
        }
    }

    @AllArgsConstructor
    public static final class DiscardPairToGain4Dollars extends Action {

        private final CattleType type;

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(type, 2);
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @Value
    @NonFinal
    public static class RemoveHazard extends Action {
        Hazard hazard;
        int cost;

        public RemoveHazard(Hazard hazard) {
            this(hazard, 7);
        }

        RemoveHazard(Hazard hazard, int cost) {
            this.hazard = hazard;
            this.cost = cost;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(cost);
            game.getTrail().removeHazard(hazard);
            return ImmediateActions.none();
        }
    }

    public static class PlayObjectiveCard extends Action {

        private final ObjectiveCard objectiveCard;

        public PlayObjectiveCard(ObjectiveCard objectiveCard) {
            this.objectiveCard = objectiveCard;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.currentPlayerState().playObjectiveCard(objectiveCard);
        }

        @Override
        public boolean canPlayAnyTime() {
            return true;
        }
    }

    public static class Move extends Action {

        private final List<Location> steps;
        private final int atLeast;
        private final int atMost;

        public Move(List<Location> steps) {
            this(steps, 1, Integer.MAX_VALUE);
        }

        Move(List<Location> steps, int atLeast, int atMost) {
            this.steps = new ArrayList<>(steps);
            this.atLeast = atLeast;
            this.atMost = atMost;
        }

        @Override
        public ImmediateActions perform(Game game) {
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("Must take at least one step");
            }

            PlayerState currentPlayerState = game.currentPlayerState();

            if (steps.size() > Math.min(atMost, currentPlayerState.getStepLimit())) {
                throw new IllegalArgumentException("Number of steps exceeds limit");
            }

            if (steps.size() < atLeast) {
                throw new IllegalArgumentException("Number of steps below minimum");
            }

            Player player = game.getCurrentPlayer();

            if (game.getTrail().isAtLocation(player)) {
                Location from = game.getTrail().getCurrentLocation(player);

                checkDirectAndConsecutiveSteps(from, steps);

                payFees(game);
            }

            Location to = steps.get(steps.size() - 1);

            game.getTrail().movePlayer(player, to);

            return to.activate(game, player);
        }

        private void checkDirectAndConsecutiveSteps(Location from, List<Location> steps) {
            Location to = steps.get(0);
            if (!from.isDirect(to)) {
                throw new IllegalArgumentException("Cannot step from " + from + " to " + to);
            }
            if (steps.size() > 1) {
                checkDirectAndConsecutiveSteps(to, steps.subList(1, steps.size()));
            }
        }

        private void payFees(Game game) {
            for (Location location : steps) {
                payFee(game, location);
            }
        }

        private void payFee(Game game, Location location) {
            PlayerState currentPlayerState = game.currentPlayerState();

            // Can never pay more than player has
            int amount = Math.min(currentPlayerState.getBalance(),
                    location.getHand().getFee(game.getPlayers().size()));

            currentPlayerState.payDollars(amount);

            feeRecipient(location)
                    .map(game::playerState)
                    .ifPresent(recipient -> recipient.gainDollars(amount));
        }

        private Optional<Player> feeRecipient(Location location) {
            Optional<Player> recipient = Optional.empty();
            if (location instanceof Location.BuildingLocation) {
                recipient = ((Location.BuildingLocation) location).getBuilding()
                        .filter(building -> building instanceof PlayerBuilding)
                        .map(building -> ((PlayerBuilding) building).getPlayer());
            }
            return recipient;
        }
    }

    public static final class Discard1BlackAngusToGain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.BLACK_ANGUS, 1);
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static class GainCertificate extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainCertificates(1);
            return ImmediateActions.none();
        }
    }

    public static final class Draw2CattleCards extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.getCattleMarket().draw();
            game.getCattleMarket().draw();

            return ImmediateActions.none();
        }
    }

    public static final class Discard2GuernseyToGain4Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.GUERNSEY, 2);
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @AllArgsConstructor
    public static final class DiscardPairToGain3Dollars extends Action {

        private final CattleType type;

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(type, 2);
            game.currentPlayerState().gainDollars(3);
            return ImmediateActions.none();
        }
    }

    public static final class Move1Forward extends Move {
        public Move1Forward(Location to) {
            super(Collections.singletonList(to), 1, 1);
        }
    }

    public static final class Move2Forward extends Move {
        public Move2Forward(Location to) {
            super(Collections.singletonList(to), 1, 2);
        }
    }

    public static final class Move3Forward extends Move {
        public Move3Forward(Location to) {
            super(Collections.singletonList(to), 1, 3);
        }
    }

    public static final class Move4Forward extends Move {
        public Move4Forward(Location to) {
            super(Collections.singletonList(to), 1, 4);
        }
    }

    public static final class RemoveHazardFor5Dollars extends RemoveHazard {
        public RemoveHazardFor5Dollars(Hazard hazard) {
            super(hazard, 5);
        }
    }

    public static final class Discard1HolsteinToGain10Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.HOLSTEIN, 1);
            game.currentPlayerState().gainDollars(10);
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class MoveEngineAtMost2Forward extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2).getImmediateActions();
        }
    }

    @Value
    public static final class MoveEngineAtMost3Forward extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 3).getImmediateActions();
        }
    }

    public static final class ExtraordinaryDelivery extends Action {
        RailroadTrack.Space to;
        City city;
        Unlockable unlock;

        @Override
        public ImmediateActions perform(Game game) {
            RailroadTrack.Space from = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());

            RailroadTrack.EngineMove engineMove = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE);

            if (city.getValue() > engineMove.getSteps()) {
                throw new IllegalArgumentException("City value must be <= spaces that engine moved backwards");
            }

            game.currentPlayerState().unlock(unlock);

            return game.getRailroadTrack().deliverToCity(game.getCurrentPlayer(), city)
                    .andThen(engineMove.getImmediateActions());
        }
    }

    public static final class MaxCertificates extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainMaxCertificates();
            return ImmediateActions.none();
        }
    }

    public static final class Gain1DollarPerBuildingInWoods extends Action {

        @Override
        public ImmediateActions perform(Game game) {
            int buildingsInWoods = game.getTrail().buildingsInWoods(game.getCurrentPlayer());
            game.currentPlayerState().gainDollars(buildingsInWoods * 2);
            return ImmediateActions.none();
        }
    }

    public static final class Gain2CertificatesAnd2DollarsPerTeepeePair extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            List<Teepee> teepees = game.currentPlayerState().getTeepees();

            int blueTeepees = (int) teepees.stream()
                    .filter(teepee -> teepee == Teepee.BLUE)
                    .count();
            int greenTeepees = teepees.size() - blueTeepees;

            int pairs = Math.max(blueTeepees, greenTeepees) / Math.min(blueTeepees, greenTeepees);

            game.currentPlayerState().gainCertificates(pairs * 2);
            game.currentPlayerState().gainDollars(pairs * 2);

            return ImmediateActions.none();
        }
    }

    @Value
    public class MoveEngineAtMost5Forward extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 5).getImmediateActions();
        }
    }

    @Value
    public static final class Discard1ObjectiveCardToGain2Certificates extends Action {
        ObjectiveCard card;

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCard(card);
            game.currentPlayerState().gainCertificates(2);
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class MoveEngine1BackwardsToGain3Dollars extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
            game.currentPlayerState().gainDollars(3);
            return immediateActions;
        }
    }

    @Value
    public static final class Discard1JerseyToMoveEngine1Forward extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 1).getImmediateActions();
        }
    }

    public static final class Discard1DutchBeltToGain3Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.DUTCH_BELT, 1);
            game.currentPlayerState().gainDollars(3);
            return ImmediateActions.none();
        }
    }

    public static final class DrawCardsUpToNumberOfCowboysThenDiscardCards extends Action {

        @Override
        public ImmediateActions perform(Game game) {
            int numberOfCowboys = game.currentPlayerState().getNumberOfCowboys();
            return ImmediateActions.of(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(numberOfCowboys)));
        }
    }

    public static final class Discard1BlackAngusToGain2Certificates extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(CattleType.BLACK_ANGUS, 1);
            game.currentPlayerState().gainCertificates(2);
            return ImmediateActions.none();
        }
    }

    public static final class Gain1DollarPerEngineer extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainDollars(game.currentPlayerState().getNumberOfEngineers());
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand extends Action {
        Card.CattleCard card;

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCard(card);
            game.currentPlayerState().gainDollars(3);
            game.currentPlayerState().addCardToHand(game.takeObjectiveCard());
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class MoveEngineForwardUpToNumberOfBuildingsInWoods extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            int buildingsInWoods = game.getTrail().buildingsInWoods(game.getCurrentPlayer());
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, buildingsInWoods).getImmediateActions();
        }
    }

    @Value
    public static final class UseAdjacentBuilding extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            Location currentLocation = game.getTrail().getCurrentLocation(game.getCurrentPlayer());

            Set<Location> adjacentLocations = game.getTrail().getAdjacentLocations(currentLocation);

            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(adjacentLocations.stream()
                    .filter(adjacentLocation -> adjacentLocation instanceof Location.BuildingLocation)
                    .map(adjacentLocation -> (Location.BuildingLocation) adjacentLocation)
                    .flatMap(adjacentBuildingLocation -> adjacentBuildingLocation.getBuilding().stream())
                    .map(Building::getPossibleAction))));
        }
    }

    @Value
    public static final class UpgradeAnyStationBehindEngine extends Action {
        Station station;
        @Override
        public ImmediateActions perform(Game game) {
            RailroadTrack.Space stationSpace = game.getRailroadTrack().getSpace(station);
            RailroadTrack.Space currentSpace = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());

            if (!stationSpace.isBefore(currentSpace)) {
                throw new IllegalArgumentException("Station must be behind engine");
            }

            return station.upgrade(game);
        }
    }

    @Value
    public static final class Gain4Dollars extends Action{
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class MoveEngineAtMost4Forward extends Action{
        RailroadTrack.Space to;
        @Override
        public ImmediateActions perform(Game game) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 4).getImmediateActions();
        }
    }

}
