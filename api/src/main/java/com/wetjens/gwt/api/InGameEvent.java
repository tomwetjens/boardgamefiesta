package com.wetjens.gwt.api;

import java.util.List;

public interface InGameEvent {
    Player getPlayer();

    String getType();

    List<String> getParameters();
}
