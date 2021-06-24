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
