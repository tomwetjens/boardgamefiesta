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

package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.gwt.logic.GWT;
import com.boardgamefiesta.gwt.logic.RailroadTrack;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class StateView {

    RailroadTrackView railroadTrack;
    PlayerStateView player;
    List<PlayerStateView> otherPlayers;
    ForesightsView foresights;
    TrailView trail;
    JobMarketView jobMarket;
    CattleMarketView cattleMarket;
    ObjectiveCardsView objectiveCards;
    PlayerView currentPlayer;
    List<ActionType> actions;
    GWT.Status status;
    List<ObjectiveCardView> startingObjectiveCards;
    List<BidView> bids;
    boolean turn;
    boolean ended;
    Set<PossibleMoveView> possibleMoves;
    Set<PossibleBuyView> possibleBuys;
    Set<PossibleDeliveryView> possibleDeliveries;
    Map<ActionType, Set<String>> possibleSpaces;
    Map<ActionType, Set<String>> possibleTowns;
    boolean railsToTheNorth;

    public StateView(GWT state, Player viewingPlayer) {
        status = state.getStatus();

        railsToTheNorth = state.isRailsToTheNorth();

        if (state.getStartingObjectiveCards() != null) {
            startingObjectiveCards = state.getStartingObjectiveCards().stream()
                    .map(ObjectiveCardView::new)
                    .collect(Collectors.toList());
        }

        bids = state.playerOrderFromBids().stream()
                .map(player -> state.playerState(player).getBid()
                        .map(bid -> new BidView(player, bid))
                        .orElseGet(() -> new BidView(player)))
                .collect(Collectors.toList());

        railroadTrack = new RailroadTrackView(state.getRailroadTrack());

        if (viewingPlayer != null) {
            player = state.getPlayers().stream()
                    .filter(p -> p == viewingPlayer)
                    .map(state::playerState)
                    .map(playerState -> new PlayerStateView(state, playerState, viewingPlayer))
                    .findAny()
                    .orElse(null);

            // Other players in play order
            var viewingPlayerIndex = state.getPlayers().indexOf(viewingPlayer);
            var playerCount = state.getPlayers().size();
            otherPlayers =
                    // In order relative to the viewing player
                    IntStream.range(1, playerCount)
                            .map(i -> (viewingPlayerIndex + i) % playerCount)
                            .mapToObj(i -> state.getPlayers().get(i))
                            .map(p -> new PlayerStateView(state, state.playerState(p), viewingPlayer))
                            .collect(Collectors.toList());
        } else {
            otherPlayers = state.getPlayers().stream()
                    .map(p -> new PlayerStateView(state, state.playerState(p), viewingPlayer))
                    .collect(Collectors.toList());
        }

        foresights = new ForesightsView(state.getForesights());

        trail = new TrailView(state.getTrail());

        jobMarket = new JobMarketView(state.getJobMarket());

        cattleMarket = new CattleMarketView(state.getMode(), state.getCattleMarket());

        objectiveCards = new ObjectiveCardsView(state.getObjectiveCards());

        currentPlayer = new PlayerView(state.getCurrentPlayer());

        ended = state.isEnded();

        if (viewingPlayer == state.getCurrentPlayer()) {
            actions = state.possibleActions().stream()
                    .map(ActionType::of)
                    .sorted(Comparator.comparing(Enum::name))
                    .collect(Collectors.toList());

            turn = true;

            possibleMoves = state.possibleMoves(state.getCurrentPlayer()).stream()
                    .map(PossibleMoveView::new)
                    // when deduplicating, sort first, to keep the same one every time
                    .sorted(Comparator
                            // shortest route first
                            .comparingInt((PossibleMoveView possibleMoveView) -> possibleMoveView.getRoute().size())
                            // then the one with the most empty locations
                            .thenComparingInt((PossibleMoveView possibleMoveView) -> possibleMoveView.getSteps().size() - possibleMoveView.getRoute().size()))
                    // then deduplicate moves with equal cost and fees
                    .collect(Collectors.toSet());

            if (actions.contains(ActionType.BUY_CATTLE)) {
                possibleBuys = getPossibleBuys(state, viewingPlayer);
            }

            if (actions.contains(ActionType.DELIVER_TO_CITY)) {
                possibleDeliveries = getPossibleDeliveries(state, viewingPlayer);
            }

            possibleSpaces = new HashMap<>();

            if (actions.contains(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, 1));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, Integer.MAX_VALUE));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, state.playerState(viewingPlayer).getNumberOfEngineers()));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_1_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_1_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 1));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_2_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_2_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 2));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_2_OR_3_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_2_OR_3_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 2, 3));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_AT_MOST_2_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_MOST_2_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 2));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_AT_MOST_3_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_MOST_3_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 3));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_AT_MOST_4_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_MOST_4_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 4));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS,
                        getPossibleSpacesForward(state, viewingPlayer, 0, state.getTrail().buildingsInWoods(viewingPlayer)));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_HAZARDS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_HAZARDS,
                        getPossibleSpacesForward(state, viewingPlayer, 0, state.playerState(viewingPlayer).numberOfHazards()));
            }
            if (actions.contains(ActionType.EXTRAORDINARY_DELIVERY)) {
                possibleSpaces.put(ActionType.EXTRAORDINARY_DELIVERY,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, Integer.MAX_VALUE));
            }

            // Aux actions
            if (actions.contains(ActionType.PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE)) {
                possibleSpaces.put(ActionType.PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, 1));
            }
            if (actions.contains(ActionType.PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES)) {
                possibleSpaces.put(ActionType.PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES,
                        getPossibleSpacesBackwards(state, viewingPlayer, 2, 2));
            }
            if (actions.contains(ActionType.PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD)) {
                possibleSpaces.put(ActionType.PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 1));
            }
            if (actions.contains(ActionType.PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD)) {
                possibleSpaces.put(ActionType.PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 2));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, 1));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD_AND_GAIN_1_DOLLAR)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD_AND_GAIN_1_DOLLAR,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, 1));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS,
                        getPossibleSpacesBackwards(state, viewingPlayer, 2, 2));
            }
            if (actions.contains(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS_AND_GAIN_2_DOLLARS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS_AND_GAIN_2_DOLLARS,
                        getPossibleSpacesBackwards(state, viewingPlayer, 2, 2));
            }

            possibleTowns = new HashMap<>();

            if (actions.contains(ActionType.PLACE_BRANCHLET)) {
                possibleTowns.put(ActionType.PLACE_BRANCHLET, state.getRailroadTrack().possibleTowns(viewingPlayer)
                        .map(RailroadTrack.Town::getName)
                        .collect(Collectors.toSet()));
            }
        }
    }

    private Set<String> getPossibleSpacesForward(GWT state, Player player, int atLeast, int atMost) {
        return state.getRailroadTrack().reachableSpacesForward(state.getRailroadTrack().currentSpace(player), atLeast, atMost).stream()
                .map(RailroadTrack.Space::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> getPossibleSpacesBackwards(GWT state, Player player, int atLeast, int atMost) {
        return state.getRailroadTrack().reachableSpacesBackwards(state.getRailroadTrack().currentSpace(player), atLeast, atMost).stream()
                .map(RailroadTrack.Space::getName)
                .collect(Collectors.toSet());
    }

    private Set<PossibleDeliveryView> getPossibleDeliveries(GWT game, Player player) {
        return game.possibleDeliveries(player).stream()
                .map(PossibleDeliveryView::new)
                .collect(Collectors.toSet());
    }

    private Set<PossibleBuyView> getPossibleBuys(GWT game, Player player) {
        var playerState = game.playerState(player);
        return game.getCattleMarket().possibleBuys(playerState.getNumberOfCowboys() - playerState.getNumberOfCowboysUsedInTurn(), playerState.getBalance())
                .map(PossibleBuyView::new)
                .collect(Collectors.toSet());
    }

}
