package com.tomsboardgames.server.domain;

import com.tomsboardgames.api.Game;
import com.tomsboardgames.api.State;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class Games implements DomainService {

    private final Map<Game.Id, Game<State>> map;

    @Inject
    public Games(Instance<Game<?>> instance) {
        map = instance.stream()
                .collect(Collectors.toMap(Game::getId, game -> (Game<State>) game));
    }

    public static Games instance() {
        return DomainService.instance(Games.class);
    }

    public Game<State> get(Game.Id gameId) {
        var implementation = map.get(gameId);
        if (implementation == null) {
            throw new IllegalArgumentException("Unknown game: " + gameId.getId());
        }
        return implementation;
    }

    public Stream<Game<State>> findAll() {
        return map.values().stream();
    }

    public Optional<Game<State>> findById(Game.Id id) {
        return Optional.ofNullable(map.get(id));
    }
}
