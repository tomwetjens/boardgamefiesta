package com.wetjens.gwt.server.domain;

import java.util.stream.Stream;

public interface Games {

    static Games instance() {
        return DomainService.instance(Games.class);
    }

    Game findById(Game.Id id);

    void add(Game game);

    void update(Game game);

    Stream<Game> findByUserId(User.Id id);

    int countByUserId(User.Id id);
}
