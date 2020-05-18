package com.wetjens.gwt.api;

import java.util.List;

public interface InGameEvent {
    Player getPlayer();

    String getType();

    /**
     *
     * <p>Special case: if it equals a {@link Player#getName()}, it will be considered a reference to a player within the game.</p>
     *
     * @return
     */
    List<String> getParameters();
}
