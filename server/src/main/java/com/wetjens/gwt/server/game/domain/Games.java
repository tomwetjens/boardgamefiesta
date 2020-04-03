package com.wetjens.gwt.server.game.domain;

import java.util.stream.Stream;

import com.wetjens.gwt.server.user.domain.User;

public interface Games {

    Game findById(Game.Id id);

    void add(Game game);

    void update(Game game);

    Stream<Game> findByUserId(User.Id id);

}
