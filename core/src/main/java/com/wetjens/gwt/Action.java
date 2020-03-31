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

        public static final class Gain1Dollars extends Action {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().gainDollars(1);
                return ImmediateActions.none();
            }
        }

        public static final class Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate extends Action {
            private final RailroadTrack.Space to;

            public Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate(RailroadTrack.Space to) {
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

        public static final class Pay1DollarToMoveEngine1SpaceForward extends Action {
            private final RailroadTrack.Space to;

            public Pay1DollarToMoveEngine1SpaceForward(RailroadTrack.Space to) {
                this.to = to;
            }

            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().payDollars(1);
                return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 1, 1);
            }
        }

        public static final class MoveEngine1SpaceBackwardsToRemove1Card extends Action {
            private final RailroadTrack.Space to;

            public MoveEngine1SpaceBackwardsToRemove1Card(RailroadTrack.Space to) {
                this.to = to;
            }

            @Override
            public ImmediateActions perform(Game game) {
                return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 1, 1)
                        .andThen(PossibleAction.mandatory(Remove1Card.class));
            }
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

        public static final class Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates extends Action {

            private final RailroadTrack.Space to;

            public Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates(RailroadTrack.Space to) {
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

        public static final class MoveEngine2SpacesBackwardsToRemove2Cards extends Action {

            private final RailroadTrack.Space to;

            public MoveEngine2SpacesBackwardsToRemove2Cards(RailroadTrack.Space to) {
                this.to = to;
            }

            @Override
            public ImmediateActions perform(Game game) {
                return game.getRailroadTrack().moveEngineBackwards(game.getCurrentPlayer(), to, 2, 2)
                        .andThen(PossibleAction.mandatory(Remove2Cards.class));
            }
        }
    }

    public static final class Pay2DollarsToMoveEngine2SpacesForward extends Action {
        private final RailroadTrack.Space to;

        public Pay2DollarsToMoveEngine2SpacesForward(RailroadTrack.Space to) {
            this.to = to;
        }

        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().payDollars(2);

            return game.getRailroadTrack().moveEngineForward(game.getCurrentPlayer(), to, 0, 2);
        }
    }

    public abstract static class DrawCardThenDiscardCard extends Action {

        public static final class Draw1CardThenDiscard1Card extends DrawCardThenDiscardCard {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().drawCard();
                return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.Discard1Card.class));
            }
        }

        public static final class Draw2CardsThenDiscard2Cards extends DrawCardThenDiscardCard {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().drawCard();
                game.currentPlayerState().drawCard();
                return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.Discard2Cards.class));
            }
        }

        public static class Draw3CardsThenDiscard3Cards extends DrawCardThenDiscardCard {
            @Override
            public ImmediateActions perform(Game game) {
                game.currentPlayerState().drawCard();
                game.currentPlayerState().drawCard();
                game.currentPlayerState().drawCard();
                return ImmediateActions.of(PossibleAction.mandatory(DiscardCards.Discard3Cards.class));
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
            }else {
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

    public static final class DiscardOneGuernsey extends Action {
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

            // TODO
            if (city == City.SAN_FRANCISCO) {
                return ImmediateActions.of(PossibleAction.optional(GainObjectiveCard.class));
            }

            return ImmediateActions.none();
        }
    }
}
