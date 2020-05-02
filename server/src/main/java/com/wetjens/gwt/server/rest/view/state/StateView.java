package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Player;
import com.wetjens.gwt.server.domain.ActionType;
import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class StateView {

    RailroadTrackView railroadTrack;
    PlayerStateView player;
    List<PlayerStateView> otherPlayers;
    List<Player> playerOrder;
    ForesightsView foresights;
    TrailView trail;
    JobMarketView jobMarket;
    CattleMarketView cattleMarket;
    List<ObjectiveCardView> objectiveCards;

    PlayerView currentPlayer;
    List<ActionType> actions;
    Instant expires;
    boolean turn;

    public StateView(Game game, Player viewingPlayer, Map<Player, User> userMap) {
        var state = game.getState();

        railroadTrack = new RailroadTrackView(state.getRailroadTrack());

        player = state.getPlayers().stream()
                .filter(p -> p == viewingPlayer)
                .map(state::playerState)
                .map(playerState -> new PlayerStateView(playerState, viewingPlayer, userMap.get(playerState.getPlayer())))
                .findAny()
                .orElse(null);

        // Other players in play order
        var viewingPlayerIndex = state.getPlayers().indexOf(viewingPlayer);
        var playerCount = state.getPlayers().size();
        otherPlayers = IntStream.range(1, playerCount)
                .map(i -> (viewingPlayerIndex + i) % playerCount)
                .mapToObj(i -> state.getPlayers().get(i))
                .map(p -> new PlayerStateView(state.playerState(p), viewingPlayer, userMap.get(p)))
                .collect(Collectors.toList());

        playerOrder = state.getPlayers();

        foresights = new ForesightsView(state.getForesights());

        trail = new TrailView(state.getTrail());

        jobMarket = new JobMarketView(state.getJobMarket());

        cattleMarket = new CattleMarketView(state.getCattleMarket());

        objectiveCards = state.getObjectiveCards().getAvailable().stream()
                .map(ObjectiveCardView::new)
                .sorted()
                .collect(Collectors.toList());

        currentPlayer = new PlayerView(state.getCurrentPlayer(), userMap.get(state.getCurrentPlayer()));

        if (viewingPlayer == state.getCurrentPlayer()) {
            actions = state.possibleActions().stream()
                    .map(ActionType::of)
                    .sorted(Comparator.comparing(Enum::name))
                    .collect(Collectors.toList());
            turn = true;
        } else {
            actions = Collections.emptyList();
            turn = false;
        }

        expires = game.getExpires();
    }

}
