package com.tomsboardgames.server.domain;

import com.tomsboardgames.api.Game;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class Games {

    private final Map<String, Game> map;

    @Inject
    public Games(Instance<Game> instance) {
        map = instance.stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
    }

    public Game get(String name) {
        var implementation = map.get(name);
        if (implementation == null) {
            throw new IllegalArgumentException("Unknown implementation: " + name);
        }
        return implementation;
    }

    public Stream<Game> findAll() {
        return map.values().stream();
    }
}
