package com.boardgamefiesta.api.query;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;

public interface ViewMapper<T extends State> {
    Object toView(T state, Player viewer);
}
