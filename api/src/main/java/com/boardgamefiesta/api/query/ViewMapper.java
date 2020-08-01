package com.boardgamefiesta.api.query;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import lombok.NonNull;

public interface ViewMapper<T extends State> {
    Object toView(@NonNull T state, Player viewer);
}
