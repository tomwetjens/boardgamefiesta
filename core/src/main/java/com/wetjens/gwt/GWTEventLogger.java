package com.wetjens.gwt;

import java.util.List;

public interface GWTEventLogger {
    void log(Player player, GWTEvent event, List<Object> values);
}
