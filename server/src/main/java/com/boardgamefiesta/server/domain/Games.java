package com.boardgamefiesta.server.domain;

import com.boardgamefiesta.api.spi.GameProviders;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class Games implements DomainService {

    private final Map<Game.Id, Game> games = new ConcurrentHashMap<>();

    public static Games instance() {
        return DomainService.instance(Games.class);
    }

    public Game get(Game.Id id) {
        return games.computeIfAbsent(id, key -> Game.builder()
                .id(key)
                .provider(GameProviders.instance().get(id.getId()))
                .build());
    }

    public Optional<Game> findById(Game.Id id) {
        return Optional.ofNullable(games.computeIfAbsent(id, key ->
                GameProviders.instance().find(id.getId())
                        .map(provider -> Game.builder()
                                .id(key)
                                .provider(provider)
                                .build())
                        .orElse(null)));
    }
}
