package com.wetjens.gwt.server.game.view;

import java.util.Set;
import java.util.stream.Collectors;

import com.wetjens.gwt.server.game.domain.Game;
import com.wetjens.gwt.server.user.domain.User;
import lombok.Value;

@Value
public class GameView {

    String id;
    Game.Status status;
    String ownerUserId;
    Set<String> playerUserIds;

    public GameView(Game game) {
        id = game.getId().getId();
        status = game.getStatus();
        ownerUserId = game.getOwner().getId();
        playerUserIds = game.getPlayers().stream().map(User.Id::getId).collect(Collectors.toSet());
    }
}
