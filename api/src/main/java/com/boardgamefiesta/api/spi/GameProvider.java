package com.boardgamefiesta.api.spi;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.*;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;

import java.time.Duration;
import java.util.Random;
import java.util.Set;

public interface GameProvider<T extends State> {

    String getId();

    int getMinNumberOfPlayers();

    int getMaxNumberOfPlayers();

    Set<PlayerColor> getSupportedColors();

    T start(Set<Player> players, Options options, EventListener eventListener, Random random);

    StateSerializer<T> getStateSerializer();

    StateDeserializer<T> getStateDeserializer();

    ActionMapper<T> getActionMapper();

    ViewMapper<T> getViewMapper();

    void executeAutoma(T state, Player player, Random random);

    boolean hasAutoma();

    Duration getTimeLimit(Options options);


}