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

package com.boardgamefiesta.api.spi;

import com.boardgamefiesta.api.domain.State;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameProviders {

    private static final GameProviders INSTANCE = new GameProviders();

    private final Map<String, GameProvider<State>> providers;

    @SuppressWarnings("unchecked")
    public GameProviders() {
        var serviceLoader = ServiceLoader.load(GameProvider.class);

        providers = serviceLoader.stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(GameProvider::getId, provider -> (GameProvider<State>) provider));
    }

    public static GameProviders instance() {
        return INSTANCE;
    }

    public GameProvider<State> get(String id) {
        return find(id).orElseThrow(()-> new IllegalArgumentException("Unknown provider: " + id));
    }

    public Optional<GameProvider<State>> find(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public Stream<GameProvider<State>> list() {
        return providers.values().stream();
    }
}
