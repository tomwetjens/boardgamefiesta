package com.wetjens.gwt.server.websockets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private EventType type;
    private String gameId;
    private String userId;
}
