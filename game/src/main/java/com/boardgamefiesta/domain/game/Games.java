/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.game;

import com.boardgamefiesta.api.spi.GameProviders;
import com.boardgamefiesta.domain.DomainService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class Games implements DomainService {

    private final Map<Game.Id, Game> games;

    @Inject
    Games(@ConfigProperty(name = "bgf.games.enabled", defaultValue = "") String enabledGameIds) {
        this(Arrays.stream(enabledGameIds.split(","))
                .map(Game.Id::fromString)
                .collect(Collectors.toSet()));
    }

    private Games(Set<Game.Id> enabled) {
        games = GameProviders.instance()
                .list()
                .map(provider -> Game.builder()
                        .id(Game.Id.of(provider.getId()))
                        .provider(provider)
                        .build())
                .filter(game -> enabled.isEmpty() || enabled.contains(game.getId()))
                .collect(Collectors.toMap(Game::getId, Function.identity()));
    }

    public static Games all() {
        return new Games(Collections.emptySet());
    }

    public Game get(Game.Id id) {
        var game = games.get(id);
        if (game == null) {
            throw new IllegalArgumentException("Unknown game: " + id.getId());
        }
        return game;
    }

    public Optional<Game> findById(Game.Id id) {
        return Optional.ofNullable(games.get(id));
    }

    public Stream<Game> list() {
        return games.values().stream();
    }
}
