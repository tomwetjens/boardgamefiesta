package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Automa {

    public void execute(GWT game, Player player, Random random) {
        // TODO Currently a super stupid implementation
        var currentPlayerState = game.currentPlayerState();
        var possibleActions = game.possibleActions();

        if (possibleActions.contains(Action.PlaceBid.class)) {
            if (game.canSkip()) {
                game.skip(random);
            } else {
                game.perform(new Action.PlaceBid(lowestBidPossible(game)), random);
            }
        } else if (possibleActions.contains(Action.Move.class)) {
            game.perform(new Action.Move(calculateMove(game, player)), random);
        } else if (possibleActions.contains(Action.Move1Forward.class)) {
            game.perform(new Action.Move1Forward(calculateMove(game, player)), random);
        } else if (possibleActions.contains(Action.Move2Forward.class)) {
            game.perform(new Action.Move2Forward(calculateMove(game, player)), random);
        } else if (possibleActions.contains(Action.Move3Forward.class)) {
            game.perform(new Action.Move3Forward(calculateMove(game, player)), random);
        } else if (possibleActions.contains(Action.Move3ForwardWithoutFees.class)) {
            game.perform(new Action.Move3ForwardWithoutFees(calculateMoveWithoutFees(game, player)), random);
        } else if (possibleActions.contains(Action.Move4Forward.class)) {
            game.perform(new Action.Move4Forward(calculateMove(game, player)), random);
        } else if (possibleActions.contains(Action.Move5Forward.class)) {
            game.perform(new Action.Move5Forward(calculateMove(game, player)), random);
        } else if (possibleActions.contains(Action.ChooseForesight1.class)) {
            game.perform(new Action.ChooseForesight1(chooseForesight(game.getForesights().choices(0), random)), random);
        } else if (possibleActions.contains(Action.ChooseForesight2.class)) {
            game.perform(new Action.ChooseForesight2(chooseForesight(game.getForesights().choices(1), random)), random);
        } else if (possibleActions.contains(Action.ChooseForesight3.class)) {
            game.perform(new Action.ChooseForesight3(chooseForesight(game.getForesights().choices(2), random)), random);
        } else if (possibleActions.contains(Action.DeliverToCity.class)) {
            // TODO Pick highest possible city for now
            var possibleDeliveries = game.possibleDeliveries(player);
            game.perform(possibleDeliveries.stream().max(Comparator.comparingInt(RailroadTrack.PossibleDelivery::getReward))
                    .map(possibleDelivery -> new Action.DeliverToCity(possibleDelivery.getCity(), possibleDelivery.getCertificates()))
                    .orElse(new Action.DeliverToCity(City.KANSAS_CITY, 0)), random);
        } else if (possibleActions.contains(Action.UnlockWhite.class)) {
            game.perform(new Action.UnlockWhite(chooseWhiteDisc(currentPlayerState, game)), random);
        } else if (possibleActions.contains(Action.UnlockBlackOrWhite.class)) {
            game.perform(new Action.UnlockBlackOrWhite(chooseBlackOrWhiteDisc(currentPlayerState, game)), random);
        } else if (possibleActions.contains(Action.TakeObjectiveCard.class) && !game.getObjectiveCards().getAvailable().isEmpty()) {
            // TODO Just pick any now
            var objectiveCard = game.getObjectiveCards().getAvailable().iterator().next();
            game.perform(new Action.TakeObjectiveCard(objectiveCard), random);
        } else {
            if (possibleActions.contains(Action.TradeWithTribes.class)) {
                // TODO For now just snatch up any positive $ teepee
                var teepeeToTake = game.getTrail().getTeepeeLocations().stream()
                        .filter(teepeeLocation -> !teepeeLocation.isEmpty())
                        .filter(teepeeLocation -> teepeeLocation.getReward() > 0)
                        .max(Comparator.comparingInt(Location.TeepeeLocation::getReward));
                if (teepeeToTake.isPresent()) {
                    game.perform(new Action.TradeWithTribes(teepeeToTake.get().getReward()), random);
                    return;
                }
            }
            // TODO For now just stupidly end turn
            game.endTurn(player, random);
        }
    }

    private Bid lowestBidPossible(GWT game) {
        var bids = game.getPlayers().stream()
                .map(game::playerState)
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        return IntStream.range(0, game.getPlayerOrder().size())
                .filter(position -> bids.stream().noneMatch(bid -> bid.getPosition() == position))
                .mapToObj(position -> new Bid(position, 0))
                .findFirst()
                .orElseGet(() -> bids.stream()
                        .max(Comparator.comparingInt(Bid::getPoints))
                        .map(bid -> new Bid(bid.getPosition(), bid.getPoints() + 1))
                        .orElse(new Bid(0, 0)));
    }

    private List<Location> calculateMove(GWT game, Player player) {
        // TODO For now just go to the nearest own player/neutral building, using the cheapest route
        return game.possibleMoves(player).stream()
                .min(Comparator.comparingInt((PossibleMove possibleMove) ->
                        possibleMove.getTo() instanceof Location.BuildingLocation
                                ? ((Location.BuildingLocation) possibleMove.getTo()).getBuilding()
                                .map(building -> building instanceof PlayerBuilding
                                        ? ((PlayerBuilding) building).getPlayer() == game.getCurrentPlayers() ? 0
                                        : 2 // Other player's building
                                        : 1) // Neutral building
                                .orElse(2) // Empty, shouldn't happen
                                : 2) // Hazard, teepee
                        .thenComparingInt(PossibleMove::getCost)
                        .thenComparingInt((PossibleMove possibleMove) -> possibleMove.getSteps().size()))
                .map(PossibleMove::getSteps)
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private List<Location> calculateMoveWithoutFees(GWT game, Player player) {
        // TODO For now just go to closest location past the highest fees
        return game.possibleMoves(player).stream()
                .max(Comparator.comparingInt(PossibleMove::getCost)
                        .thenComparing(Comparator.comparingInt((PossibleMove possibleMove) -> possibleMove.getSteps().size()).reversed()))
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS))
                .getSteps();
    }

    private Unlockable chooseWhiteDisc(PlayerState playerState, GWT game) {
        if (playerState.canUnlock(Unlockable.AUX_GAIN_DOLLAR, game)) {
            return Unlockable.AUX_GAIN_DOLLAR;
        }
        if (playerState.canUnlock(Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD, game)) {
            return Unlockable.AUX_DRAW_CARD_TO_DISCARD_CARD;
        }
        if (playerState.canUnlock(Unlockable.CERT_LIMIT_4, game)) {
            return Unlockable.CERT_LIMIT_4;
        }

        // TODO Just pick any now
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> unlockable.getDiscColor() == DiscColor.WHITE)
                .filter(unlockable1 -> playerState.canUnlock(unlockable1, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private Unlockable chooseBlackOrWhiteDisc(PlayerState playerState, GWT game) {
        if (playerState.canUnlock(Unlockable.EXTRA_STEP_DOLLARS, game)) {
            return Unlockable.EXTRA_STEP_DOLLARS;
        }
        if (playerState.canUnlock(Unlockable.EXTRA_CARD, game)) {
            return Unlockable.EXTRA_CARD;
        }

        // TODO Just pick any now
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> playerState.canUnlock(unlockable, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private int chooseForesight(List<KansasCitySupply.Tile> choices, Random random) {
        // TODO Just pick a random tile now
        var index = random.nextInt(choices.size());
        if (choices.get(index) != null) {
            return index;
        }
        // Pick the other one (could be empty as well)
        return (index + 1) % 2;
    }


}
