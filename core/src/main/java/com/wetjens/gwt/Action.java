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

            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2);
        }
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

    public static class Draw3CardsThenDiscard3Cards extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().drawCard();
            game.currentPlayerState().drawCard();
            game.currentPlayerState().drawCard();
            return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.Discard3Cards.class));
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
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, game.currentPlayerState().getNumberOfEngineers());
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
    }

    @Value
    public static class RemoveHazardForFree extends Action {
        Hazard hazard;

        @Override
        public ImmediateActions perform(Game game) {
            game.getTrail().removeHazard(hazard);
            game.currentPlayerState().addHazard(hazard);
            return ImmediateActions.none();
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

        @Override
        public ImmediateActions perform(Game game) {
            int breedingValue = game.currentPlayerState().handValue() + certificates;

            if (breedingValue < city.getValue()) {
                throw new IllegalArgumentException("Not enough hand value for city");
            }

            game.currentPlayerState().spendCertificates(certificates);

            game.getRailroadTrack().deliverToCity(game.getCurrentPlayer(), city);

            int reward = breedingValue - city.getSignals() + game.getRailroadTrack().signalsPassed(game.getCurrentPlayer());

            game.currentPlayerState().gainDollars(reward);
            game.currentPlayerState().discardAllCards();
            game.currentPlayerState().drawUpToHandLimit();

            // TODO Immediate actions from delivering to a city
            if (city == City.SAN_FRANCISCO) {
                return ImmediateActions.of(PossibleAction.optional(GainObjectiveCard.class));
            }
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class MoveEngineAtLeast1BackwardsAndGain3Dollars extends Action {
        RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game) {
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE);

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
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2);
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
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 2, 3);
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
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1);
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
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1);
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
                    .andThen(PossibleAction.mandatory(Remove1Card.class));
        }
    }

    public static final class DiscardPairToGain4Dollars extends Action {

        private final CattleType type;

        public DiscardPairToGain4Dollars(CattleType type) {
            this.type = type;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().discardCattleCards(type, 2);
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @Value
    public static final class RemoveHazard extends Action {
        Hazard hazard;
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(7);
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
        public boolean canPlayAnyTime() {
            return true;
        }

        @Override
        public ImmediateActions perform(Game game) {
            return game.currentPlayerState().playObjectiveCard(objectiveCard);
        }
    }

    public static final class Move extends Action {

        private final List<Location> steps;

        public Move(List<Location> steps) {
            this.steps = new ArrayList<>(steps);
        }

        @Override
        public ImmediateActions perform(Game game) {
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("Must take at least one step");
            }

            PlayerState currentPlayerState = game.currentPlayerState();
            if (steps.size() > currentPlayerState.getStepLimit()) {
                throw new IllegalArgumentException("Number of steps exceeds player step limit");
            }

            if (game.getTrail().isAtLocation(game.getCurrentPlayer())) {
                Location from = game.getTrail().getCurrentLocation(game.getCurrentPlayer());

                checkDirectAndConsecutiveSteps(from, steps);

                payFees(game);
            }

            Location to = steps.get(steps.size() - 1);

            game.getTrail().movePlayer(game.getCurrentPlayer(), to);

            return to.getPossibleAction()
                    .map(ImmediateActions::of)
                    .orElse(ImmediateActions.none());
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
}
