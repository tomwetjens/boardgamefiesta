package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Action {

    abstract ImmediateActions perform(Game game, Random random);

    /**
     * Indicates whether this action can be played at any time before, between or after other actions in a players turn.
     *
     * @return <code>true</code> if action can be played at any time, <code>false</code> otherwise.
     */
    public boolean canPlayAnyTime() {
        return false;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class BuyCattle extends Action {

        @NonNull Set<Card.CattleCard> cattleCards;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            if (cattleCards.isEmpty()) {
                throw new IllegalArgumentException("Must specify cattle cards");
            }

            int cost = game.getCattleMarket().cost(cattleCards, game.currentPlayerState().getNumberOfCowboys());

            game.currentPlayerState().payDollars(cost);

            ImmediateActions immediateActions = game.getCattleMarket().buy(cattleCards, game.currentPlayerState().getNumberOfCowboys());

            game.currentPlayerState().gainCards(cattleCards);

            return immediateActions;
        }
    }

    public static final class SingleAuxiliaryAction extends Action {

        public ImmediateActions perform(Game game, Random random) {
            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(game.currentPlayerState().unlockedSingleAuxiliaryActions())));
        }
    }

    public static final class Gain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static final class SingleOrDoubleAuxiliaryAction extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            PlayerState playerState = game.currentPlayerState();
            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(playerState.unlockedSingleOrDoubleAuxiliaryActions())));
        }
    }

    public static final class Pay2DollarsToMoveEngine2Forward extends Action {
        private final RailroadTrack.Space to;

        public Pay2DollarsToMoveEngine2Forward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().payDollars(2);

            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2).getImmediateActions();
        }
    }

    public static final class DrawCard extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().drawCard(random);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class TakeObjectiveCard extends Action {
        @NonNull ObjectiveCard objectiveCard;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.getObjectiveCards().remove(objectiveCard);
            game.currentPlayerState().gainCard(objectiveCard);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineForward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, game.currentPlayerState().getNumberOfEngineers()).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Remove1Card extends Action {
        @NonNull Card card;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().removeCards(Collections.singleton(card));
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Remove2Cards extends Action {
        @NonNull Set<Card> cards;

        public Remove2Cards(@NonNull Set<Card> cards) {
            if (cards.size() != 2) {
                throw new IllegalArgumentException("Must specify 2 cards");
            }
            this.cards = new HashSet<>(cards);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().removeCards(cards);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class TradeWithIndians extends Action {
        int reward;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            Location.TeepeeLocation teepeeLocation = game.getTrail().getTeepeeLocation(reward);
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

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class DiscardCard extends Action {
        @NonNull Card card;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCard(card);
            return ImmediateActions.none();
        }
    }

    public static final class RemoveHazardForFree extends RemoveHazard {
        public RemoveHazardForFree(Hazard hazard) {
            super(hazard, 0);
        }
    }

    public static final class Discard1Guernsey extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            PlayerState playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.GUERNSEY, 1);
            playerState.gainDollars(2);
            return ImmediateActions.none();
        }
    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class HireWorker extends Action {

        @NonNull Worker worker;
        int modifier;

        public HireWorker(Worker worker) {
            this(worker, 0);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
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
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(1);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
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
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(2);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain4Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(4);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1DutchBeltToGain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            PlayerState playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.DUTCH_BELT, 1);
            playerState.gainDollars(2);
            return ImmediateActions.none();
        }
    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class PlaceBuilding extends Action {
        @NonNull Location.BuildingLocation location;
        @NonNull PlayerBuilding building;
        int costPerCraftsman;

        public PlaceBuilding(Location.BuildingLocation location, PlayerBuilding building) {
            this(location, building, 2);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
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
        public ImmediateActions perform(@NonNull Game game, Random random) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new IllegalStateException("Not at station"));

            return station.upgrade(game);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class DowngradeStation extends Action {
        @NonNull Station station;

        @Override
        ImmediateActions perform(Game game, Random random) {
            return station.downgrade(game);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class AppointStationMaster extends Action {
        @NonNull Worker worker;

        @Override
        public ImmediateActions perform(@NonNull Game game, Random random) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new IllegalStateException("Not at station"));

            return station.appointStationMaster(game, worker);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class ChooseForesights extends Action {
        @NonNull List<Integer> choices;

        public ChooseForesights(@NonNull List<Integer> choices) {
            if (choices.size() != 3) {
                throw new IllegalArgumentException("Must have 3 choices");
            }
            this.choices = new ArrayList<>(choices);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            for (int columnIndex = 0; columnIndex < choices.size(); columnIndex++) {
                int rowIndex = choices.get(columnIndex);

                KansasCitySupply.Tile tile = game.getForesights().take(columnIndex, rowIndex);

                if (tile.getWorker() != null) {
                    JobMarket jobMarket = game.getJobMarket();

                    if (!jobMarket.isClosed()) {
                        jobMarket.addWorker(tile.getWorker());

                        if (game.getJobMarket().fillUpCattleMarket()) {
                            game.getCattleMarket().fillUp();
                        }

                        if (jobMarket.isClosed()) {
                            game.currentPlayerState().gainJobMarketToken();
                        }
                    }
                } else if (tile.getHazard() != null) {
                    game.getTrail().placeHazard(tile.getHazard());
                } else {
                    game.getTrail().placeTeepee(tile.getTeepee());
                }
            }

            return ImmediateActions.of(PossibleAction.mandatory(DeliverToCity.class));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class DeliverToCity extends Action {
        @NonNull City city;
        int certificates;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            int breedingValue = game.currentPlayerState().handValue() + certificates;

            if (breedingValue < city.getValue()) {
                throw new IllegalArgumentException("Not enough hand value for city");
            }

            game.currentPlayerState().spendCertificates(certificates);
            game.currentPlayerState().gainDollars(breedingValue);
            game.currentPlayerState().discardAllCards();

            int transportCosts = city.getSignals() + game.getRailroadTrack().signalsPassed(game.getCurrentPlayer());

            if (transportCosts > 0) {
                game.currentPlayerState().payDollars(transportCosts);
            }

            ImmediateActions immediateActions = game.deliverToCity(city);

            if (city == City.KANSAS_CITY) {
                game.currentPlayerState().gainDollars(6);
            }

            game.getTrail().moveToStart(game.getCurrentPlayer());

            return immediateActions;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class UnlockWhite extends Action {
        @NonNull Unlockable unlock;

        @Override
        ImmediateActions perform(Game game, Random random) {
            if (unlock.getDiscColor() != DiscColor.WHITE) {
                throw new IllegalArgumentException("Must pick a white disc");
            }
            game.currentPlayerState().unlock(unlock);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class UnlockBlackOrWhite extends Action {
        @NonNull Unlockable unlock;

        @Override
        ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().unlock(unlock);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineAtLeast1BackwardsAndGain3Dollars extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE).getImmediateActions();

            game.currentPlayerState().gainDollars(3);

            return immediateActions;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().payDollars(2);
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2).getImmediateActions();
            game.currentPlayerState().gainCertificates(2);
            return immediateActions;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngine2BackwardsToRemove2Cards extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2)
                    .getImmediateActions()
                    .andThen(PossibleAction.mandatory(Remove2Cards.class));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngine2Or3Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 2, 3).getImmediateActions();
        }
    }

    public static final class Gain1Dollar extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(1);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Pay1DollarAndMoveEngine1BackwardsToGain1Certificate extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().payDollars(1);
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
            game.currentPlayerState().gainCertificates(1);
            return immediateActions;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Pay1DollarToMoveEngine1Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().payDollars(1);
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngine1BackwardsToRemove1Card extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1)
                    .getImmediateActions()
                    .andThen(PossibleAction.mandatory(Remove1Card.class));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class DiscardPairToGain4Dollars extends Action {
        @NonNull CattleType type;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(type, 2);
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class RemoveHazard extends Action {
        @NonNull Hazard hazard;
        int cost;

        public RemoveHazard(Hazard hazard) {
            this(hazard, 7);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().payDollars(cost);
            game.getTrail().removeHazard(hazard);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class PlayObjectiveCard extends Action {
        @NonNull ObjectiveCard objectiveCard;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.currentPlayerState().playObjectiveCard(objectiveCard);
        }

        @Override
        public boolean canPlayAnyTime() {
            return true;
        }
    }

    public static final class Discard1BlackAngusToGain2Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.BLACK_ANGUS, 1);
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    public static final class Gain1Certificate extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainCertificates(1);
            return ImmediateActions.none();
        }
    }

    public static final class Draw2CattleCards extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.getCattleMarket().draw();
            game.getCattleMarket().draw();

            return ImmediateActions.none();
        }
    }

    public static final class Discard2GuernseyToGain4Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.GUERNSEY, 2);
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class DiscardPairToGain3Dollars extends Action {
        @NonNull
        private final CattleType type;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(type, 2);
            game.currentPlayerState().gainDollars(3);
            return ImmediateActions.none();
        }
    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class Move extends Action {

        @NonNull List<Location> steps;
        int atLeast;
        int atMost;
        boolean fees;

        public Move(List<Location> steps) {
            this(steps, 1, Integer.MAX_VALUE, true);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("Must take at least one step");
            }

            PlayerState currentPlayerState = game.currentPlayerState();

            if (steps.size() > Math.min(atMost, currentPlayerState.getStepLimit(game.getPlayers().size()))) {
                throw new IllegalArgumentException("Number of steps exceeds limit");
            }

            if (steps.size() < atLeast) {
                throw new IllegalArgumentException("Number of steps below minimum");
            }

            Player player = game.getCurrentPlayer();

            if (game.getTrail().isAtLocation(player)) {
                Location from = game.getTrail().getCurrentLocation(player);

                checkDirectAndConsecutiveSteps(from, steps);

                if (fees) {
                    payFees(game);
                }
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

    public static final class Move1Forward extends Move {
        public Move1Forward(Location to) {
            super(Collections.singletonList(to), 1, 1, true);
        }
    }

    public static final class Move2Forward extends Move {
        public Move2Forward(Location to) {
            super(Collections.singletonList(to), 1, 2, true);
        }
    }

    public static final class Move3Forward extends Move {
        public Move3Forward(Location to) {
            super(Collections.singletonList(to), 1, 3, true);
        }
    }

    public static final class Move3ForwardWithoutFees extends Move {
        public Move3ForwardWithoutFees(Location to) {
            super(Collections.singletonList(to), 1, 3, false);
        }
    }

    public static final class Move4Forward extends Move {
        public Move4Forward(Location to) {
            super(Collections.singletonList(to), 1, 4, true);
        }
    }

    public static final class RemoveHazardFor5Dollars extends RemoveHazard {
        public RemoveHazardFor5Dollars(Hazard hazard) {
            super(hazard, 5);
        }
    }

    public static final class Discard1HolsteinToGain10Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.HOLSTEIN, 1);
            game.currentPlayerState().gainDollars(10);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineAtMost2Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineAtMost3Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 3).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class ExtraordinaryDelivery extends Action {
        @NonNull RailroadTrack.Space to;
        @NonNull City city;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            RailroadTrack.EngineMove engineMove = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE);

            if (city.getValue() > engineMove.getSteps()) {
                throw new IllegalArgumentException("City value must be <= spaces that engine moved backwards");
            }

            return game.deliverToCity(city).andThen(engineMove.getImmediateActions());
        }
    }

    public static final class MaxCertificates extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainMaxCertificates();
            return ImmediateActions.none();
        }
    }

    public static final class Gain1DollarPerBuildingInWoods extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            int buildingsInWoods = game.getTrail().buildingsInWoods(game.getCurrentPlayer());
            game.currentPlayerState().gainDollars(buildingsInWoods * 2);
            return ImmediateActions.none();
        }
    }

    public static final class Gain2CertificatesAnd2DollarsPerTeepeePair extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            int pairs = game.currentPlayerState().numberOfTeepeePairs();

            game.currentPlayerState().gainCertificates(pairs * 2);
            game.currentPlayerState().gainDollars(pairs * 2);

            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineAtMost5Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 5).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Discard1ObjectiveCardToGain2Certificates extends Action {
        @NonNull ObjectiveCard card;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCard(card);
            game.currentPlayerState().gainCertificates(2);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngine1BackwardsToGain3Dollars extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
            game.currentPlayerState().gainDollars(3);
            return immediateActions;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Discard1JerseyToMoveEngine1Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 1).getImmediateActions();
        }
    }

    public static final class Discard1DutchBeltToGain3Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.DUTCH_BELT, 1);
            game.currentPlayerState().gainDollars(3);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1BlackAngusToGain2Certificates extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.BLACK_ANGUS, 1);
            game.currentPlayerState().gainCertificates(2);
            return ImmediateActions.none();
        }
    }

    public static final class Gain1DollarPerEngineer extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(game.currentPlayerState().getNumberOfEngineers());
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand extends Action {
        @NonNull Card.CattleCard cattleCard;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCard(cattleCard);
            game.currentPlayerState().gainDollars(3);

            return ImmediateActions.of(PossibleAction.mandatory(Add1ObjectiveCardToHand.class));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineForwardUpToNumberOfBuildingsInWoods extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            int buildingsInWoods = game.getTrail().buildingsInWoods(game.getCurrentPlayer());
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, buildingsInWoods).getImmediateActions();
        }
    }

    public static final class UseAdjacentBuilding extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            Location currentLocation = game.getTrail().getCurrentLocation(game.getCurrentPlayer());

            Set<Location> adjacentLocations = game.getTrail().getAdjacentLocations(currentLocation);

            return ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(adjacentLocations.stream()
                    .filter(adjacentLocation -> adjacentLocation instanceof Location.BuildingLocation)
                    .map(adjacentLocation -> (Location.BuildingLocation) adjacentLocation)
                    .flatMap(adjacentBuildingLocation -> adjacentBuildingLocation.getBuilding().stream())
                    .map(building -> building.getPossibleAction(game)))));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class UpgradeAnyStationBehindEngine extends Action {
        @NonNull Station station;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            RailroadTrack.Space stationSpace = game.getRailroadTrack().getSpace(station);
            RailroadTrack.Space currentSpace = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());

            if (!stationSpace.isBefore(currentSpace)) {
                throw new IllegalArgumentException("Station must be behind engine");
            }

            return station.upgrade(game);
        }
    }

    public static final class Gain4Dollars extends Action {
        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(4);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class MoveEngineAtMost4Forward extends Action {
        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 4).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Discard1CattleCardToGain1Certificate extends Action {
        @NonNull Card.CattleCard cattleCard;

        @Override
        ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCard(cattleCard);
            game.currentPlayerState().gainCertificates(1);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain1CertificateAnd2Dollars extends Action {
        @Override
        ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainCertificates(1);
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Add1ObjectiveCardToHand extends Action {
        @NonNull ObjectiveCard objectiveCard;

        @Override
        ImmediateActions perform(Game game, Random random) {
            game.getObjectiveCards().remove(objectiveCard);
            game.currentPlayerState().addCardToHand(objectiveCard);

            return ImmediateActions.none();
        }
    }
}
