package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.server.domain.Game;
import com.wetjens.gwt.server.domain.Player;
import com.wetjens.gwt.server.domain.User;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class GameView {

    String id;
    Game.Status status;
    Instant created;
    Instant started;
    Instant ended;
    Instant expires;
    UserView owner;
    Set<PlayerView> otherPlayers;
    boolean accepted;
    boolean startable;

    Boolean turn;
    UserView currentPlayer;

    public GameView(Game game, Map<User.Id, User> userMap, User.Id currentUserId) {
        id = game.getId().getId();
        status = game.getStatus();
        owner = new UserView(game.getOwner(), userMap.get(game.getOwner()));
        otherPlayers = game.getPlayers().stream()
                .filter(player -> !player.getUserId().equals(currentUserId))
                .map(player -> new PlayerView(player, userMap.get(player.getUserId())))
                .collect(Collectors.toSet());
        accepted = game.getPlayers().stream().filter(player -> player.getUserId().equals(currentUserId)).anyMatch(player -> player.getStatus() == Player.Status.ACCEPTED);
        created = game.getCreated();
        started = game.getStarted();
        ended = game.getEnded();
        expires = game.getExpires();
        startable = game.canStart() && currentUserId.equals(game.getOwner());

        if(game.getState() != null) {
            turn =  game.getState().getCurrentPlayer().getName().equals(currentUserId.getId());

            User.Id currentPlayerUserId = User.Id.of(game.getState().getCurrentPlayer().getName());
            currentPlayer = new UserView(currentPlayerUserId, userMap.get(currentPlayerUserId));
        } else {
            turn = null;
            currentPlayer = null;
        }
    }
}
