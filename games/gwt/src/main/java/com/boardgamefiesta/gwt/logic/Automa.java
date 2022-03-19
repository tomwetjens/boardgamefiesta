/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Automa {

    public void execute(GWT game, Player player, Random random) {
        // TODO Currently a super stupid implementation
        var currentPlayerState = game.currentPlayerState();
        var possibleActions = game.possibleActions();

        if (possibleActions.contains(Action.PlaceBid.class)) {
            if (game.canSkip()) {
                game.skip(random);
            } else {
                game.perform(new Action.PlaceBid(game.lowestBidPossible()), random);
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
            game.perform(new Action.ChooseForesight1(game.getForesights().chooseAnyForesight(0, random)), random);
        } else if (possibleActions.contains(Action.ChooseForesight2.class)) {
            game.perform(new Action.ChooseForesight2(game.getForesights().chooseAnyForesight(1, random)), random);
        } else if (possibleActions.contains(Action.ChooseForesight3.class)) {
            game.perform(new Action.ChooseForesight3(game.getForesights().chooseAnyForesight(2, random)), random);
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
                    game.perform(new Action.TradeWithTribes(teepeeToTake.get()), random);
                    return;
                }
            }
            // TODO For now just stupidly end turn
            game.endTurn(player, random);
        }
    }


    private List<Location> calculateMove(GWT game, Player player) {
        // TODO For now just go to the nearest own player/neutral building, using the cheapest route
        return game.getTrail().calculateMoveToNearestOwnThenNeutralThenOtherPlayersBuildingUsingCheapestThenShortestRoute(player, game.playerState(player).getBalance(), game.getPlayers().size());
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
        return playerState.chooseAnyWhiteDisc(game);
    }

    private Unlockable chooseBlackOrWhiteDisc(PlayerState playerState, GWT game) {
        return playerState.chooseAnyBlackOrWhiteDisc(game);
    }

}
