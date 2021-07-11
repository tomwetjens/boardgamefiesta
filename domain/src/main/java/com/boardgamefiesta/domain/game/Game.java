package com.boardgamefiesta.domain.game;

import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.api.spi.GameProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.Duration;
import java.util.Random;
import java.util.Set;

@Builder
public class Game {

    @Getter
    private final Id id;

    @Getter
    private final GameProvider<State> provider;

    public int getMinNumberOfPlayers() {
        return provider.getMinNumberOfPlayers();
    }

    public int getMaxNumberOfPlayers() {
        return provider.getMaxNumberOfPlayers();
    }

    public Set<PlayerColor> getSupportedColors() {
        return provider.getSupportedColors();
    }

    public State start(Set<Player> players, Options options, EventListener eventListener, Random random) {
        return provider.start(players, options, eventListener, random);
    }

    public void executeAutoma(State state, Player player, Random random) {
        provider.executeAutoma(state, player, random);
    }

    public boolean hasAutoma() {
        return provider.hasAutoma();
    }

    public Duration getTimeLimit(Options options) {
        return provider.getTimeLimit(options);
    }

    @Value(staticConstructor = "fromString")
    public static class Id {
        String id;

        /**
         * @deprecated For backwards compatibility. Use {@link #fromString(String)} instead.
         */
        @Deprecated
        public static Id of(String str) {
            return fromString(str);
        }
    }

}
