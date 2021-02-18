package com.boardgamefiesta.powergrid.logic.map;

public interface Connection {
    City getFrom();
    City getTo();
    int getCost();
}
