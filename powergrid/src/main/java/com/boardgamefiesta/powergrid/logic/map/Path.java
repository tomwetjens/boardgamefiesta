package com.boardgamefiesta.powergrid.logic.map;

import lombok.Value;

import java.util.List;

@Value
public class Path {

    List<Connection> connections;

    public int getCost() {
        return connections.stream().mapToInt(Connection::getCost).sum();
    }

}
