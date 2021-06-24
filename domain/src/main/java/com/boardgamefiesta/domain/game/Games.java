package com.boardgamefiesta.domain.game;

import com.boardgamefiesta.api.spi.GameProviders;
import com.boardgamefiesta.domain.DomainService;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class Games implements DomainService {

    private final Map<Game.Id, Game> games;

    public Games() {
        games = GameProviders.instance()
                .list()
                .map(provider -> Game.builder()
                        .id(Game.Id.of(provider.getId()))
                        .provider(provider)
                        .build())
                .collect(Collectors.toMap(Game::getId, Function.identity()));
    }

    public Game get(Game.Id id) {
        return games.get(id);
    }

    public Optional<Game> findById(Game.Id id) {
        return Optional.ofNullable(games.get(id));
    }

    public Stream<Game> list() {
        return games.values().stream();
    }
}
