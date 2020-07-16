package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import lombok.*;
import lombok.experimental.NonFinal;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    abstract ImmediateActions perform(@NonNull Game game, @NonNull Random random);

    List<String> toEventParams(Game game) {
        return Collections.emptyList();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class BuyCattle extends Action {

        @NonNull
        private Set<Card.CattleCard> cattleCards;

        public BuyCattle(@NonNull Set<Card.CattleCard> cattleCards) {
            if (cattleCards.isEmpty()) {
                throw new GWTException(GWTError.MUST_SPECIFY_CATTLE_CARD);
            }

            this.cattleCards = cattleCards;
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            int cost = calculateCost(game);

            game.currentPlayerState().payDollars(cost);

            int unusedCowboys = game.getCattleMarket().buy(cattleCards, game.currentPlayerState().getNumberOfCowboys());

            game.currentPlayerState().gainCards(cattleCards);

            if (unusedCowboys > 0) {
                //Any of the cowboys that you do not put to use buying a cattle card during this action may instead
                //be used to draw 2 cards from the market cattle stack and add them face up to the cattle market
                game.fireEvent(game.getCurrentPlayer(), GWTEvent.Type.MAY_DRAW_CATTLE_CARDS, Collections.emptyList());
                return ImmediateActions.of(PossibleAction.repeat(0, unusedCowboys, Action.Draw2CattleCards.class));
            }
            return ImmediateActions.none();
        }

        @Override
        List<String> toEventParams(Game game) {
            return Stream.concat(Stream.of(Integer.toString(calculateCost(game))), cattleCards.stream()
                    .map(Card.CattleCard::getType)
                    .map(CattleType::name))
                    .collect(Collectors.toList());
        }

        private int calculateCost(Game game) {
            return game.getCattleMarket().cost(cattleCards, game.currentPlayerState().getNumberOfCowboys());
        }
    }

    public static final class SingleAuxiliaryAction extends Action {

        public ImmediateActions perform(Game game, Random random) {
            return ImmediateActions.of(PossibleAction.choice(game.currentPlayerState().unlockedSingleAuxiliaryActions()));
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
            return ImmediateActions.of(PossibleAction.choice(playerState.unlockedSingleOrDoubleAuxiliaryActions()));
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
            return ImmediateActions.of(PossibleAction.mandatory(Action.DiscardCard.class));
        }
    }

    public static final class Draw2Cards extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().drawCards(2, random);
            return ImmediateActions.of(PossibleAction.repeat(2, 2, Action.DiscardCard.class));
        }
    }

    public static final class Draw3Cards extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().drawCards(3, random);
            return ImmediateActions.of(PossibleAction.repeat(3, 3, Action.DiscardCard.class));
        }
    }

    public static final class Draw4Cards extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().drawCards(4, random);
            return ImmediateActions.of(PossibleAction.repeat(4, 4, Action.DiscardCard.class));
        }
    }

    public static final class Draw5Cards extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().drawCards(5, random);
            return ImmediateActions.of(PossibleAction.repeat(5, 5, Action.DiscardCard.class));
        }
    }

    public static final class Draw6Cards extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().drawCards(6, random);
            return ImmediateActions.of(PossibleAction.repeat(6, 6, Action.DiscardCard.class));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class TakeObjectiveCard extends Action {

        // null means draw from stack
        ObjectiveCard objectiveCard;

        public TakeObjectiveCard() {
            this.objectiveCard = null;
        }

        public TakeObjectiveCard(@NonNull ObjectiveCard objectiveCard) {
            this.objectiveCard = objectiveCard;
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            if (objectiveCard != null) {
                game.getObjectiveCards().remove(objectiveCard);
                game.currentPlayerState().gainCard(objectiveCard);
            } else {
                game.currentPlayerState().gainCard(game.getObjectiveCards().draw());
            }
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
    public static final class MoveEngine1Forward extends Action {

        @NonNull RailroadTrack.Space to;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1).getImmediateActions();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class RemoveCard extends Action {

        @NonNull Card card;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().removeCards(Collections.singleton(card));
            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class TradeWithTribes extends Action {

        int reward;

        @Override
        public ImmediateActions perform(Game game, Random random) {
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

            return ImmediateActions.none();
        }

        @Override
        List<String> toEventParams(Game game) {
            return List.of(Integer.toString(reward));
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

        public RemoveHazardForFree(Location.HazardLocation location) {
            super(location, 0);
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

        int rowIndex;
        @NonNull Worker worker;
        int modifier;

        public HireWorker(int rowIndex, Worker worker) {
            this(rowIndex, worker, 0);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            int cost = game.getJobMarket().cost(rowIndex, worker) + modifier;

            game.currentPlayerState().payDollars(cost);

            game.getJobMarket().takeWorker(rowIndex, worker);

            return game.currentPlayerState().gainWorker(worker, game);
        }

        @Override
        List<String> toEventParams(Game game) {
            return List.of(worker.name());
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
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainTempCertificates(1);
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
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainTempCertificates(2);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain4Dollars extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainTempCertificates(4);
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

            return ImmediateActions.none();
        }

        @Override
        List<String> toEventParams(Game game) {
            return List.of(building.getName(), location.getName());
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
        public ImmediateActions perform(@NonNull Game game, Random random) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            Station station = current.getStation().orElseThrow(() -> new GWTException(GWTError.NOT_AT_STATION));

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
            Station station = getStation(game);

            return station.appointStationMaster(game, worker);
        }

        @Override
        List<String> toEventParams(Game game) {
            return getStation(game).getStationMaster()
                    .map(stationMaster -> List.of(worker.name(), stationMaster.name()))
                    .orElseGet(() -> List.of(worker.name()));
        }

        private Station getStation(Game game) {
            RailroadTrack.Space current = game.getRailroadTrack().currentSpace(game.getCurrentPlayer());
            return current.getStation().orElseThrow(() -> new GWTException(GWTError.NOT_AT_STATION));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class ChooseForesights extends Action {

        @NonNull
        private List<Integer> choices;

        public ChooseForesights(@NonNull List<Integer> choices) {
            if (choices.size() != 3) {
                throw new GWTException(GWTError.MUST_SPECIFY_3_FORESIGHTS);
            }

            this.choices = new ArrayList<>(choices);
        }

        @Override
        public ImmediateActions perform(@NonNull Game game, @NonNull Random random) {
            for (int columnIndex = 0; columnIndex < choices.size(); columnIndex++) {
                int rowIndex = choices.get(columnIndex);

                KansasCitySupply.Tile tile = game.getForesights().take(columnIndex, rowIndex);

                placeTile(game, tile);
            }

            return ImmediateActions.of(PossibleAction.mandatory(DeliverToCity.class));
        }

        @Override
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
    public static final class DeliverToCity extends Action {

        @NonNull City city;
        int certificates; // temp + perm player wants to use

        @Override
        public ImmediateActions perform(Game game, Random random) {
            int payout = calculatePayout(game);

            int tempCertificates = Math.max(0, certificates - game.currentPlayerState().permanentCertificates());
            if (tempCertificates > 0) {
                game.currentPlayerState().spendTempCertificates(tempCertificates);
            }

            game.currentPlayerState().gainDollars(payout);
            game.currentPlayerState().discardHand();

            ImmediateActions immediateActions = game.deliverToCity(city);

            game.getTrail().moveToStart(game.getCurrentPlayer());

            return immediateActions;
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

        @Override
        List<String> toEventParams(Game game) {
            return List.of(city.name(), Integer.toString(certificates), Integer.toString(calculatePayout(game)));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class UnlockWhite extends Action {

        @NonNull Unlockable unlock;

        @Override
        ImmediateActions perform(Game game, Random random) {
            if (unlock.getDiscColor() != DiscColor.WHITE) {
                throw new GWTException(GWTError.MUST_PICK_WHITE_DISC);
            }
            game.currentPlayerState().unlock(unlock);
            return ImmediateActions.none();
        }

        @Override
        List<String> toEventParams(Game game) {
            return List.of(unlock.name());
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

        @Override
        List<String> toEventParams(Game game) {
            return List.of(unlock.name());
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
            game.currentPlayerState().gainTempCertificates(2);
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
                    .andThen(PossibleAction.mandatory(RemoveCard.class), PossibleAction.mandatory(RemoveCard.class));
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
            game.currentPlayerState().gainTempCertificates(1);
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
                    .andThen(PossibleAction.mandatory(RemoveCard.class));
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

        @Override
        List<String> toEventParams(Game game) {
            return List.of(type.name());
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
        public ImmediateActions perform(Game game, Random random) {
            if (game.getTrail().getLocation(location.getName()) != location) {
                throw new GWTException(GWTError.NO_SUCH_LOCATION);
            }

            var hazard = location.getHazard().orElseThrow(() -> new GWTException(GWTError.LOCATION_EMPTY));

            game.currentPlayerState().payDollars(cost);
            game.currentPlayerState().addHazard(hazard);
            location.removeHazard();

            return ImmediateActions.none();
        }

        @Override
        List<String> toEventParams(Game game) {
            var hazard = location.getHazard().orElseThrow(() -> new GWTException(GWTError.LOCATION_EMPTY));
            return List.of(hazard.getType().name(), Integer.toString(hazard.getPoints()), Integer.toString(cost));
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
            game.currentPlayerState().gainTempCertificates(1);
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

    public static final class Discard1GuernseyToGain4Dollars extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.GUERNSEY, 1);
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
        int atMost;
        boolean payFeesAndActivate;

        public Move(List<Location> steps) {
            this(steps, Integer.MAX_VALUE, true);
        }

        @Override
        public ImmediateActions perform(Game game, Random random) {
            if (steps.isEmpty()) {
                throw new GWTException(GWTError.MUST_MOVE_AT_LEAST_STEPS);
            }

            PlayerState currentPlayerState = game.currentPlayerState();

            int stepLimit = currentPlayerState.getStepLimit(game.getPlayers().size());
            if (steps.size() > Math.min(atMost, stepLimit)) {
                throw new GWTException(GWTError.STEPS_EXCEED_LIMIT);
            }

            Player player = game.getCurrentPlayer();

            Location to = steps.get(steps.size() - 1);

            game.getTrail().getCurrentLocation(player).ifPresent(from -> {
                checkDirectAndConsecutiveSteps(from, steps);

                if (payFeesAndActivate) {
                    payFees(game);
                }
            });

            game.getTrail().movePlayer(player, to);

            return payFeesAndActivate ? to.activate(game) : ImmediateActions.none();
        }

        @Override
        List<String> toEventParams(Game game) {
            Location to = steps.get(steps.size() - 1);
            // TODO Include name of building, hazard or teepee in the params
            return List.of(to.getName());
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
                    location.getHand().getFee(game.getPlayers().size()));

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
                throw new GWTException(GWTError.CITY_VALUE_MUST_BE_LESS_THEN_OR_EQUAL_TO_SPACES_THAT_ENGINE_MOVED_BACKWARDS);
            }

            return game.deliverToCity(city).andThen(engineMove.getImmediateActions());
        }

        @Override
        List<String> toEventParams(Game game) {
            return List.of(city.name());
        }
    }

    public static final class MaxCertificates extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().gainMaxTempCertificates();
            return ImmediateActions.none();
        }
    }

    public static final class Gain2DollarsPerBuildingInWoods extends Action {

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

            game.currentPlayerState().gainTempCertificates(pairs * 2);
            game.currentPlayerState().gainDollars(pairs * 2);

            return ImmediateActions.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static final class Discard1ObjectiveCardToGain2Certificates extends Action {
        @NonNull ObjectiveCard card;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCard(card);
            game.currentPlayerState().gainTempCertificates(2);
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

    public static final class Discard1JerseyToMoveEngine1Forward extends Action {

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            return ImmediateActions.of(PossibleAction.mandatory(MoveEngine1Forward.class));
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
            game.currentPlayerState().gainTempCertificates(2);
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

        @NonNull CattleType cattleType;

        @Override
        public ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(cattleType, 1);
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
            Location currentLocation = game.getTrail().getCurrentLocation(game.getCurrentPlayer())
                    .orElseThrow(() -> new GWTException(GWTError.NOT_AT_LOCATION));

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
                throw new GWTException(GWTError.STATION_MUST_BE_BEHIND_ENGINE);
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

        @NonNull CattleType cattleType;

        @Override
        ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(cattleType, 1);
            game.currentPlayerState().gainTempCertificates(1);
            return ImmediateActions.none();
        }
    }

    public static final class Discard1JerseyToGain1CertificateAnd2Dollars extends Action {

        @Override
        ImmediateActions perform(Game game, Random random) {
            game.currentPlayerState().discardCattleCards(CattleType.JERSEY, 1);
            game.currentPlayerState().gainTempCertificates(1);
            game.currentPlayerState().gainDollars(2);
            return ImmediateActions.none();
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
        ImmediateActions perform(Game game, Random random) {
            if (objectiveCard != null) {
                game.getObjectiveCards().remove(objectiveCard);
                game.currentPlayerState().addCardToHand(objectiveCard);
            } else {
                game.currentPlayerState().addCardToHand(game.getObjectiveCards().draw());
            }
            return ImmediateActions.none();
        }
    }
}
