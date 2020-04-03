package com.wetjens.gwt.server;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;

import com.wetjens.gwt.Game;

@ApplicationScoped
public class GameRepository {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public Game findById(String id) {
        return games.computeIfAbsent(id, k -> new Game(Arrays.asList("Player A", "Player B"), Game.Options.builder().beginner(true).build(), new Random()));
    }

    public void save(String id, Game game) {
        games.put(id, game);
    }
}
