package com.boardgamefiesta.api;

public interface ViewMapper<T extends State> {
    Object toView(T state, Player viewer);
}
