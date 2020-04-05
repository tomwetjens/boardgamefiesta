package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class GameView {

    String id;
    Game.Status status;
    String ownerUserId;
    Set<PlayerView> players;
    boolean accepted;

    public GameView(Game game, Map<User.Id, User> userMap, User.Id currentUserId) {
        id = game.getId().getId();
        status = game.getStatus();
        ownerUserId = game.getOwner().getId();
        players = game.getPlayers().stream()
                .map(player -> new PlayerView(player, userMap.get(player.getUserId())))
                .collect(Collectors.toSet());
        accepted = game.getPlayers().stream().filter(player -> player.getUserId().equals(currentUserId)).anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);
    }
}
