package com.wetjens.gwt.view;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.api.Player;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    Set<PossibleMoveView> possibleMoves;
    Set<PossibleBuyView> possibleBuys;
    Set<PossibleDeliveryView> possibleDeliveries;

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
            }

            if (actions.contains(ActionType.BUY_CATTLE)) {
                possibleBuys = getPossibleBuys(state, viewingPlayer);
            }

            if (actions.contains(ActionType.DELIVER_TO_CITY)) {
                possibleDeliveries = getPossibleDeliveries(state, viewingPlayer);
            }
        } else {
            actions = Collections.emptyList();
        }
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
