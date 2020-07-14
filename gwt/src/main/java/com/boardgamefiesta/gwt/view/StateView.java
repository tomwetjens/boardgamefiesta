package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.Player;
import com.boardgamefiesta.gwt.logic.Game;
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
    boolean turn;
    boolean ended;
    Set<PossibleMoveView> possibleMoves;
    Set<PossibleBuyView> possibleBuys;
    Set<PossibleDeliveryView> possibleDeliveries;
    Map<ActionType, Set<RailroadTrackView.SpaceView>> possibleSpaces;

    public StateView(Game state, Player viewingPlayer) {
        railroadTrack = new RailroadTrackView(state.getRailroadTrack());

        player = state.getPlayers().stream()
                .filter(p -> p == viewingPlayer)
                .map(state::playerState)
                .map(playerState -> new PlayerStateView(state, playerState, viewingPlayer))
                .findAny()
                .orElse(null);

        // Other players in play order
        var viewingPlayerIndex = state.getPlayers().indexOf(viewingPlayer);
        var playerCount = state.getPlayers().size();
        otherPlayers = IntStream.range(1, playerCount)
                .map(i -> (viewingPlayerIndex + i) % playerCount)
                .mapToObj(i -> state.getPlayers().get(i))
                .map(p -> new PlayerStateView(state, state.playerState(p), viewingPlayer))
                .collect(Collectors.toList());

        foresights = new ForesightsView(state.getForesights());

        trail = new TrailView(state.getTrail());

        jobMarket = new JobMarketView(state.getJobMarket());

        cattleMarket = new CattleMarketView(state.getCattleMarket());

        objectiveCards = new ObjectiveCardsView(state.getObjectiveCards());

        currentPlayer = new PlayerView(state.getCurrentPlayer());

        ended = state.isEnded();

        if (viewingPlayer == state.getCurrentPlayer()) {
            actions = state.possibleActions().stream()
                    .map(ActionType::of)
                    .sorted(Comparator.comparing(Enum::name))
                    .collect(Collectors.toList());

            turn = true;

            if (actions.contains(ActionType.MOVE)) {
                possibleMoves = getPossibleMoves(state, state.currentPlayerState().getStepLimit(state.getPlayers().size()));
            } else if (actions.contains(ActionType.MOVE_1_FORWARD)) {
                possibleMoves = getPossibleMoves(state, 1);
            } else if (actions.contains(ActionType.MOVE_2_FORWARD)) {
                possibleMoves = getPossibleMoves(state, 2);
            } else if (actions.contains(ActionType.MOVE_3_FORWARD)) {
                possibleMoves = getPossibleMoves(state, 3);
            } else if (actions.contains(ActionType.MOVE_3_FORWARD_WITHOUT_FEES)) {
                possibleMoves = getPossibleMoves(state, 3);
            } else if (actions.contains(ActionType.MOVE_4_FORWARD)) {
                possibleMoves = getPossibleMoves(state, 4);
            } else if (actions.contains(ActionType.MOVE_5_FORWARD)) {
                possibleMoves = getPossibleMoves(state, 5);
            }

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
            } else if (actions.contains(ActionType.MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS,
                        getPossibleSpacesBackwards(state, viewingPlayer, 1, Integer.MAX_VALUE));
            } else if (actions.contains(ActionType.MOVE_ENGINE_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, state.playerState(viewingPlayer).getNumberOfEngineers()));
            } else if (actions.contains(ActionType.MOVE_ENGINE_1_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_1_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 1));
            } else if (actions.contains(ActionType.MOVE_ENGINE_2_OR_3_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_2_OR_3_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 2, 3));
            } else if (actions.contains(ActionType.MOVE_ENGINE_AT_MOST_2_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_MOST_2_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 2));
            } else if (actions.contains(ActionType.MOVE_ENGINE_AT_MOST_3_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_MOST_3_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 3));
            } else if (actions.contains(ActionType.MOVE_ENGINE_AT_MOST_4_FORWARD)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_AT_MOST_4_FORWARD,
                        getPossibleSpacesForward(state, viewingPlayer, 1, 4));
            } else if (actions.contains(ActionType.MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS)) {
                possibleSpaces.put(ActionType.MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS,
                        getPossibleSpacesForward(state, viewingPlayer, 1, state.getTrail().buildingsInWoods(viewingPlayer)));
            } else {
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
                            getPossibleSpacesForward(state, viewingPlayer, 2, 2));
                }

                if (actions.contains(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD)) {
                    possibleSpaces.put(ActionType.MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD,
                            getPossibleSpacesBackwards(state, viewingPlayer, 1, 1));
                }
                if (actions.contains(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS)) {
                    possibleSpaces.put(ActionType.MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS,
                            getPossibleSpacesBackwards(state, viewingPlayer, 2, 2));
                }
            }
        } else {
            actions = Collections.emptyList();
        }
    }

    private Set<RailroadTrackView.SpaceView> getPossibleSpacesForward(Game state, Player player, int atLeast, int atMost) {
        return state.getRailroadTrack().reachableSpacesForward(state.getRailroadTrack().currentSpace(player), atLeast, atMost).stream()
                .map(space -> new RailroadTrackView.SpaceView(state.getRailroadTrack(), space))
                .collect(Collectors.toSet());
    }

    private Set<RailroadTrackView.SpaceView> getPossibleSpacesBackwards(Game state, Player player, int atLeast, int atMost) {
        return state.getRailroadTrack().reachableSpacesBackwards(state.getRailroadTrack().currentSpace(player), atLeast, atMost).stream()
                .map(space -> new RailroadTrackView.SpaceView(state.getRailroadTrack(), space))
                .collect(Collectors.toSet());
    }

    private Set<PossibleDeliveryView> getPossibleDeliveries(Game game, Player player) {
        var playerState = game.playerState(player);
        return playerState.possibleDeliveries(game.getRailroadTrack()).stream()
                .map(PossibleDeliveryView::new)
                .collect(Collectors.toSet());
    }

    private Set<PossibleBuyView> getPossibleBuys(Game game, Player player) {
        var playerState = game.playerState(player);
        return game.getCattleMarket().possibleBuys(playerState.getNumberOfCowboys(), playerState.getBalance()).stream()
                .map(PossibleBuyView::new)
                .collect(Collectors.toSet());
    }

    private Set<PossibleMoveView> getPossibleMoves(Game game, int atMost) {
        return game.possibleMoves(game.getCurrentPlayer(), atMost).stream()
                .map(PossibleMoveView::new)
                .collect(Collectors.toSet());
    }

}
