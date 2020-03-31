package com.wetjens.gwt.server;

import com.wetjens.gwt.Game;
import com.wetjens.gwt.Player;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GameRepository {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public Game findById(String id) {
        return games.computeIfAbsent(id, k -> new Game(Arrays.asList("Player A", "Player B"), new Random()));
    }

    public void save(String id, Game game) {
        games.put(id, game);
    }
}
