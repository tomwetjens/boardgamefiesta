package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import lombok.*;
import lombok.experimental.NonFinal;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Action implements com.boardgamefiesta.api.domain.Action {

    @SuppressWarnings("unchecked")
    static Class<? extends Action> deserializeClass(String str) {
        try {
            return (Class<? extends Action>) Class.forName(Action.class.getName() + "$" + str);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown action: " + str);
        }
    }

    static String serializeClass(Class<? extends Action> action) {
        return action.getSimpleName();
    }

    abstract ActionResult perform(@NonNull Game game, @NonNull Random random);

    @Value
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class BuyCattle extends Action {

        @NonNull
        Card.CattleCard card;

        Card.CattleCard secondCard;

        int cowboys;
        int dollars;

        public BuyCattle(@NonNull List<Card.CattleCard> cards, int cowboys, int dollars) {
            this(cards.get(0), cards.size() > 1 ? cards.get(1) : null, cowboys, dollars);
        }

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();

            var usableCowboys = playerState.getNumberOfCowboys() - playerState.getUsedCowboys();

            if (cowboys > usableCowboys) {
                throw new GWTException(GWTError.NOT_ENOUGH_COWBOYS);
            }

            var cost = game.getCattleMarket().buy(card, secondCard, cowboys, dollars);

            playerState.payDollars(cost.getDollars());
            playerState.useCowboys(cost.getCowboys());

            playerState.gainCard(card);

            if (secondCard != null) {
                playerState.gainCard(secondCard);

                game.fireActionEvent("BUY_2_CATTLE", List.of(
                        Integer.toString(cost.getDollars()),
                        card.getType().name(),
                        Integer.toString(card.getPoints()),
                        secondCard.getType().name(),
                        Integer.toString(secondCard.getPoints()),
                        Integer.toString(cost.getCowboys())));
            } else {
                game.fireActionEvent("BUY_CATTLE", List.of(
                        Integer.toString(cost.getDollars()),
                        card.getType().name(),
                        Integer.toString(card.getPoints()),
                        Integer.toString(cost.getCowboys())));
            }

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

    }

    public static final class SingleAuxiliaryAction extends Action {

        public ActionResult perform(Game game, Random random) {
            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.of(PossibleAction.choice(game.currentPlayerState().unlockedSingleAuxiliaryActions())));
        }
    }

    public static final class Gain2Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class SingleOrDoubleAuxiliaryAction extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.of(PossibleAction.choice(game.currentPlayerState().unlockedSingleOrDoubleAuxiliaryActions())));
        }
    }

    public static final class Pay2DollarsToMoveEngine2Forward extends Action {

        private final RailroadTrack.Space to;

        public Pay2DollarsToMoveEngine2Forward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().payDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2).getImmediateActions());
        }
    }

    public static final class DrawCard extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().drawCard(random);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.mandatory(Action.DiscardCard.class)));
        }
    }

    public static final class Draw2Cards extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().drawCards(2, random);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.repeat(2, 2, Action.DiscardCard.class)));
        }
    }

    public static final class Draw3Cards extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().drawCards(3, random);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.repeat(3, 3, Action.DiscardCard.class)));
        }
    }

    public static final class Draw4Cards extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().drawCards(4, random);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.repeat(4, 4, Action.DiscardCard.class)));
        }
    }

    public static final class Draw5Cards extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().drawCards(5, random);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.repeat(5, 5, Action.DiscardCard.class)));
        }
    }

    public static final class Draw6Cards extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().drawCards(6, random);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.repeat(6, 6, Action.DiscardCard.class)));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class TakeObjectiveCard extends Action {

        // null means draw from stack
        ObjectiveCard objectiveCard;

        public TakeObjectiveCard() {
            this.objectiveCard = null;
        }

        public TakeObjectiveCard(@NonNull ObjectiveCard objectiveCard) {
            this.objectiveCard = objectiveCard;
        }

        @Override
        public ActionResult perform(Game game, Random random) {
            if (objectiveCard != null) {
                game.getObjectiveCards().remove(objectiveCard);
                game.currentPlayerState().gainCard(objectiveCard);
            } else {
                game.currentPlayerState().gainCard(game.getObjectiveCards().draw());
            }

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngineForward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, game.currentPlayerState().getNumberOfEngineers());

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngine1Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class RemoveCard extends Action {

        @NonNull Card card;

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().removeCards(Collections.singleton(card));

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class TradeWithTribes extends Action {

        int reward;

        @Override
        public ActionResult perform(Game game, Random random) {
            Location.TeepeeLocation teepeeLocation = game.getTrail().getTeepeeLocation(reward);
            Teepee teepee = teepeeLocation.getTeepee()
                    .orElseThrow(() -> new GWTException(GWTError.NO_TEEPEE_AT_LOCATION));

            if (teepeeLocation.getReward() > 0) {
                teepeeLocation.removeTeepee();
                game.currentPlayerState().addTeepee(teepee);
                game.currentPlayerState().gainDollars(teepeeLocation.getReward());
            } else {
                game.currentPlayerState().payDollars(teepeeLocation.getReward());
                teepeeLocation.removeTeepee();
                game.currentPlayerState().addTeepee(teepee);
            }

            game.fireActionEvent(this, List.of(Integer.toString(reward)));

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DiscardCard extends Action {

        @NonNull Card card;

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().discardCard(card);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class RemoveHazardForFree extends RemoveHazard {

        public RemoveHazardForFree(Location.HazardLocation location) {
            super(location, 0);
        }

    }

    public static final class Discard1Guernsey extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            PlayerState playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.GUERNSEY, 1);
            playerState.gainDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class HireWorker extends Action {

        int rowIndex;
        @NonNull Worker worker;
        int modifier;

        public HireWorker(int rowIndex, Worker worker) {
            this(rowIndex, worker, 0);
        }

        @Override
        public ActionResult perform(Game game, Random random) {
            int cost = game.getJobMarket().cost(rowIndex, worker) + modifier;

            game.currentPlayerState().payDollars(cost);

            game.getJobMarket().takeWorker(rowIndex, worker);

            game.fireActionEvent(this, List.of(worker.name()));

            return ActionResult.undoAllowed(game.currentPlayerState().gainWorker(worker, game));
        }

    }

    public static final class HireWorkerPlus2 extends HireWorker {

        public HireWorkerPlus2(int rowIndex, Worker worker) {
            super(rowIndex, worker, 2);
        }
    }

    public static final class PlaceCheapBuilding extends PlaceBuilding {

        public PlaceCheapBuilding(Location.BuildingLocation location, PlayerBuilding building) {
            super(location, building, 1);
        }
    }

    public static final class Discard1JerseyToGain1Certificate extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.JERSEY, 1);
            playerState.gainTempCertificates(1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Discard1JerseyToGain2Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.JERSEY, 1);
            playerState.gainDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class HireWorkerMinus2 extends HireWorker {

        public HireWorkerMinus2(int rowIndex, Worker worker) {
            super(rowIndex, worker, -2);
        }
    }

    public static final class HireWorkerMinus1 extends HireWorker {

        public HireWorkerMinus1(int rowIndex, Worker worker) {
            super(rowIndex, worker, -1);
        }
    }

    public static final class Discard1JerseyToGain2Certificates extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.JERSEY, 1);
            playerState.gainTempCertificates(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Discard1JerseyToGain4Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.JERSEY, 1);
            playerState.gainDollars(4);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Discard1DutchBeltToGain2Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            PlayerState playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.DUTCH_BELT, 1);
            playerState.gainDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
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
        public ActionResult perform(Game game, Random random) {
            if (!game.currentPlayerState().hasAvailable(building)) {
                throw new GWTException(GWTError.BUILDING_NOT_AVAILABLE);
            }

            int craftsmenNeeded = craftsmenNeeded();

            if (craftsmenNeeded > game.currentPlayerState().getNumberOfCraftsmen()) {
                throw new GWTException(GWTError.NOT_ENOUGH_CRAFTSMEN);
            }

            int cost = craftsmenNeeded * costPerCraftsman;
            game.currentPlayerState().payDollars(cost);
            game.currentPlayerState().removeBuilding(building);

            location.placeBuilding(building);

            game.fireActionEvent(this, List.of(building.getName(), location.getName()));

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

        private int craftsmenNeeded() {
            return existingBuildingToReplace()
                    // If replacing an existing building, only the difference is needed
                    .filter(existingBuilding -> {
                        if (existingBuilding.getCraftsmen() >= building.getCraftsmen()) {
                            throw new GWTException(GWTError.REPLACEMENT_BUILDING_MUST_BE_HIGHER);
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
                            throw new GWTException(GWTError.CANNOT_REPLACE_NEUTRAL_BUILDING);
                        }
                        return true;
                    })
                    .map(existingBuilding -> (PlayerBuilding) existingBuilding)
                    .filter(existingBuilding -> {
                        if (existingBuilding.getPlayer() != building.getPlayer()) {
                            throw new GWTException(GWTError.CANNOT_REPLACE_BUILDING_OF_OTHER_PLAYER);
                        }
                        return true;
                    });
        }
    }

    public static final class UpgradeStation extends Action {

        @Override
        public ActionResult perform(@NonNull Game game, Random random) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new GWTException(GWTError.NOT_AT_STATION));

            var immediateActions = station.upgrade(game);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(immediateActions);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DowngradeStation extends Action {

        @NonNull Station station;

        @Override
        ActionResult perform(Game game, Random random) {
            station.downgrade(game);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class AppointStationMaster extends Action {

        @NonNull Worker worker;

        @Override
        public ActionResult perform(@NonNull Game game, Random random) {
            Station station = getStation(game);

            var stationMaster = station.getStationMaster().orElseThrow(() -> new GWTException(GWTError.CANNOT_PERFORM_ACTION));
            var immediateActions = station.appointStationMaster(game, worker);

            game.fireActionEvent(this, List.of(worker.name(), stationMaster.name()));

            return ActionResult.undoAllowed(immediateActions);
        }

        private Station getStation(Game game) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            return current.getStation().orElseThrow(() -> new GWTException(GWTError.NOT_AT_STATION));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ChooseForesights extends Action {

        @NonNull
        List<Integer> choices;

        public ChooseForesights(@NonNull List<Integer> choices) {
            if (choices.size() != 3) {
                throw new GWTException(GWTError.MUST_SPECIFY_3_FORESIGHTS);
            }

            this.choices = new ArrayList<>(choices);
        }

        @Override
        public ActionResult perform(@NonNull Game game, @NonNull Random random) {
            game.fireActionEvent(this, toEventParams(game));

            for (int columnIndex = 0; columnIndex < choices.size(); columnIndex++) {
                int rowIndex = choices.get(columnIndex);

                KansasCitySupply.Tile tile = game.getForesights().take(columnIndex, rowIndex);

                if (tile != null) {
                    placeTile(game, tile);
                }
            }

            return ActionResult.undoNotAllowed(ImmediateActions.of(PossibleAction.mandatory(DeliverToCity.class)));
        }

        List<String> toEventParams(Game game) {
            return IntStream.range(0, 3)
                    .mapToObj(columnIndex -> game.getForesights().choices(columnIndex).get(choices.get(columnIndex)))
                    .map(tile -> tile.getTeepee() != null
                            ? tile.getTeepee().name() : tile.getWorker() != null
                            ? tile.getWorker().name() : tile.getHazard().getType().name())
                    .collect(Collectors.toList());
        }

        private void placeTile(Game game, KansasCitySupply.Tile tile) {
            if (tile.getWorker() != null) {
                JobMarket jobMarket = game.getJobMarket();

                if (!jobMarket.isClosed()) {
                    boolean fillUpCattleMarket = jobMarket.addWorker(tile.getWorker());

                    if (fillUpCattleMarket) {
                        game.fireEvent(game.getCurrentPlayer(), GWTEvent.Type.FILL_UP_CATTLE_MARKET, Collections.emptyList());

                        game.getCattleMarket().fillUp();
                    }

                    if (jobMarket.isClosed()) {
                        game.currentPlayerState().gainJobMarketToken();
                        game.fireEvent(game.getCurrentPlayer(), GWTEvent.Type.GAINS_JOB_MARKET_TOKEN, Collections.emptyList());
                    }
                }
            } else if (tile.getHazard() != null) {
                game.getTrail().placeHazard(tile.getHazard());
            } else {
                game.getTrail().placeTeepee(tile.getTeepee());
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DeliverToCity extends Action {

        @NonNull City city;
        int certificates; // temp + perm player wants to use

        @Override
        public ActionResult perform(Game game, Random random) {
            int payout = calculatePayout(game);

            int tempCertificates = Math.max(0, certificates - game.currentPlayerState().permanentCertificates());
            if (tempCertificates > 0) {
                game.currentPlayerState().spendTempCertificates(tempCertificates);
            }

            game.currentPlayerState().gainDollars(payout);
            game.currentPlayerState().discardHand();

            ImmediateActions immediateActions = game.deliverToCity(city);

            game.getTrail().moveToStart(game.getCurrentPlayer());

            game.fireActionEvent(this, List.of(city.name(), Integer.toString(certificates), Integer.toString(payout)));

            return ActionResult.undoAllowed(immediateActions);
        }

        private int calculatePayout(Game game) {
            int breedingValue = game.currentPlayerState().handValue() + certificates;

            if (breedingValue < city.getValue()) {
                throw new GWTException(GWTError.NOT_ENOUGH_BREEDING_VALUE);
            }

            int transportCosts = Math.max(0, city.getSignals() - game.getRailroadTrack().signalsPassed(game.getCurrentPlayer()));

            int payout = breedingValue - transportCosts;
            if (city == City.KANSAS_CITY) {
                payout += 6;
            }
            return payout;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class UnlockWhite extends Action {

        @NonNull Unlockable unlock;

        @Override
        ActionResult perform(Game game, Random random) {
            if (unlock.getDiscColor() != DiscColor.WHITE) {
                throw new GWTException(GWTError.MUST_PICK_WHITE_DISC);
            }

            game.currentPlayerState().unlock(unlock);

            game.fireActionEvent(this, List.of(unlock.name()));

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class UnlockBlackOrWhite extends Action {

        @NonNull Unlockable unlock;

        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().unlock(unlock);

            game.fireActionEvent(this, List.of(unlock.name()));

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngineAtLeast1BackwardsAndGain3Dollars extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            ImmediateActions immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE).getImmediateActions();

            game.currentPlayerState().gainDollars(3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(immediateActions);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().payDollars(2);

            var immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2).getImmediateActions();
            game.currentPlayerState().gainTempCertificates(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(immediateActions);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngine2BackwardsToRemove2Cards extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move
                    .getImmediateActions()
                    .andThen(PossibleAction.repeat(2, 2, RemoveCard.class)));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngine2Or3Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 2, 3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    public static final class Gain1Dollar extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay1DollarAndMoveEngine1BackwardsToGain1Certificate extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.payDollars(1);

            var immediateActions = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
            playerState.gainTempCertificates(1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(immediateActions);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay1DollarToMoveEngine1Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().payDollars(1);

            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngine1BackwardsToRemove1Card extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move
                    .getImmediateActions()
                    .andThen(PossibleAction.mandatory(RemoveCard.class)));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DiscardPairToGain4Dollars extends Action {

        @NonNull CattleType type;

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(type, 2);
            playerState.gainDollars(4);

            game.fireActionEvent(this, List.of(type.name()));

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class RemoveHazard extends Action {

        @NonNull Location.HazardLocation location;
        int cost;

        public RemoveHazard(Location.HazardLocation location) {
            this(location, 7);
        }

        @Override
        public ActionResult perform(Game game, Random random) {
            if (game.getTrail().getLocation(location.getName()) != location) {
                throw new GWTException(GWTError.NO_SUCH_LOCATION);
            }

            var hazard = location.getHazard().orElseThrow(() -> new GWTException(GWTError.LOCATION_EMPTY));

            game.currentPlayerState().payDollars(cost);
            game.currentPlayerState().addHazard(hazard);
            location.removeHazard();

            game.fireActionEvent(this, List.of(hazard.getType().name(), Integer.toString(hazard.getPoints()), Integer.toString(cost)));

            return ActionResult.undoAllowed(ImmediateActions.none());
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class PlayObjectiveCard extends Action {

        @NonNull ObjectiveCard objectiveCard;

        @Override
        public ActionResult perform(Game game, Random random) {
            var immediateActions = game.currentPlayerState().playObjectiveCard(objectiveCard);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(immediateActions);
        }
    }

    public static final class Discard1BlackAngusToGain2Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.BLACK_ANGUS, 1);
            playerState.gainDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Gain1Certificate extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().gainTempCertificates(1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Draw2CattleCards extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().useCowboys(1);

            game.getCattleMarket().draw();
            game.getCattleMarket().draw();

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.none());
        }
    }

    public static final class Discard1GuernseyToGain4Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.GUERNSEY, 1);
            playerState.gainDollars(4);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DiscardPairToGain3Dollars extends Action {

        @NonNull
        CattleType type;

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(type, 2);
            playerState.gainDollars(3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @NonFinal
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static class Move extends Action {

        @NonNull List<Location> steps;
        Integer atMost;
        boolean payFeesAndActivate;

        public Move(List<Location> steps) {
            this(steps, null, true);
        }

        @Override
        public ActionResult perform(Game game, Random random) {
            if (steps.isEmpty()) {
                throw new GWTException(GWTError.MUST_MOVE_AT_LEAST_STEPS);
            }

            PlayerState currentPlayerState = game.currentPlayerState();
            if (atMost == null) {
                var stepLimit = currentPlayerState.getStepLimit(game.getPlayerOrder().size());
                if (steps.size() > stepLimit) {
                    throw new GWTException(GWTError.STEPS_EXCEED_LIMIT);
                }
            } else if (steps.size() > atMost) {
                throw new GWTException(GWTError.STEPS_EXCEED_LIMIT);
            }

            Player player = game.getCurrentPlayer();

            Location to = steps.get(steps.size() - 1);
            if (to.isEmpty()) {
                throw new GWTException(GWTError.LOCATION_EMPTY);
            }

            game.getTrail().getCurrentLocation(player).ifPresentOrElse(from -> {
                checkDirectAndConsecutiveSteps(from, steps);

                if (payFeesAndActivate) {
                    payFees(game);
                }
            }, () -> {
                // Must start at neutral building
                if (!(to instanceof Location.BuildingLocation)) {
                    throw new GWTException(GWTError.MUST_START_ON_NEUTRAL_BUILDING);
                }
            });

            game.getTrail().movePlayer(player, to);

            if (to instanceof Location.BuildingLocation) {
                ((Location.BuildingLocation) to).getBuilding()
                        .ifPresentOrElse(building -> {
                            if (building instanceof PlayerBuilding) {
                                game.fireActionEvent("MOVE_TO_PLAYER_BUILDING"
                                        + (payFeesAndActivate ? "" : "_WITHOUT_FEES"), List.of(to.getName(), building.getName(), ((PlayerBuilding) building).getPlayer().getName()));
                            } else {
                                game.fireActionEvent("MOVE_TO_BUILDING"
                                        + (payFeesAndActivate ? "" : "_WITHOUT_FEES"), List.of(to.getName(), building.getName()));
                            }
                        }, () -> game.fireActionEvent("MOVE"
                                + (payFeesAndActivate ? "" : "_WITHOUT_FEES"), List.of(to.getName())));
            } else {
                game.fireActionEvent("MOVE", List.of(to.getName()));
            }

            if (payFeesAndActivate
                    || to == game.getTrail().getKansasCity()) { // Always activate KC, else player is stuck
                // Actions from previous locations (and possibly a mandatory Move in case of free-moving to KC) cannot be performed anymore
                game.getActionStack().clear();

                // Actions of new location are now possible
                return ActionResult.undoAllowed(to.activate(game)
                        .map(ImmediateActions::of)
                        .orElse(ImmediateActions.none()));
            } else {
                // From Objective Card action
                return ActionResult.undoAllowed(ImmediateActions.none());
            }
        }

        private void checkDirectAndConsecutiveSteps(Location from, List<Location> steps) {
            Location to = steps.get(0);
            if (!from.isDirect(to)) {
                throw new GWTException(GWTError.CANNOT_STEP_DIRECTLY_FROM_TO);
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
                    location.getHand().getFee(game.getPlayerOrder().size()));

            if (amount > 0) {
                feeRecipient(location)
                        .ifPresentOrElse(recipient -> {
                            if (recipient != game.getCurrentPlayer()) {
                                // Pay to other player
                                currentPlayerState.payDollars(amount);
                                game.playerState(recipient).gainDollars(amount);

                                game.fireEvent(game.getCurrentPlayer(), GWTEvent.Type.PAY_FEE_PLAYER, List.of(Integer.toString(amount), recipient.getName()));
                            }
                        }, () -> {
                            // Pay to bank
                            currentPlayerState.payDollars(amount);

                            game.fireEvent(game.getCurrentPlayer(), GWTEvent.Type.PAY_FEE_BANK, List.of(Integer.toString(amount)));
                        });
            }
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

        public Move1Forward(List<Location> steps) {
            super(steps, 1, true);
        }
    }

    public static final class Move2Forward extends Move {

        public Move2Forward(List<Location> steps) {
            super(steps, 2, true);
        }
    }

    public static final class Move3Forward extends Move {

        public Move3Forward(List<Location> steps) {
            super(steps, 3, true);
        }
    }

    public static final class Move3ForwardWithoutFees extends Move {

        public Move3ForwardWithoutFees(List<Location> steps) {
            super(steps, 3, false);
        }
    }

    public static final class Move4Forward extends Move {

        public Move4Forward(List<Location> steps) {
            super(steps, 4, true);
        }
    }

    public static final class Move5Forward extends Move {

        public Move5Forward(List<Location> steps) {
            super(steps, 5, true);
        }
    }

    public static final class RemoveHazardFor5Dollars extends RemoveHazard {

        public RemoveHazardFor5Dollars(Location.HazardLocation location) {
            super(location, 5);
        }
    }

    public static final class Discard1HolsteinToGain10Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.HOLSTEIN, 1);
            playerState.gainDollars(10);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngineAtMost2Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngineAtMost3Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ExtraordinaryDelivery extends Action {

        @NonNull RailroadTrack.Space to;
        @NonNull City city;

        @Override
        public ActionResult perform(Game game, Random random) {
            RailroadTrack.EngineMove engineMove = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, Integer.MAX_VALUE);

            if (city.getValue() > engineMove.getSteps()) {
                throw new GWTException(GWTError.CITY_VALUE_MUST_BE_LESS_THEN_OR_EQUAL_TO_SPACES_THAT_ENGINE_MOVED_BACKWARDS);
            }

            var immediateActions = game.deliverToCity(city);

            game.fireActionEvent(this, List.of(city.name()));

            return ActionResult.undoAllowed(immediateActions.andThen(engineMove.getImmediateActions()));
        }

    }

    public static final class MaxCertificates extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().gainMaxTempCertificates();

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Gain2DollarsPerBuildingInWoods extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            int buildingsInWoods = game.getTrail().buildingsInWoods(game.getCurrentPlayer());
            game.currentPlayerState().gainDollars(buildingsInWoods * 2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Gain2CertificatesAnd2DollarsPerTeepeePair extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            int pairs = playerState.numberOfTeepeePairs();
            playerState.gainTempCertificates(pairs * 2);
            playerState.gainDollars(pairs * 2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Discard1ObjectiveCardToGain2Certificates extends Action {
        @NonNull ObjectiveCard card;

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCard(card);
            playerState.gainTempCertificates(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngine1BackwardsToGain3Dollars extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1);
            game.currentPlayerState().gainDollars(3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    public static final class Discard1JerseyToMoveEngine1Forward extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.of(PossibleAction.mandatory(MoveEngine1Forward.class)));
        }
    }

    public static final class Discard1DutchBeltToGain3Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.DUTCH_BELT, 1);
            playerState.gainDollars(3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Discard1BlackAngusToGain2Certificates extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.BLACK_ANGUS, 1);
            playerState.gainTempCertificates(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Gain1DollarPerEngineer extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.gainDollars(playerState.getNumberOfEngineers());

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand extends Action {

        @NonNull CattleType cattleType;

        @Override
        public ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(cattleType, 1);
            playerState.gainDollars(3);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(
                    !game.getObjectiveCards().isEmpty()
                            ? ImmediateActions.of(PossibleAction.mandatory(Add1ObjectiveCardToHand.class))
                            : ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngineForwardUpToNumberOfBuildingsInWoods extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            int buildingsInWoods = game.getTrail().buildingsInWoods(game.getCurrentPlayer());
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, buildingsInWoods);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class UseAdjacentBuilding extends Action {

        Location.BuildingLocation adjacentLocation;

        @Override
        public ActionResult perform(Game game, Random random) {
            var currentLocation = game.getTrail().getCurrentLocation(game.getCurrentPlayer())
                    .orElseThrow(() -> new GWTException(GWTError.NOT_AT_LOCATION));

            var adjacentLocations = game.getTrail().getAdjacentLocations(currentLocation);

            if (!adjacentLocations.contains(adjacentLocation)) {
                throw new GWTException(GWTError.LOCATION_NOT_ADJACENT);
            }

            var building = adjacentLocation.getBuilding()
                    .orElseThrow(() -> new GWTException(GWTError.LOCATION_EMPTY));

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.of(building.activate(game)));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class UpgradeAnyStationBehindEngine extends Action {

        @NonNull Station station;

        @Override
        public ActionResult perform(Game game, Random random) {
            RailroadTrack.Space stationSpace = game.getRailroadTrack().getSpace(station);
            RailroadTrack.Space currentSpace = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());

            if (!currentSpace.isAfter(stationSpace)) {
                throw new GWTException(GWTError.STATION_MUST_BE_BEHIND_ENGINE);
            }

            var immediateActions = station.upgrade(game);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(immediateActions);
        }
    }

    public static final class Gain4Dollars extends Action {

        @Override
        public ActionResult perform(Game game, Random random) {
            game.currentPlayerState().gainDollars(4);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MoveEngineAtMost4Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ActionResult perform(Game game, Random random) {
            var move = game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 4);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(move.getImmediateActions());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Discard1CattleCardToGain1Certificate extends Action {

        @NonNull CattleType cattleType;

        @Override
        ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(cattleType, 1);
            playerState.gainTempCertificates(1);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    public static final class Discard1JerseyToGain1CertificateAnd2Dollars extends Action {

        @Override
        ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();
            playerState.discardCattleCards(CattleType.JERSEY, 1);
            playerState.gainTempCertificates(1);
            playerState.gainDollars(2);

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoAllowed(ImmediateActions.none());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Add1ObjectiveCardToHand extends Action {

        // null means draw from stack
        ObjectiveCard objectiveCard;

        public Add1ObjectiveCardToHand() {
            this.objectiveCard = null;
        }

        public Add1ObjectiveCardToHand(@NonNull ObjectiveCard objectiveCard) {
            this.objectiveCard = objectiveCard;
        }

        @Override
        ActionResult perform(Game game, Random random) {
            var playerState = game.currentPlayerState();

            if (objectiveCard != null) {
                game.getObjectiveCards().remove(objectiveCard);
                playerState.addCardToHand(objectiveCard);
            } else {
                playerState.addCardToHand(game.getObjectiveCards().draw());
            }

            game.fireActionEvent(this, Collections.emptyList());

            return ActionResult.undoNotAllowed(ImmediateActions.none());
        }
    }
}
